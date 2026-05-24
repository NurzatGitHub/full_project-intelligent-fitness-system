#!/usr/bin/env python
"""
Full database reset for the Fitness Coach AI backend.

What it does (in order):
  1. Drops EVERY table in the public schema (Neon-safe; uses DROP SCHEMA CASCADE).
  2. Re-runs all Django migrations from scratch.
  3. Seeds exercise catalog via `manage.py seed_exercises`.
  4. Optionally creates a superuser if SUPERUSER_EMAIL + SUPERUSER_PASSWORD are set.

Usage (from the backend/ folder, with venv activated and .env loaded):

    python scripts/reset_db.py

Or with explicit confirmation skipped (CI / scripted use):

    RESET_CONFIRM=YES python scripts/reset_db.py

ENV VARS USED:
  DATABASE_URL          — required (Neon connection string with sslmode=require)
  SUPERUSER_EMAIL       — optional
  SUPERUSER_PASSWORD    — optional
  SUPERUSER_USERNAME    — optional, defaults to email prefix
  RESET_CONFIRM         — set to "YES" to skip the interactive y/n prompt
"""
import os
import sys
import subprocess
from pathlib import Path

BACKEND_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BACKEND_DIR))
sys.path.insert(0, str(BACKEND_DIR / "apps"))

# Load .env BEFORE checking DATABASE_URL / before Django setup.
try:
    from dotenv import load_dotenv
    load_dotenv(BACKEND_DIR / ".env")
except ImportError:
    # python-dotenv not installed yet — fail with a clear message later.
    pass

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "config.settings")


def _confirm() -> bool:
    if os.getenv("RESET_CONFIRM", "").upper() == "YES":
        return True
    print("=" * 64)
    print(" THIS WILL DROP EVERY TABLE IN YOUR DATABASE")
    print(" All users, workouts, plans, exercises will be permanently lost.")
    print("=" * 64)
    answer = input("Type 'RESET' (uppercase) to continue: ").strip()
    return answer == "RESET"


def _drop_schema():
    """DROP SCHEMA public CASCADE — wipes everything Neon-compatible style."""
    import django
    django.setup()
    from django.db import connection

    print("[1/4] Dropping public schema…")
    with connection.cursor() as cur:
        cur.execute("DROP SCHEMA IF EXISTS public CASCADE;")
        cur.execute("CREATE SCHEMA public;")
        cur.execute("GRANT ALL ON SCHEMA public TO public;")
    print("       done. All tables dropped, fresh empty schema created.")


def _run(label, *args):
    print(f"[{label}] {' '.join(args)}")
    subprocess.check_call([sys.executable, "manage.py", *args], cwd=str(BACKEND_DIR))


def _create_superuser():
    email = os.getenv("SUPERUSER_EMAIL")
    password = os.getenv("SUPERUSER_PASSWORD")
    if not email or not password:
        print("[4/4] SUPERUSER_EMAIL / SUPERUSER_PASSWORD not set — skipping admin creation.")
        return

    username = os.getenv("SUPERUSER_USERNAME") or email.split("@")[0]

    import django
    django.setup()
    from django.contrib.auth import get_user_model
    User = get_user_model()

    if User.objects.filter(email__iexact=email).exists():
        print(f"[4/4] Superuser with email {email} already exists — skipping.")
        return

    user = User.objects.create_superuser(email=email, username=username, password=password)
    print(f"[4/4] Created superuser: {user.email}")


def main():
    if "DATABASE_URL" not in os.environ:
        print("ERROR: DATABASE_URL is not set. Load your .env or export it first.")
        sys.exit(1)

    if not _confirm():
        print("Aborted.")
        sys.exit(0)

    _drop_schema()
    _run("2/4", "migrate", "--noinput")
    _run("3/4", "seed_exercises")
    _create_superuser()
    print()
    print("Database reset complete.")


if __name__ == "__main__":
    main()
