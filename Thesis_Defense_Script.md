# Thesis Defense — Speech Script

**Project:** Intelligent AI-Based Fitness Coach
**Team:** N. Turganbek (P3), Z. Tulegen (P2), A. Abdumalik (P1), T. Zhanibek (P4)
**Supervisor:** A. Issabek
**Target duration:** ~13 minutes + Q&A
**Format:** 4 speakers, smooth handoffs, 1 demo video + 2 live app demos

---

## Total timing

| Block | Slides | Speaker | Time |
|---|---|---|---|
| Intro + problem + methodology | 1–5 | **P1** (Abdumalik) | ~3:50 |
| Architecture (layers) | 6 (left) | **P2** (Tulegen) | 0:35 |
| **Database** | 6 (right) | **P3 Nurzat** | 0:30 |
| CV pipeline + ML + demo video | 7–9 | **P2** | 4:10 (incl. video ~1:30) |
| Backend + FitBot + Weekly Plan | 10–11 | **P3 Nurzat** | 2:00 |
| Results + conclusion + thank you | 12–14 | **P4** (Zhanibek) | 2:15 |
| **Total** | | | **~13:20** |

---

# PRESENTER 1 — Abdumalik

## Slide 1 — Title (0:30)

> "Good afternoon, dear members of the committee. We are pleased to present our Bachelor thesis project: **Intelligent AI-Based Fitness Coach** — a personal trainer based on video analysis with computer vision.
>
> Our team consists of four people: Nurzat Turganbek, Zhanibek Tulegen, Abdumalik Abdumalik, and Tulegen Zhanibek. Our supervisor is Mr. Issabek.
>
> Let me start with a quick overview of what we are going to present today."

## Slide 2 — Agenda (0:20)

> "The presentation is structured in seven blocks: introduction and relevance, what exactly we built, the methodology we followed, our four-layer architecture, the machine learning and computer vision part, the backend and AI assistant, and finally results and conclusion.
>
> The total talk is about 13 minutes, including one demo video in the middle and live demos of the assistant at the end."

## Slide 3 — Problem & Relevance (1:30)

> "Let's start with the problem.
>
> Three numbers tell the story. **Sixty-seven percent of beginners quit fitness in the first three months.** A personal trainer costs **at least fifty dollars per hour**, which most people cannot afford. And **bad form is the number one cause of gym injuries**. According to the World Health Organization, **one point four billion people worldwide are physically inactive**.
>
> Now, why don't existing apps solve this? Because they share three weaknesses: their plans are **static** — they ignore your age, body, and goals; they offer **no real-time feedback** — they show you a video but cannot see you; and there's a **knowledge gap** — beginners get hurt because no one corrects their technique.
>
> Our solution is an **AI coach in your pocket**. It's free, gives instant per-segment feedback, runs computer vision on-device with no cloud lag, and combines NLP coaching with adaptive weekly plans. The key word here is **personalization plus real-time correction**."

## Slide 4 — Research Object (0:45)

> "What exactly did we build?
>
> An intelligent mobile fitness coaching system that integrates three things: computer vision pose analysis, an NLP-powered AI assistant, and adaptive workout planning.
>
> Technically, the system stands on eight pillars: a native Android client in Kotlin; a Django REST backend on Render Cloud; a PostgreSQL database on Neon; a computer vision engine based on MediaPipe and ONNX Runtime; five Random Forest classifiers; the Gemini 2.5 Flash AI assistant; a resilient weekly-plan generator; and REST + WebSocket communication.
>
> All of these talk to each other in a single coherent system."

## Slide 5 — Methodology (0:45)

> "To get here, we used four research methods. **Comparative analysis** of five fitness apps on eight criteria to identify the gap. **System design** with a client-server pattern and four-layer abstraction. The **experimental method**, training five Random Forest classifiers on real video data. And **empirical testing** with twelve users on a five-point Likert scale.
>
> The work was organized in five phases: research, design, development, integration, and evaluation. Each phase produced concrete artifacts that fed into the next.
>
> Now I'll hand over to Zhanibek to walk you through the architecture."

---

# PRESENTER 2 — Tulegen

## Slide 6 — Architecture (FIRST HALF, 0:35)

> "Thank you. Architecturally, the system is split into **four layers**, each with a single responsibility.
>
> The **presentation layer** is the Android app — Kotlin, Material 3 design, CameraX for video capture, and MediaPipe with ONNX Runtime for on-device inference.
>
> The **application layer** is the Django backend — REST APIs for authentication, weekly plans, workouts, and the assistant.
>
> The **intelligence layer** holds the AI modules — Gemini 2.5 Flash for natural language, five Random Forest classifiers for computer vision, and the plan generator.
>
> And the bottom layer is data — for which I'll hand over to Nurzat."

---

# PRESENTER 3 — Nurzat (Backend lead)

## Slide 6 — Database (SECOND HALF, 0:30)

> "Thank you. The **data layer** runs on **PostgreSQL hosted on Neon Cloud**. The schema contains **ten relational tables** — visible on the right side of the slide.
>
> They are grouped by domain: **users** for accounts and profiles; **exercises** with categories and subcategories; **assistant** for the weekly plan, plan days, plan exercises, and chat messages; and **workouts** for sessions, exercises, and the progress snapshot we use to render stats.
>
> All migrations are ORM-managed, authentication uses **JWT with refresh tokens**, and communication with the mobile client happens over **REST plus WebSocket** — Django Channels with Daphne stream camera frames for live CV inference. Back to Zhanibek for the computer vision pipeline."

---

# PRESENTER 2 — Tulegen (continues)

## Slide 7 — CV Pipeline (1:10)

> "Thanks. The computer vision pipeline has **seven stages**.
>
> A camera frame from CameraX streams via WebSocket. **MediaPipe extracts 33 keypoints**, which we map down to 18. A **5-frame stabilizer** removes flickering by about 80%. We engineer **7 to 9 geometric features** per exercise — angles and ratios. The Random Forest classifier returns a **correct / incorrect** verdict with confidence. Rep counting uses a **phase machine with a two-frame streak** to filter out false positives. And finally we paint the skeleton: **green segments mean good form, red highlights the specific wrong joint, grey means return to position**.
>
> The numbers on the right: **98.4% pose accuracy, 33-to-18 keypoint reduction, 25 to 30 frames per second on-device, and under 5 milliseconds for ONNX inference**.
>
> One important detail — we use **hybrid scoring**. For plank, squat, and push-up the ML verdict is combined with a **geometry gate** — a hard threshold on body-line angle, knee-cave ratio, or trunk angle. This makes the scoring stricter than ML alone."

## Slide 8 — ML Training (1:00)

> "On the training side: all five classifiers are **Random Forest models trained with scikit-learn 1.5.1**. Binary classification, class weights set to balanced, exported to ONNX through skl2onnx for fast on-device inference.
>
> Each model uses 7 to 9 geometric features specific to its exercise, validated on a stratified hold-out split.
>
> The results — every model scores **above 85% test accuracy**: push-up at 88%, plank at 90%, crunch at 88%, squat at 88%, and shoulder press at 87%. The confusion matrices show diagonals dominating, so both false positives and false negatives stay low.
>
> Each ONNX file runs in under 5 milliseconds per frame — fast enough for real-time on a mid-range phone."

## Slide 9 — Live Demo Video (2:00 with video)

> "Instead of more numbers, let me show you what this looks like in practice.
>
> [PLAY VIDEO — about 1:30]
>
> While the video plays, pay attention to **five things**: the real-time skeleton overlay running at 25 to 30 FPS; **green segments** for correct form; **red segments** pointing at the exact joint that's wrong; the **rep counter** driven by the phase machine; and the **live form score** at the bottom, aggregated per session.
>
> [AFTER VIDEO ends, 5 seconds] As you saw, the user gets feedback continuously — not after the set, but during every single rep.
>
> Now I'll hand over to Nurzat for the backend and the AI assistant."

---

# PRESENTER 3 — Nurzat (continues)

## Slide 10 — Backend & FitBot (1:00)

> "Thank you. Let me cover the backend and the AI assistant.
>
> On the **backend side**: a REST API on Django REST Framework with JSON and JWT authentication; Daphne as the ASGI server handling Server-Sent Events and WebSocket; PostgreSQL on Neon; throttling configured at three scopes — anonymous, authenticated, and per-endpoint; authentication supports email-password plus Google OAuth, with refresh tokens; deployed on Render Cloud with a cron-job keep-alive and auto-deploy from GitHub; and we use Django Unfold for the admin panel.
>
> On the **FitBot side**: it runs on **Gemini 2.5 Flash through the google-genai SDK**. It has a **12-turn rolling context** — that's 24 messages of memory. Responses arrive via **Server-Sent Events token-by-token**, so the chat feels instant. The user profile is injected into the system prompt for personalization. All messages persist in the ChatMessage table. The Android client renders Markdown using the Markwon library. And we throttle at 30 messages per hour per user to control Gemini cost.
>
> *(If time allows, briefly open the app and send one message — show streaming.)*"

## Slide 11 — AI Weekly Plan (1:00)

> "Now the weekly plan — this is one of the most distinctive parts of the project.
>
> Generation is a **five-step pipeline**: the **user profile** with eleven parameters; the **Gemini call** in no-thinking mode for speed; **retry logic** with three attempts and exponential backoff; if Gemini still fails, a **deterministic fallback** template; and finally **exercise matching** from the database by level, location, and target muscle.
>
> The numbers below speak for themselves: **plan generation in under 1.2 seconds**, **100% delivery rate** thanks to retry plus fallback, **eleven profile parameters** for personalization, and a balanced **7-day Monday-through-Sunday schedule**.
>
> The most important point: **the system is resilient by design**. Even when the AI provider is unavailable, the user still gets a sensible plan.
>
> *(If time allows, open the app and pull-to-refresh the weekly plan — show the result.)*
>
> Now I'll pass it to Tulegen Zhanibek for the results."

---

# PRESENTER 4 — Zhanibek

## Slide 12 — Results & Evaluation (1:00)

> "Thank you. Let's look at the numbers we hit.
>
> Four headline metrics: **98.4% pose classification accuracy**, all **five ML models above 85% on the test split**, **plan generation under 1.2 seconds**, and a **4.5 out of 5 UX rating** for real-time feedback.
>
> Per-exercise rep accuracy is on the left: push-up 96%, plank 94%, crunch 92%, squat 91%, shoulder press 90%. Each uses a different mechanism — ML plus body-line gate for push-up, ML plus four geometry gates for plank, and so on.
>
> Usability testing on the right: we tested with twelve users. **100% completed their first workout**, 92% understood the form feedback colors, plan personalization rated **4.6 out of 5**, FitBot helpfulness **4.4 out of 5**, and **11 out of 12 users said they would use the app again**.
>
> All target KPIs were met."

## Slide 13 — Innovation & Future Work (1:00)

> "To summarize the originality.
>
> The quote at the top captures it: **no existing mobile system unifies NLP coaching, on-device pose estimation, and adaptive AI plan generation in one product**.
>
> Our **three innovations**: a **unified AI ecosystem** combining Gemini, Random Forest, and adaptive plans; **on-device computer vision** through MediaPipe and ONNX — low latency, no cloud cost for inference; and a **resilient plan engine** that achieves 100% delivery thanks to retry plus fallback.
>
> All five thesis objectives were achieved — listed on the left. Five apps analyzed, four-layer architecture designed, full stack built, AI modules integrated, and metrics evaluated.
>
> The roadmap on the right has three horizons: in the **short term**, expanding from five to ten-plus exercises and adding voice coaching; **mid term**, social features and an iOS port; and **long term**, replacing Random Forest with LSTM or Transformer and integrating with wearables for heart-rate and HRV."

## Slide 14 — Thank You (0:15)

> "Thank you for your attention. We're ready for your questions."

---

# Likely Q&A — Cheat Sheet

**Q: Why Random Forest and not a deep network?**
P2: "Random Forest gives 87–90% with **under 5 ms inference on mid-range phones and no GPU**. A CNN or LSTM would mean larger model files, possibly TFLite delegates, and battery cost. The roadmap includes LSTM/Transformer once we collect more data."

**Q: How do you handle a poor camera angle or bad lighting?**
P2: "Three layers of defense. MediaPipe drops keypoints below a visibility threshold. The 5-frame stabilizer absorbs single-frame errors. And if too few valid keypoints arrive, the skeleton is painted **grey** with a 'return to position' message — we never count reps from bad input."

**Q: How is form score actually calculated?**
P3 Nurzat: "Per-frame the model returns correct or incorrect. We sum correct frames over total frames analyzed during the session and multiply by 100. So a 70% form score means 70% of the time you were in the camera in correct form."

**Q: What about privacy?**
P3 Nurzat: "Camera frames **never leave the device** — all CV inference runs locally through ONNX Runtime. Only the rep count and aggregated form score go to the backend. JWT tokens are stored in **EncryptedSharedPreferences** on Android."

**Q: Why Gemini and not GPT or Claude?**
P3 Nurzat: "Gemini has a **generous free tier** through Google AI Studio — important for a student project — plus a fast 2.5 Flash model with thinking that can be disabled for low-latency replies. The integration via google-genai SDK is also simple."

**Q: What if Gemini is down?**
P3 Nurzat: "Two layers. **Three retries with exponential backoff** for transient errors. If all three fail, a **deterministic template plan** is generated locally on the server, personalized by the user's stated frequency and fitness level. The user always gets a plan."

**Q: How did you test usability with only 12 users?**
P4: "For a bachelor thesis 12 users is consistent with Nielsen's heuristic that 5 users uncover most issues. We focused on **qualitative insights** rather than statistical power: did onboarding work, were the colors readable, did users feel the plan was personalized."

**Q: Why on-device and not server-side CV?**
P2: "Three reasons. **Latency** — 25-30 FPS feedback is impossible with a network round-trip. **Cost** — running CV in the cloud at scale is expensive. **Privacy** — no video leaves the phone."

**Q: Database — why PostgreSQL and not SQLite?**
P3 Nurzat: "**Multi-user, multi-device.** A user can log in from a second phone and see the same workout history. SQLite would be per-device only. PostgreSQL on Neon Cloud also gives us automatic backups, an admin panel, and migrations."

**Q: Why Render and not AWS / GCP?**
P3 Nurzat: "Render has a **free tier with automatic GitHub deploys** — perfect for a thesis budget. The trade-off is a 15-minute idle sleep, which we mitigate with a cron-job ping every 10 minutes. For production we would move to a paid tier or AWS."

**Q: What was the biggest technical challenge?**
P2: "Getting **rep counting reliable**. The first version triggered on every up-down transition, including partial reps and false positives. We solved it with a **phase machine plus 2-frame streak** — a transition only counts when the new phase is stable across 2 frames."

**Q: Is the app available on Google Play?**
P3 Nurzat: "Not yet — Play Console requires a one-time $25 developer fee. For the defense we distribute the APK directly. Roadmap includes publishing once we add more exercises."

---

# Speaker handoff phrases (memorize these)

- P1 → P2 (end of slide 5): *"Now I'll hand over to Zhanibek to walk you through the architecture."*
- P2 → P3 (middle of slide 6): *"And the bottom layer is data — for which I'll hand over to Nurzat."*
- P3 → P2 (end of DB block, still slide 6): *"Back to Zhanibek for the computer vision pipeline."*
- P2 → P3 (end of slide 9): *"Now I'll hand over to Nurzat for the backend and the AI assistant."*
- P3 → P4 (end of slide 11): *"Now I'll pass it to Tulegen Zhanibek for the results."*
- P4 closes on slide 14.

---

# Rehearsal tips

1. Time each speaker individually with a stopwatch. Don't aim for exact — aim for **never exceeding** the slot.
2. The video on slide 9 should be **1:00 to 1:30 max**. If your raw recording is longer, trim it.
3. Live demos on slides 10 and 11 are **optional** — do them only if the projector is hooked up to a phone. If something is broken on stage, just talk through it.
4. Keep eye contact with the committee, not the slides.
5. The handoff is where the team looks coordinated — practice the **5 handoff phrases** until they're automatic.

Good luck!
