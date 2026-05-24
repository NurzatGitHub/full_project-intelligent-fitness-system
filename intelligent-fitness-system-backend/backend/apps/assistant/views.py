import os
import json
from collections import defaultdict
from datetime import datetime, timedelta

from django.db import transaction
from django.db.models import Prefetch
from django.utils import timezone

from rest_framework.decorators import api_view, permission_classes, throttle_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from rest_framework.throttling import UserRateThrottle

from google import genai
try:
    # google-genai 1.x: typed config class. We use this when available,
    # falling back to plain dict configs on older SDK versions.
    from google.genai import types as genai_types  # type: ignore
except Exception:  # pragma: no cover - defensive
    genai_types = None  # type: ignore

from .models import WeeklyPlan, WeeklyPlanDay, WeeklyPlanExercise, ChatMessage
from exercises.models import Exercise
from workouts.models import WorkoutSession


# Conversation memory: how many of the user's most recent chat turns we feed
# back into Gemini on each new message. Each turn = 1 user msg + 1 assistant
# reply, so 12 turns = 24 messages tops.
CHAT_MEMORY_TURNS = 12
CHAT_MESSAGE_CHAR_CAP = 2000  # per-message soft cap when sending to Gemini


class AssistantChatThrottle(UserRateThrottle):
    scope = "assistant_chat"


DAY_KEYS = ["mon", "tue", "wed", "thu", "fri", "sat", "sun"]
DAY_LABELS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]


def _safe(value, default="not specified"):
    if value is None:
        return default
    text = str(value).strip()
    return text if text else default


def _get_current_week_start():
    today = timezone.localdate()
    return today - timedelta(days=today.weekday())


def _day_key_from_index(index: int) -> str:
    return DAY_KEYS[index]


def _day_label_from_index(index: int) -> str:
    return DAY_LABELS[index]


def _serialize_plan(plan: WeeklyPlan, user) -> dict:
    sessions = (
        WorkoutSession.objects.filter(
            user=user,
            status="completed",
            weekly_plan_day__weekly_plan=plan,
        )
        .prefetch_related("exercises__exercise")
        .order_by("-finished_at", "-created_at")
    )

    completed_day_sessions = set()
    completed_slugs_by_day = defaultdict(set)

    for session in sessions:
        day_id = session.weekly_plan_day_id
        if not day_id:
            continue

        completed_day_sessions.add(day_id)

        for ex_item in session.exercises.all():
            slug = (ex_item.exercise_slug or "").strip()
            if not slug and ex_item.exercise:
                slug = ex_item.exercise.slug
            if slug:
                completed_slugs_by_day[day_id].add(slug)

    # Prefetch already-ordered plan_exercises so the inner loop doesn't re-hit DB.
    days_qs = (
        plan.days.all()
        .prefetch_related(
            Prefetch(
                "plan_exercises",
                queryset=WeeklyPlanExercise.objects.select_related("exercise").order_by("sort_order", "id"),
            )
        )
        .order_by("sort_order", "day_of_week", "id")
    )

    days = []
    for day in days_qs:
        exercises = []
        total_exercise_count = 0
        completed_exercise_count = 0

        for item in day.plan_exercises.all():  # already ordered via Prefetch
            ex = item.exercise
            is_completed = ex.slug in completed_slugs_by_day[day.id]

            total_exercise_count += 1
            if is_completed:
                completed_exercise_count += 1

            exercises.append(
                {
                    "id": ex.id,
                    "external_id": ex.external_id,
                    "name": ex.name,
                    "slug": ex.slug,
                    "description": ex.description,
                    "target_muscle": ex.target_muscle,
                    "equipment": ex.equipment,
                    "difficulty": ex.difficulty,
                    "asset_image_name": ex.asset_image_name,
                    "asset_video_name": ex.asset_video_name,
                    "default_sets": ex.default_sets,
                    "default_reps": ex.default_reps,
                    "default_duration_min": ex.default_duration_min,
                    "plan_sets": item.sets,
                    "plan_reps": item.reps,
                    "plan_duration_min": item.duration_min,
                    "plan_notes": item.notes,
                    "sort_order": item.sort_order,
                    "is_completed": is_completed,
                }
            )

        if total_exercise_count > 0:
            day_is_completed = completed_exercise_count == total_exercise_count
        else:
            day_is_completed = day.id in completed_day_sessions

        days.append(
            {
                "id": day.id,
                "day_key": _day_key_from_index(day.day_of_week),
                "label": day.label or _day_label_from_index(day.day_of_week),
                "type": day.day_type,
                "title": day.title,
                "description": day.description,
                "duration_min": day.duration_min,
                "note": day.note,
                "sort_order": day.sort_order,
                "is_completed": day_is_completed,
                "completed_exercise_count": completed_exercise_count,
                "total_exercise_count": total_exercise_count,
                "exercises": exercises,
            }
        )

    return {
        "id": plan.id,
        "title": plan.title or "AI Weekly Plan",
        "goal_summary": plan.summary or "Personalized weekly training plan",
        "today_tip": plan.today_tip or "Focus on form and consistency",
        "week_start_date": str(plan.week_start_date),
        "is_active": plan.is_active,
        "generated_at": plan.created_at.isoformat() if plan.created_at else None,
        "days": days,
    }


def build_user_profile_block(user) -> str:
    return (
        f"- age: {_safe(getattr(user, 'age', None))}\n"
        f"- height cm: {_safe(getattr(user, 'height', None))}\n"
        f"- weight kg: {_safe(getattr(user, 'weight', None))}\n"
        f"- gender: {_safe(getattr(user, 'gender', ''))}\n"
        f"- fitness level: {_safe(getattr(user, 'fitness_level', ''))}\n"
        f"- goal: {_safe(getattr(user, 'goal', ''))}\n"
        f"- limitations: {_safe(getattr(user, 'limitations', ''))}\n"
        f"- training frequency per week: {_safe(getattr(user, 'frequency', ''))}\n"
        f"- workout duration preference: {_safe(getattr(user, 'workout_duration', ''))}\n"
        f"- workout place: {_safe(getattr(user, 'workout_place', ''))}\n"
        f"- endurance level: {_safe(getattr(user, 'endurance_level', ''))}\n"
    )


def _build_profile_snapshot(user) -> dict:
    return {
        "age": getattr(user, "age", None),
        "height": getattr(user, "height", None),
        "weight": getattr(user, "weight", None),
        "gender": getattr(user, "gender", ""),
        "fitness_level": getattr(user, "fitness_level", ""),
        "goal": getattr(user, "goal", ""),
        "limitations": getattr(user, "limitations", ""),
        "frequency": getattr(user, "frequency", ""),
        "workout_duration": getattr(user, "workout_duration", ""),
        "workout_place": getattr(user, "workout_place", ""),
        "endurance_level": getattr(user, "endurance_level", ""),
    }


def build_system_prompt(user) -> str:
    return (
        "You are a fitness coach inside a mobile app. "
        "Answer briefly, clearly, and practically. "
        "Use the user's fitness profile to personalize the answer. "
        "If the user mentions pain or injury, advise them to stop and consult a professional. "
        "Do not give medical diagnosis.\n\n"
        "User profile:\n"
        f"{build_user_profile_block(user)}"
    )


def _pick_exercises_for_day(day_type: str, title: str, user):
    if day_type != "workout":
        return []

    qs = Exercise.objects.filter(is_active=True).select_related("category", "subcategory")

    fitness_level = str(getattr(user, "fitness_level", "") or "").strip().lower()
    workout_place = str(getattr(user, "workout_place", "") or "").strip().lower()
    title_lower = (title or "").lower()

    if fitness_level in ("beginner", "intermediate", "advanced"):
        allowed_difficulties = ["beginner"]
        if fitness_level == "intermediate":
            allowed_difficulties = ["beginner", "intermediate"]
        elif fitness_level == "advanced":
            allowed_difficulties = ["beginner", "intermediate", "advanced"]
        qs = qs.filter(difficulty__in=allowed_difficulties)

    if any(x in workout_place for x in ["home", "house", "apartment"]):
        qs = qs.exclude(equipment__iregex=r"(machine|barbell|smith|cable)")

    keyword_to_category = [
        (["chest"], "chest"),
        (["back", "lats"], "back"),
        (["legs", "glutes", "quads", "hamstrings", "calves"], "legs"),
        (["abs", "core"], "abs"),
        (["cardio", "hiit"], "cardio"),
        (["arms", "biceps", "triceps"], "arms"),
    ]

    matched_category_slug = None
    for keywords, category_slug in keyword_to_category:
        if any(word in title_lower for word in keywords):
            matched_category_slug = category_slug
            break

    if matched_category_slug:
        filtered = list(qs.filter(category__slug=matched_category_slug).order_by("sort_order", "name")[:6])
        if filtered:
            return filtered[:4]

    return list(qs.order_by("category__sort_order", "subcategory__sort_order", "sort_order", "name")[:4])


def build_weekly_plan_prompt(user) -> str:
    return (
        "You are a fitness coach inside a mobile app.\n"
        "Generate a practical weekly training plan personalized to the user's profile.\n"
        "Respect injuries, limitations, fitness level, training frequency, workout duration, workout place, and endurance level.\n"
        "Do not give medical diagnosis.\n"
        "If limitations suggest caution, make the plan safer and lighter.\n\n"
        "Return ONLY valid JSON. No markdown. No explanation. No code fences.\n\n"
        "Required JSON format:\n"
        "{\n"
        '  "title": "AI Weekly Plan",\n'
        '  "goal_summary": "short 1-sentence summary",\n'
        '  "days": [\n'
        '    {"day_key":"mon","label":"Mon","type":"workout","title":"Chest + Triceps","duration_min":45,"note":"short tip"},\n'
        '    {"day_key":"tue","label":"Tue","type":"rest","title":"Recovery","duration_min":20,"note":"short tip"},\n'
        '    {"day_key":"wed","label":"Wed","type":"workout","title":"Back + Biceps","duration_min":45,"note":"short tip"},\n'
        '    {"day_key":"thu","label":"Thu","type":"rest","title":"Mobility","duration_min":15,"note":"short tip"},\n'
        '    {"day_key":"fri","label":"Fri","type":"workout","title":"Legs + Core","duration_min":50,"note":"short tip"},\n'
        '    {"day_key":"sat","label":"Sat","type":"workout","title":"Full Body","duration_min":40,"note":"short tip"},\n'
        '    {"day_key":"sun","label":"Sun","type":"rest","title":"Recovery","duration_min":20,"note":"short tip"}\n'
        "  ],\n"
        '  "today_tip": "short helpful tip"\n'
        "}\n\n"
        "Rules:\n"
        "- exactly 7 days in this order: Mon, Tue, Wed, Thu, Fri, Sat, Sun\n"
        "- type must be either workout, rest, or recovery\n"
        "- duration_min must be an integer\n"
        "- title must be very short\n"
        "- note must be very short\n"
        "- goal_summary must be very short\n"
        "- keep the plan realistic for a mobile fitness app\n\n"
        "User profile:\n"
        f"{build_user_profile_block(user)}"
    )


def _extract_json_object(text: str) -> dict:
    raw = (text or "").strip()

    if raw.startswith("```"):
        raw = raw.replace("```json", "").replace("```", "").strip()

    start = raw.find("{")
    end = raw.rfind("}")
    if start == -1 or end == -1 or end <= start:
        raise ValueError("JSON object not found in Gemini response")

    raw = raw[start:end + 1]
    return json.loads(raw)


def _normalize_day(day: dict, index: int) -> dict:
    day_key = DAY_KEYS[index]
    label = DAY_LABELS[index]

    day_type = str(day.get("type", "rest")).strip().lower()
    if day_type not in ("workout", "rest", "recovery"):
        day_type = "rest"

    title = str(day.get("title", "")).strip() or ("Workout" if day_type == "workout" else "Recovery")
    note = str(day.get("note", "")).strip() or "Stay consistent"

    try:
        duration_min = int(day.get("duration_min", 20))
    except Exception:
        duration_min = 20

    duration_min = max(5, min(duration_min, 180))

    return {
        "day_key": day_key,
        "label": label,
        "type": day_type,
        "title": title[:60],
        "duration_min": duration_min,
        "note": note[:120],
    }


def _normalize_plan(data: dict) -> dict:
    if not isinstance(data, dict):
        raise ValueError("Weekly plan response is not a JSON object")

    raw_days = data.get("days")
    if not isinstance(raw_days, list) or len(raw_days) != 7:
        raise ValueError("Weekly plan must contain exactly 7 days")

    normalized_days = [_normalize_day(raw_days[i], i) for i in range(7)]

    title = str(data.get("title", "AI Weekly Plan")).strip() or "AI Weekly Plan"
    goal_summary = str(data.get("goal_summary", "")).strip() or "Personalized weekly training plan"
    today_tip = str(data.get("today_tip", "")).strip() or "Focus on form and consistency"

    return {
        "title": title[:80],
        "goal_summary": goal_summary[:180],
        "days": normalized_days,
        "today_tip": today_tip[:180],
        "generated_at": datetime.now().isoformat(),
    }


def _is_transient_gemini_error(exc: Exception) -> bool:
    """503 UNAVAILABLE / 429 rate-limit / timeouts / empty responses are worth retrying."""
    msg = str(exc).upper()
    return any(token in msg for token in (
        "503", "UNAVAILABLE",
        "429", "RESOURCE_EXHAUSTED", "RATE",
        "DEADLINE_EXCEEDED", "TIMEOUT",
        "EMPTY_RESPONSE",
        "JSON OBJECT NOT FOUND",
    ))


def _summarize_empty_response(response) -> str:
    """
    Extracts whatever useful diagnostics we can from an empty Gemini response.

    Common reasons for empty `response.text`:
      * `finish_reason == MAX_TOKENS` — thinking ate the whole budget
      * `finish_reason == SAFETY` — content blocked by safety filters
      * `finish_reason == RECITATION` — verbatim copy block
      * model returned only `thought_summary` parts (2.5 thinking mode)
    """
    try:
        candidates = getattr(response, "candidates", None) or []
        if not candidates:
            return "no candidates returned"
        first = candidates[0]
        finish = getattr(first, "finish_reason", None)
        finish_name = getattr(finish, "name", str(finish)) if finish is not None else "UNKNOWN"

        content = getattr(first, "content", None)
        parts = getattr(content, "parts", None) if content else None
        part_kinds = []
        if parts:
            for p in parts:
                if getattr(p, "text", None):
                    part_kinds.append("text")
                elif getattr(p, "thought", False):
                    part_kinds.append("thought")
                else:
                    part_kinds.append("other")
        return f"finish_reason={finish_name}, parts={part_kinds or 'none'}"
    except Exception as e:
        return f"diag-error: {type(e).__name__}: {e}"


def _generate_weekly_plan_data(user) -> dict:
    """
    Generate a weekly plan via Gemini.

    Gemini occasionally returns 503 UNAVAILABLE when the model is hot —
    that's transient. We retry with short exponential backoff (3 tries
    total) so a brand-new user doesn't see "Failed to load weekly plan"
    on their very first home screen.
    """
    import time
    import logging
    log = logging.getLogger(__name__)

    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise RuntimeError("GEMINI_API_KEY is not set")

    model_name = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
    client = genai.Client(api_key=api_key)
    prompt = build_weekly_plan_prompt(user)

    attempts = 3
    backoff_seconds = [1.5, 4.0]  # waits BETWEEN attempts 1->2 and 2->3
    last_exc: Exception | None = None

    # gemini-2.5-flash burns tokens on "thinking" before generating visible
    # output. For weekly plan we want a guaranteed-non-empty JSON, so:
    #   * disable thinking (all budget goes to actual text)
    #   * give a huge budget (the JSON is small, but better safe than truncated)
    #   * DON'T set response_mime_type=json — that's unreliable on 2.5-flash
    #     without a schema and is a frequent cause of empty responses.
    plan_config = _gemini_generation_config(
        structured_json=False,
        max_output_tokens=4000,
        disable_thinking=True,
    )

    for attempt in range(attempts):
        try:
            response = _safe_generate_content(
                client,
                model=model_name,
                contents=prompt,
                config=plan_config,
            )

            raw_text = (response.text or "")
            if not raw_text.strip():
                # Empty body. Log finish reason / safety blocks so we can
                # see WHY in the server console and treat it as transient
                # (retry next loop iter or surface to client as 503).
                finish_info = _summarize_empty_response(response)
                log.warning(
                    "Gemini weekly-plan attempt %d/%d returned empty body. %s",
                    attempt + 1, attempts, finish_info,
                )
                raise RuntimeError(f"empty_response: {finish_info}")

            data = _extract_json_object(raw_text)
            return _normalize_plan(data)

        except Exception as exc:
            last_exc = exc
            log.warning(
                "Gemini weekly-plan attempt %d/%d failed: %s: %s",
                attempt + 1, attempts, type(exc).__name__, exc,
            )
            # Empty responses + JSON parsing failures are usually transient
            # (model overloaded, hit safety filter once, etc.) so retry.
            is_retryable = (
                _is_transient_gemini_error(exc)
                or "empty_response" in str(exc).lower()
                or isinstance(exc, ValueError)  # JSON parse failure
            )
            if attempt < attempts - 1 and is_retryable:
                wait = backoff_seconds[attempt]
                log.warning("Retrying in %.1fs…", wait)
                time.sleep(wait)
                continue
            raise

    # Should not reach here, but guard anyway.
    if last_exc:
        raise last_exc
    raise RuntimeError("Weekly plan generation failed for unknown reasons")


@transaction.atomic
def _create_and_save_weekly_plan(user, plan_data: dict) -> WeeklyPlan:
    week_start = _get_current_week_start()

    WeeklyPlan.objects.filter(
        user=user,
        is_active=True,
    ).exclude(week_start_date=week_start).update(is_active=False)

    WeeklyPlan.objects.filter(
        user=user,
        week_start_date=week_start,
        is_active=True,
    ).update(is_active=False)

    plan = WeeklyPlan.objects.create(
        user=user,
        title=plan_data.get("title") or "AI Weekly Plan",
        summary=plan_data.get("goal_summary") or "Personalized weekly training plan",
        today_tip=plan_data.get("today_tip") or "Focus on form and consistency",
        week_start_date=week_start,
        is_active=True,
        profile_snapshot=_build_profile_snapshot(user),
    )

    for index, day_data in enumerate(plan_data.get("days", [])):
        day = WeeklyPlanDay.objects.create(
            weekly_plan=plan,
            day_of_week=index,
            label=day_data.get("label") or _day_label_from_index(index),
            day_type=day_data.get("type") or "rest",
            title=day_data.get("title") or "",
            description="",
            duration_min=day_data.get("duration_min") or 20,
            note=day_data.get("note") or "",
            sort_order=index,
        )

        selected_exercises = _pick_exercises_for_day(
            day_type=day.day_type,
            title=day.title,
            user=user,
        )

        for exercise_index, exercise in enumerate(selected_exercises):
            WeeklyPlanExercise.objects.create(
                weekly_plan_day=day,
                exercise=exercise,
                sort_order=exercise_index,
                sets=exercise.default_sets,
                reps=exercise.default_reps,
                duration_min=exercise.default_duration_min,
                notes="",
            )

    return plan


def _get_or_create_current_week_plan(user) -> WeeklyPlan:
    week_start = _get_current_week_start()

    existing = (
        WeeklyPlan.objects.filter(
            user=user,
            week_start_date=week_start,
            is_active=True,
        )
        .prefetch_related("days__plan_exercises__exercise")
        .order_by("-created_at")
        .first()
    )
    if existing:
        return existing

    plan_data = _generate_weekly_plan_data(user)
    return _create_and_save_weekly_plan(user, plan_data)


def _load_chat_history_for_gemini(user, limit_turns: int = CHAT_MEMORY_TURNS):
    """
    Returns the last `limit_turns * 2` messages for the user, formatted for the
    Gemini SDK's `contents=` argument.

    Gemini expects:
        [{"role": "user"|"model", "parts": [{"text": "..."}]}, ...]

    We use Django role "assistant" but Gemini wants "model".
    """
    # Pull the newest 2N rows in reverse, then reverse back so they're chronological.
    rows = list(
        ChatMessage.objects
        .filter(user=user)
        .order_by("-created_at")[: limit_turns * 2]
    )
    rows.reverse()

    history = []
    for row in rows:
        text = (row.content or "")[:CHAT_MESSAGE_CHAR_CAP]
        if not text:
            continue
        gemini_role = "model" if row.role == ChatMessage.ROLE_ASSISTANT else "user"
        history.append({"role": gemini_role, "parts": [{"text": text}]})
    return history


def _persist_turn(user, user_message: str, assistant_reply: str):
    """Save both halves of a conversation turn atomically."""
    with transaction.atomic():
        ChatMessage.objects.create(
            user=user,
            role=ChatMessage.ROLE_USER,
            content=user_message[:CHAT_MESSAGE_CHAR_CAP],
            tokens_estimate=max(1, len(user_message) // 4),
        )
        ChatMessage.objects.create(
            user=user,
            role=ChatMessage.ROLE_ASSISTANT,
            content=assistant_reply[:CHAT_MESSAGE_CHAR_CAP * 2],
            tokens_estimate=max(1, len(assistant_reply) // 4),
        )


def _gemini_generation_config(
    *,
    structured_json: bool = False,
    system_instruction: str | None = None,
    max_output_tokens: int | None = None,
    disable_thinking: bool = False,
):
    """
    Returns a config object suitable for `config=` on Gemini calls.

    Notes about gemini-2.5-flash:
      * The model has a *thinking budget* that eats from the same token pool
        as visible output. If your max is too low (e.g. 450), the visible
        text can come back empty even though the API call succeeded. We
        either bump tokens way up or set thinking_budget=0 for short tasks.
      * `response_mime_type="application/json"` without a `response_schema`
        is unreliable on 2.5-flash — the model sometimes returns nothing.
        For weekly-plan we prefer parsing markdown JSON instead.
    """
    try:
        default_tokens = int(os.getenv("GEMINI_MAX_OUTPUT_TOKENS", "1200"))
    except ValueError:
        default_tokens = 1200
    tokens = max_output_tokens or default_tokens

    def _try_build_typed():
        if genai_types is None:
            return None

        kwargs = {"max_output_tokens": tokens, "temperature": 0.7}
        if structured_json:
            kwargs["response_mime_type"] = "application/json"
        if system_instruction:
            kwargs["system_instruction"] = system_instruction

        # Best-effort: turn OFF the 2.5-flash "thinking" budget so the entire
        # token budget goes into visible output. The SDK exposes this via a
        # nested ThinkingConfig — wrap in try because not every SDK version
        # has it.
        if disable_thinking:
            try:
                thinking_cfg = genai_types.ThinkingConfig(thinking_budget=0)  # type: ignore[attr-defined]
                kwargs["thinking_config"] = thinking_cfg
            except Exception:
                pass

        try:
            return genai_types.GenerateContentConfig(**kwargs)  # type: ignore[attr-defined]
        except Exception:
            return None

    typed = _try_build_typed()
    if typed is not None:
        return typed

    # Dict fallback (older / different SDKs).
    cfg = {"max_output_tokens": tokens, "temperature": 0.7}
    if structured_json:
        cfg["response_mime_type"] = "application/json"
    if system_instruction:
        cfg["system_instruction"] = system_instruction
    if disable_thinking:
        cfg["thinking_config"] = {"thinking_budget": 0}
    return cfg


def _safe_generate_content(client, *, model: str, contents, config):
    """
    Calls Gemini with the given config. If the SDK rejects the config (e.g.
    older version that doesn't know `response_mime_type`), retries once
    WITHOUT config so we still get an answer.
    """
    try:
        return client.models.generate_content(
            model=model,
            contents=contents,
            config=config,
        )
    except TypeError:
        # SDK signature mismatch (config kwarg unsupported) — retry plain.
        return client.models.generate_content(model=model, contents=contents)
    except Exception as exc:
        # Some SDK versions raise on unknown config fields with a generic error.
        msg = str(exc).lower()
        if "unexpected" in msg or "unknown" in msg or "response_mime_type" in msg:
            return client.models.generate_content(model=model, contents=contents)
        raise


@api_view(["POST"])
@permission_classes([IsAuthenticated])
@throttle_classes([AssistantChatThrottle])
def assistant_chat(request):
    message = (request.data.get("message") or "").strip()

    if not message:
        return Response({"detail": "message is required"}, status=status.HTTP_400_BAD_REQUEST)

    if len(message) > 4000:
        return Response({"detail": "message too long"}, status=status.HTTP_400_BAD_REQUEST)

    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return Response({"detail": "GEMINI_API_KEY is not set"}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

    model_name = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
    system_prompt = build_system_prompt(request.user)

    try:
        client = genai.Client(api_key=api_key)

        # Build contents: prior turns + current user message.
        contents = _load_chat_history_for_gemini(request.user)
        contents.append({"role": "user", "parts": [{"text": message}]})

        # Chat replies are short, so 1200 tokens with thinking disabled
        # is plenty and avoids the empty-response issue on 2.5-flash.
        response = _safe_generate_content(
            client,
            model=model_name,
            contents=contents,
            config=_gemini_generation_config(
                system_instruction=system_prompt,
                max_output_tokens=1200,
                disable_thinking=True,
            ),
        )

        reply = (response.text or "").strip()
        if not reply:
            import logging
            logging.getLogger(__name__).warning(
                "assistant_chat empty reply. %s",
                _summarize_empty_response(response),
            )
            reply = "I couldn't generate a response. Please try again."

        if len(reply) > 3000:
            reply = reply[:3000]

        # Persist the turn so subsequent requests have memory.
        _persist_turn(request.user, message, reply)

        return Response({"reply": reply}, status=status.HTTP_200_OK)

    except Exception as e:
        import logging
        log = logging.getLogger(__name__)
        log.exception("assistant_chat failed")

        error_text = str(e)

        if "RESOURCE_EXHAUSTED" in error_text or "429" in error_text:
            return Response(
                {"detail": "Coach is busy right now. Please try again in a minute."},
                status=status.HTTP_429_TOO_MANY_REQUESTS,
            )

        if "PERMISSION_DENIED" in error_text or "API key" in error_text:
            return Response(
                {"detail": "Coach is temporarily unavailable."},
                status=status.HTTP_502_BAD_GATEWAY,
            )

        if "INVALID_ARGUMENT" in error_text or "404" in error_text:
            return Response(
                {"detail": "Invalid request to the coach."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        return Response(
            {"detail": "Coach error. Please try again."},
            status=status.HTTP_502_BAD_GATEWAY,
        )


@api_view(["GET"])
@permission_classes([IsAuthenticated])
def weekly_plan(request):
    try:
        plan = _get_or_create_current_week_plan(request.user)
        return Response(_serialize_plan(plan, request.user), status=status.HTTP_200_OK)

    except Exception as e:
        import logging
        logging.getLogger(__name__).exception("weekly_plan failed")

        # Differentiate transient Gemini outages (503) from real errors so the
        # client can show "try again in a moment" rather than a hard failure.
        if _is_transient_gemini_error(e):
            return Response(
                {
                    "detail": "Coach is busy generating your first plan. "
                              "Please pull to refresh in a moment.",
                    "transient": True,
                },
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )

        return Response(
            {"detail": "Failed to load weekly plan. Please try again."},
            status=status.HTTP_502_BAD_GATEWAY,
        )


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def regenerate_weekly_plan(request):
    try:
        WeeklyPlan.objects.filter(user=request.user, is_active=True).update(is_active=False)
        plan_data = _generate_weekly_plan_data(request.user)
        plan = _create_and_save_weekly_plan(request.user, plan_data)
        return Response(_serialize_plan(plan, request.user), status=status.HTTP_200_OK)

    except Exception as e:
        import logging
        logging.getLogger(__name__).exception("regenerate_weekly_plan failed")

        if _is_transient_gemini_error(e):
            return Response(
                {
                    "detail": "Coach is busy. Please try again in a moment.",
                    "transient": True,
                },
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )

        return Response(
            {"detail": "Failed to regenerate weekly plan. Please try again."},
            status=status.HTTP_502_BAD_GATEWAY,
        )


# ============================================================================
#   Chat history endpoints
# ============================================================================


@api_view(["GET"])
@permission_classes([IsAuthenticated])
def chat_messages(request):
    """
    Returns the user's conversation history with FitBot, oldest first.

    Query params:
        limit  — max number of messages to return (default 200, max 500).
    """
    try:
        limit = int(request.query_params.get("limit", "200"))
    except ValueError:
        limit = 200
    limit = max(1, min(limit, 500))

    # Get the newest `limit` rows, then send chronological.
    rows = list(
        ChatMessage.objects
        .filter(user=request.user)
        .order_by("-created_at")[:limit]
    )
    rows.reverse()

    return Response({
        "messages": [
            {
                "id": row.id,
                "role": row.role,
                "content": row.content,
                "created_at": row.created_at.isoformat(),
            }
            for row in rows
        ],
    })


@api_view(["DELETE"])
@permission_classes([IsAuthenticated])
def clear_chat_messages(request):
    """Wipe the user's entire chat history with FitBot. UI calls this when the
    user taps 'Reset conversation'."""
    deleted, _ = ChatMessage.objects.filter(user=request.user).delete()
    return Response({"deleted": deleted}, status=status.HTTP_200_OK)


# ============================================================================
#   Streaming chat (Server-Sent Events)
# ============================================================================


def _sse_event(data: dict) -> bytes:
    """Format a single SSE frame: 'data: {json}\\n\\n'."""
    return f"data: {json.dumps(data, ensure_ascii=False)}\n\n".encode("utf-8")


@api_view(["POST"])
@permission_classes([IsAuthenticated])
@throttle_classes([AssistantChatThrottle])
def assistant_chat_stream(request):
    """
    Streams the FitBot reply token-by-token as Server-Sent Events.

    Response frames (each terminated by a blank line):
        data: {"type": "chunk", "text": "..."}
        data: {"type": "done",  "text": "<full reply>"}
        data: {"type": "error", "detail": "..."}

    The client should keep reading until it sees "done" or "error".
    """
    from django.http import StreamingHttpResponse

    message = (request.data.get("message") or "").strip()
    if not message:
        return Response({"detail": "message is required"}, status=status.HTTP_400_BAD_REQUEST)
    if len(message) > 4000:
        return Response({"detail": "message too long"}, status=status.HTTP_400_BAD_REQUEST)

    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return Response({"detail": "GEMINI_API_KEY is not set"}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

    model_name = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
    system_prompt = build_system_prompt(request.user)

    # Build conversation context BEFORE entering the generator,
    # because Django ORM calls inside a streaming generator are awkward.
    contents = _load_chat_history_for_gemini(request.user)
    contents.append({"role": "user", "parts": [{"text": message}]})

    user_obj = request.user

    def event_stream():
        import logging
        log = logging.getLogger(__name__)
        full_reply_parts = []
        try:
            client = genai.Client(api_key=api_key)
            try:
                stream = client.models.generate_content_stream(
                    model=model_name,
                    contents=contents,
                    config=_gemini_generation_config(system_instruction=system_prompt),
                )
            except TypeError:
                # SDK version doesn't accept `config=` on stream call.
                stream = client.models.generate_content_stream(
                    model=model_name,
                    contents=contents,
                )
            for chunk in stream:
                piece = (getattr(chunk, "text", "") or "")
                if not piece:
                    continue
                full_reply_parts.append(piece)
                yield _sse_event({"type": "chunk", "text": piece})

            full_reply = "".join(full_reply_parts).strip()
            if not full_reply:
                full_reply = "I couldn't generate a response. Please try again."
            if len(full_reply) > 3000:
                full_reply = full_reply[:3000]

            try:
                _persist_turn(user_obj, message, full_reply)
            except Exception:
                log.exception("failed to persist streamed chat turn")

            yield _sse_event({"type": "done", "text": full_reply})

        except Exception as e:
            log.exception("assistant_chat_stream failed")
            err = str(e)
            if "RESOURCE_EXHAUSTED" in err or "429" in err:
                msg = "Coach is busy right now. Please try again in a minute."
            elif "PERMISSION_DENIED" in err or "API key" in err:
                msg = "Coach is temporarily unavailable."
            else:
                msg = "Coach error. Please try again."
            yield _sse_event({"type": "error", "detail": msg})

    resp = StreamingHttpResponse(
        event_stream(),
        content_type="text/event-stream",
    )
    resp["Cache-Control"] = "no-cache"
    resp["X-Accel-Buffering"] = "no"
    return resp