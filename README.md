# FitnessCoachAI - Intelligent AI-Based Fitness Coach

> Personal trainer in your pocket: real-time form analysis with computer vision, an AI assistant powered by Gemini, and an adaptive 7-day workout plan that adjusts to your profile.

**Bachelor thesis project** - Kazakh-British Technical University (KBTU) - Almaty, 2026

![Status](https://img.shields.io/badge/status-thesis%20defended-success)
![Backend](https://img.shields.io/badge/backend-Django%205.2-092E20)
![Mobile](https://img.shields.io/badge/mobile-Kotlin-7F52FF)
![AI](https://img.shields.io/badge/AI-Gemini%202.5%20Flash-4285F4)
![CV](https://img.shields.io/badge/CV-MediaPipe-00897B)

---

## What it does

| Feature | What it means for the user |
|---|---|
| **Real-time form analysis** | Camera watches you, AI highlights bad joints with a red overlay while you exercise - 25-30 FPS, no cloud lag. |
| **Per-rep AI scoring** | Every workout ends with a real form-score percentage based on how clean your technique was. |
| **FitBot AI assistant** | Chat with a Gemini-powered coach that knows your profile, goals and limitations. Streaming responses. |
| **AI weekly plan** | 7-day adaptive schedule personalized from 11 profile parameters. Falls back to a template plan if the AI is down. |
| **Form Stats dashboard** | Custom bar chart of your last 20 workouts, distribution across quality zones (Excellent / Good / Average / Poor). |
| **Material 3 design** | Light/dark themes, Pulse design system (electric purple + mint). |

---

## Demo

**Live backend:** https://fitness-coach-ai-z10u.onrender.com/

🎥 Application Demo Video



Click the button above to watch the full demonstration of the FitnessCoachAI mobile application.

[Watch Demo](https://drive.google.com/file/d/1t-ur7mJpkqfIPkA1PTG0JHRpeSX04Oav/view?usp=sharing)



---

## Tech Stack

### Backend
- **Django 5.2** + Django REST Framework 3.14
- **PostgreSQL** (Neon Cloud, SSL + channel binding)
- **Daphne** ASGI server (handles SSE + WebSocket)
- **JWT auth** via simplejwt (access + refresh tokens)
- **Gemini 2.5 Flash** via google-genai SDK
- **Render Cloud** with GitHub auto-deploy + cron keep-alive

### Mobile (Android)
- **Kotlin** + Material 3 + ConstraintLayout
- **CameraX** + MediaPipe Pose Landmarker
- **Retrofit + OkHttp** with custom AuthInterceptor and TokenAuthenticator
- **EncryptedSharedPreferences** for secret storage
- **Markwon** for Markdown rendering in chat
- **Custom Canvas chart view** for Form Stats

### AI / ML
- **MediaPipe Pose** - 33 landmarks mapped to 18 anatomical keypoints
- **5 Random Forest classifiers** (scikit-learn) - push-up, squat, plank, crunch, shoulder press
- **Hybrid scoring** - ML verdict + geometric gates (body-line angle, knee-cave ratio, etc.)
- **Gemini 2.5 Flash** - chat, weekly-plan generation, 12-turn rolling memory

---

## Architecture

```
+---------------------------------------------------------+
|  PRESENTATION   Android (Kotlin, CameraX, MediaPipe)    |
+---------------------------------------------------------+
                          |  REST + SSE + WebSocket
                          v
+---------------------------------------------------------+
|  APPLICATION    Django REST API (auth, workouts,        |
|                 weekly plan, assistant, stats)          |
+---------------------------------------------------------+
                          |
       +------------------+------------------+
       v                  v                  v
+--------------+  +--------------+  +--------------+
| INTELLIGENCE |  | INTELLIGENCE |  | INTELLIGENCE |
| Gemini NLP   |  | RF CV models |  | Plan engine  |
+--------------+  +--------------+  +--------------+
                          |
                          v
+---------------------------------------------------------+
|  DATA           PostgreSQL on Neon (10 tables)          |
+---------------------------------------------------------+
```

---

## Repository layout

```
full_project/
+- intelligent-fitness-system-backend/    # Django backend
|   +- backend/
|       +- apps/
|       |   +- users/         # auth, profile
|       |   +- exercises/     # exercise catalog
|       |   +- workouts/      # sessions, history, stats
|       |   +- assistant/     # FitBot chat + weekly plan
|       +- config/            # settings, urls, asgi
|       +- scripts/           # reset_db, seed_exercises
|       +- requirements.txt
|       +- Dockerfile
|       +- start.sh
|
+- intelligent-fitness-system-front/      # Android client
    +- mobile/app/src/main/
        +- java/com/example/fitnesscoachai/
        |   +- ui/            # screens
        |   +- data/api/      # Retrofit + token store
        |   +- data/local/    # SharedPreferences helpers
        |   +- ui/workout/    # CV pipeline per exercise
        +- res/               # layouts, drawables, themes
```

---

## Getting Started

### Prerequisites
- Python 3.12+
- PostgreSQL (or use Neon Cloud free tier)
- Android Studio Hedgehog+
- A free Gemini API key from https://aistudio.google.com/apikey

### Backend

```bash
cd intelligent-fitness-system-backend/backend
python -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate
pip install -r requirements.txt

# Copy .env.example to .env and fill in your values
cp .env.example .env
# Required: DATABASE_URL, SECRET_KEY, GEMINI_API_KEY, GOOGLE_CLIENT_ID

python manage.py migrate
python manage.py loaddata initial_exercises.json
python manage.py createsuperuser
python manage.py runserver
```

API will be at `http://localhost:8000/`. Admin: `/admin/`. Swagger: `/swagger/`.

### Mobile

1. Open `intelligent-fitness-system-front/mobile/` in Android Studio
2. Sync Gradle
3. In `RetrofitClient.kt` set `BASE_URL` to your backend
4. Add your Google Sign-In OAuth client ID to `strings.xml`
5. Run on a device or emulator (min SDK 24)

> **Heads up:** Google Sign-In requires the SHA-1 of your debug keystore to be registered in Google Cloud Console.

---

## ML Models

Trained on hand-labeled video data. Each model uses 7-9 engineered geometric features.

| Exercise        | Features | Test accuracy | F1   |
|-----------------|----------|---------------|------|
| Push-up         | 7        | 88%           | 0.88 |
| Plank           | 7        | 90%           | 0.89 |
| Crunch          | 7        | 88%           | 0.87 |
| Squat           | 8        | 88%           | 0.87 |
| Shoulder press  | 9        | 87%           | 0.85 |

Models run on-device for sub-5 ms inference per frame on a mid-range Android.

**Form score formula:**

```
form_score = (correct_frames / total_analyzed_frames) * 100
```

Each frame is judged by **ML classifier + geometric gates** combined.

---

## Selected API endpoints

```
POST   /api/auth/register/                # email + password sign-up
POST   /api/auth/login/                   # JWT access + refresh
POST   /api/auth/google/                  # Google ID token sign-in
POST   /api/auth/token/refresh/

GET    /api/users/me/                     # current user profile
PATCH  /api/users/me/                     # update profile

GET    /api/exercises/categories/
GET    /api/exercises/?category=chest

GET    /api/assistant/weekly-plan/        # auto-generates if missing
POST   /api/assistant/weekly-plan/regenerate/
POST   /api/assistant/chat/               # one-shot reply
POST   /api/assistant/chat/stream/        # SSE streaming reply
GET    /api/assistant/chat/messages/      # paginated history

POST   /api/workouts/sessions/            # save a finished workout
GET    /api/workouts/history/?page=1      # paginated workout log
GET    /api/workouts/stats/               # snapshot + form-score history + zones
```

Full OpenAPI spec at `/swagger/`.

---

## Notable engineering decisions

- **On-device CV instead of cloud inference** - sub-100ms latency, no per-call cost, privacy-first (camera frames never leave the phone).
- **5-frame stabilizer** removes single-frame keypoint flicker before the classifier runs (~80% noise reduction).
- **Hybrid scoring** - pure ML alone gives too many false positives on edge poses, so each exercise also has a hard geometric gate (e.g. squat must satisfy `knee_cave_ratio < 2.35` to count).
- **Gemini fallback** - three retries with exponential backoff; if Gemini still fails, a deterministic template plan is generated locally on the server so the user always sees a plan.
- **Partial unique constraint** on the active weekly plan - Postgres `UNIQUE (user, week_start_date) WHERE is_active=TRUE`, so historical inactive plans don't conflict.
- **User-scoped local storage** - `history_count_<userId>` keys in SharedPreferences so different accounts on the same phone never see each other's data.

---

## License

Academic project - code is provided as-is for educational and portfolio purposes. Please get in touch before reusing it in a commercial product.

---

## Contact

**Nurzat Turganbek** - Backend & Computer Vision
- Email: nurzat.turganbek5@gmail.com
- Telegram: [@nurzat_turganbek](https://t.me/nurzat_turganbek)
- GitHub: [github.com/NurzatGitHub](https://github.com/NurzatGitHub)
