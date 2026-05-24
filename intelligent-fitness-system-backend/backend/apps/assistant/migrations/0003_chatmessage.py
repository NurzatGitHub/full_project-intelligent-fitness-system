from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("assistant", "0002_weeklyplanday_weeklyplanexercise_and_more"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="ChatMessage",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False)),
                ("role", models.CharField(
                    choices=[("user", "User"), ("assistant", "Assistant")],
                    max_length=20,
                )),
                ("content", models.TextField()),
                ("tokens_estimate", models.PositiveIntegerField(default=0)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("user", models.ForeignKey(
                    on_delete=models.deletion.CASCADE,
                    related_name="chat_messages",
                    to=settings.AUTH_USER_MODEL,
                )),
            ],
            options={
                "ordering": ["created_at"],
                "indexes": [
                    models.Index(fields=["user", "-created_at"], name="assistant_c_user_id_created_idx"),
                ],
            },
        ),
    ]
