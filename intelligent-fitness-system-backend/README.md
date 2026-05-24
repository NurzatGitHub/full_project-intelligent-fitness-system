# 🏋️‍♂️ Intelligent Fitness System

AI-powered fitness trainer application with posture analysis.

## 📱 Features
- **User Authentication** (JWT tokens)
- **Workout Tracking** 
- **AI Movement Analysis** using pose estimation
- **Real-time Feedback** on exercise form

## 🏗️ Architecture
- **Backend**: Django REST Framework + PostgreSQL
- **Frontend**: Android (Kotlin)
- **AI Model**: MediaPipe/TensorFlow for pose estimation

## 🚀 Quick Start

### Backend Setup
```bash
cd backend

# With Docker (recommended)
docker-compose up -d

# Or manually
python -m venv venv
venv\Scripts\activate  # Windows
pip install -r requirements.txt
python manage.py migrate
python manage.py runserver 0.0.0.0:8000

API Endpoints
POST /api/auth/register/ - User registration

POST /api/auth/login/ - User login

GET /api/auth/profile/ - User profile

POST /api/ai/analyze/ - Analyze exercise video
