from collections import Counter
from datetime import timedelta

from django.db import transaction
from django.db.models import Sum, Avg
from django.utils import timezone

from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status

from assistant.models import WeeklyPlanDay
from exercises.models import Exercise
from .models import WorkoutSession, WorkoutExercise, UserProgressSnapshot
from .serializers import (
    WorkoutSessionCreateSerializer,
    WorkoutSessionReadSerializer,
    UserProgressSnapshotSerializer,
)


def _calculate_streak(sessions):
    if not sessions:
        return 0

    workout_days = sorted(
        {
            timezone.localtime(session.finished_at).date()
            for session in sessions
            if session.finished_at
        },
        reverse=True,
    )

    if not workout_days:
        return 0

    today = timezone.localdate()
    yesterday = today - timedelta(days=1)

    first_day = workout_days[0]
    if first_day not in (today, yesterday):
        return 0

    streak = 1
    current_day = first_day

    for next_day in workout_days[1:]:
        if next_day == current_day - timedelta(days=1):
            streak += 1
            current_day = next_day
        elif next_day == current_day:
            continue
        else:
            break

    return streak


def _refresh_user_progress(user):
    sessions = list(
        WorkoutSession.objects.filter(user=user, status="completed")
        .prefetch_related("exercises")
        .order_by("-finished_at", "-created_at")
    )

    total_workouts = len(sessions)
    total_reps = sum(session.total_reps or 0 for session in sessions)

    scores = [session.avg_form_score for session in sessions if session.avg_form_score is not None]
    average_form_score = round(sum(scores) / len(scores), 2) if scores else 0

    exercise_counter = Counter()
    for session in sessions:
        for exercise_item in session.exercises.all():
            name = (
                exercise_item.exercise.name
                if exercise_item.exercise and exercise_item.exercise.name
                else exercise_item.exercise_name
            )
            if name:
                exercise_counter[name] += 1

    best_exercise = exercise_counter.most_common(1)[0][0] if exercise_counter else ""
    current_streak = _calculate_streak(sessions)
    last_workout_at = sessions[0].finished_at if sessions else None

    snapshot, _ = UserProgressSnapshot.objects.update_or_create(
        user=user,
        defaults={
            "total_workouts": total_workouts,
            "total_reps": total_reps,
            "average_form_score": average_form_score,
            "current_streak": current_streak,
            "best_exercise": best_exercise,
            "last_workout_at": last_workout_at,
        },
    )
    return snapshot


@api_view(["POST"])
@permission_classes([IsAuthenticated])
@transaction.atomic
def create_workout_session(request):
    serializer = WorkoutSessionCreateSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    data = serializer.validated_data

    # Idempotency: if the client retries with the same client_uuid, return the
    # existing session instead of creating a duplicate.
    client_uuid = (data.get("client_uuid") or "").strip()
    if client_uuid:
        existing = (
            WorkoutSession.objects
            .filter(user=request.user, client_uuid=client_uuid)
            .prefetch_related("exercises")
            .first()
        )
        if existing:
            snapshot = _refresh_user_progress(request.user)
            return Response(
                {
                    "session": WorkoutSessionReadSerializer(existing).data,
                    "stats": UserProgressSnapshotSerializer(snapshot).data,
                    "idempotent": True,
                },
                status=status.HTTP_200_OK,
            )

    started_at = data.get("started_at") or timezone.now()
    finished_at = data.get("finished_at") or timezone.now()

    weekly_plan_day = None
    weekly_plan_day_id = data.get("weekly_plan_day_id")
    if weekly_plan_day_id:
        weekly_plan_day = WeeklyPlanDay.objects.filter(
            id=weekly_plan_day_id,
            weekly_plan__user=request.user,
        ).first()

    session = WorkoutSession.objects.create(
        user=request.user,
        weekly_plan_day=weekly_plan_day,
        title=data.get("title", ""),
        started_at=started_at,
        finished_at=finished_at,
        total_duration_sec=max(0, data.get("total_duration_sec", 0)),
        total_reps=max(0, data.get("total_reps", 0)),
        avg_form_score=data.get("avg_form_score"),
        calories_burned=data.get("calories_burned"),
        status=data.get("status") or "completed",
        notes=data.get("notes", ""),
        client_uuid=client_uuid,
    )

    exercises_data = data.get("exercises", [])
    for index, item in enumerate(exercises_data):
        exercise = None
        exercise_slug = item.get("exercise_slug", "").strip()
        if exercise_slug:
            exercise = Exercise.objects.filter(slug=exercise_slug).first()

        exercise_name = item.get("exercise_name", "").strip()
        if not exercise_name and exercise:
            exercise_name = exercise.name

        WorkoutExercise.objects.create(
            workout_session=session,
            exercise=exercise,
            exercise_name=exercise_name,
            exercise_slug=exercise_slug or (exercise.slug if exercise else ""),
            sort_order=index,
            completed_sets=item.get("completed_sets"),
            completed_reps=max(0, item.get("completed_reps", 0)),
            duration_sec=max(0, item.get("duration_sec", 0)),
            avg_form_score=item.get("avg_form_score"),
            detected_mistake=item.get("detected_mistake", ""),
            notes=item.get("notes", ""),
        )

    snapshot = _refresh_user_progress(request.user)

    return Response(
        {
            "session": WorkoutSessionReadSerializer(session).data,
            "stats": UserProgressSnapshotSerializer(snapshot).data,
        },
        status=status.HTTP_201_CREATED,
    )


@api_view(["GET"])
@permission_classes([IsAuthenticated])
def workout_history(request):
    """
    Paginated workout history. Response shape (DRF PageNumberPagination):

        {
          "count": 42,
          "next":  "http://.../api/workouts/history/?page=2",
          "previous": null,
          "results": [ { ...session... }, ... ]
        }

    Clients can pass `?page=2` and `?page_size=50` (max 100).
    """
    from rest_framework.pagination import PageNumberPagination

    class WorkoutHistoryPagination(PageNumberPagination):
        page_size = 20
        page_size_query_param = "page_size"
        max_page_size = 100

    sessions = (
        WorkoutSession.objects.filter(user=request.user, status="completed")
        .prefetch_related("exercises")
        .order_by("-finished_at", "-created_at")
    )

    paginator = WorkoutHistoryPagination()
    page = paginator.paginate_queryset(sessions, request)
    serializer = WorkoutSessionReadSerializer(page, many=True)
    return paginator.get_paginated_response(serializer.data)


@api_view(["GET"])
@permission_classes([IsAuthenticated])
def workout_stats(request):
    """
    Returns the user's progress snapshot PLUS extra series the mobile Stats
    screen needs to render the form-score bar chart and the time-in-zone
    distribution. Backwards-compatible: existing fields are unchanged, new
    ones are additive.

    Shape:
        {
          "total_workouts": 12,
          "total_reps": 287,
          "average_form_score": 81.4,
          "current_streak": 3,
          "best_exercise": "Push-up",
          "last_workout_at": "...",
          "updated_at": "...",

          "form_score_history": [          // newest-last, last 20 sessions
            {"date": "2026-06-08", "title": "Push-up", "form_score": 78.0},
            ...
          ],
          "zone_distribution": {           // counts of sessions per quality bucket
            "excellent": 4,                //  >= 85
            "good":      5,                //  70-84
            "average":   2,                //  50-69
            "poor":      1                 //  < 50
          }
        }
    """
    user = request.user
    snapshot = _refresh_user_progress(user)
    base = UserProgressSnapshotSerializer(snapshot).data

    # Pull the last 20 completed sessions with a non-null form score so the
    # mobile chart can plot a meaningful trend without zeros polluting it.
    recent = list(
        WorkoutSession.objects
        .filter(user=user, status="completed", avg_form_score__isnull=False)
        .order_by("-finished_at", "-created_at")[:20]
    )
    recent.reverse()  # chronological so the chart draws left→right

    base["form_score_history"] = [
        {
            "date": (
                timezone.localtime(s.finished_at).date().isoformat()
                if s.finished_at else None
            ),
            "title": s.title or "",
            "form_score": round(float(s.avg_form_score or 0), 1),
        }
        for s in recent
    ]

    # Bucket every scored session into a quality zone. These thresholds match
    # the colors we picked on the Android side (Excellent/Good/Average/Poor).
    distribution = {"excellent": 0, "good": 0, "average": 0, "poor": 0}
    all_scored = (
        WorkoutSession.objects
        .filter(user=user, status="completed", avg_form_score__isnull=False)
        .values_list("avg_form_score", flat=True)
    )
    for score in all_scored:
        s = float(score)
        if s >= 85:
            distribution["excellent"] += 1
        elif s >= 70:
            distribution["good"] += 1
        elif s >= 50:
            distribution["average"] += 1
        else:
            distribution["poor"] += 1
    base["zone_distribution"] = distribution

    return Response(base)