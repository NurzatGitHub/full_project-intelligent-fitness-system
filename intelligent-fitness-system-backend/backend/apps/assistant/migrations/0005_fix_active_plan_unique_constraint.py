"""
Fix the unique constraint on WeeklyPlan.

Original constraint (broken):
    UNIQUE (user_id, week_start_date, is_active)

Problem: that forbids a user from having TWO inactive plans for the same week.
The view `_create_and_save_weekly_plan` deactivates the current active plan
when a new one is generated, which can collide with a previously-deactivated
plan on the same week → IntegrityError → weekly_plan endpoint returns 502.

New constraint (correct):
    UNIQUE (user_id, week_start_date) WHERE is_active = TRUE

This guarantees only one ACTIVE plan per week, but allows unlimited history of
inactive plans.

This migration also cleans up any pre-existing duplicate inactive rows so the
new constraint can be created without conflict on existing data.
"""
from django.db import migrations, models
from django.db.models import Q


def dedupe_inactive_plans(apps, schema_editor):
    """
    Keep only the most recent inactive plan per (user, week_start_date) pair.
    Older inactive duplicates are deleted. Active plans are never touched.
    """
    WeeklyPlan = apps.get_model("assistant", "WeeklyPlan")

    # Group by (user, week_start_date) and find pairs with >1 inactive plan.
    seen = {}
    inactive_qs = (
        WeeklyPlan.objects
        .filter(is_active=False)
        .order_by("user_id", "week_start_date", "-created_at", "-id")
    )
    to_delete = []
    for plan in inactive_qs:
        key = (plan.user_id, plan.week_start_date)
        if key in seen:
            # We already kept a newer one — drop this older duplicate.
            to_delete.append(plan.id)
        else:
            seen[key] = plan.id

    if to_delete:
        WeeklyPlan.objects.filter(id__in=to_delete).delete()


def noop_reverse(apps, schema_editor):
    # Nothing to restore — we permanently dropped duplicate history.
    pass


class Migration(migrations.Migration):

    dependencies = [
        ("assistant", "0004_rename_assistant_c_user_id_created_idx_assistant_c_user_id_5a9081_idx_and_more"),
    ]

    operations = [
        # 1. Drop the broken constraint first.
        migrations.RemoveConstraint(
            model_name="weeklyplan",
            name="unique_active_weekly_plan_per_user_week",
        ),
        # 2. Clean any rows that would violate the new partial unique.
        migrations.RunPython(dedupe_inactive_plans, noop_reverse),
        # 3. Add the correct partial unique constraint.
        migrations.AddConstraint(
            model_name="weeklyplan",
            constraint=models.UniqueConstraint(
                fields=["user", "week_start_date"],
                condition=Q(is_active=True),
                name="unique_active_weekly_plan_per_user_week",
            ),
        ),
    ]
