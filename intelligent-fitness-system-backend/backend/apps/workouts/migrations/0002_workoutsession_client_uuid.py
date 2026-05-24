from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("workouts", "0001_initial"),
    ]

    operations = [
        migrations.AddField(
            model_name="workoutsession",
            name="client_uuid",
            field=models.CharField(blank=True, db_index=True, default="", max_length=64),
        ),
        migrations.AddConstraint(
            model_name="workoutsession",
            constraint=models.UniqueConstraint(
                fields=("user", "client_uuid"),
                condition=models.Q(client_uuid__gt=""),
                name="unique_workout_session_per_user_client_uuid",
            ),
        ),
    ]
