# Pathinator

Pathinator is a full-stack project with a **Kotlin Android frontend** and a **FastAPI backend**.  
The backend handles authentication and session management, while the Android app provides the user interface.

---

## 📂 Project Structure

Pathinator/
│── frontend/ # Kotlin Android app
│── backend/ # FastAPI backend
│── README.md


Each folder has its own `.gitignore` file for language-specific rules.

---

## 🚀 Backend (FastAPI)

### 🔧 Setup
1. Navigate to the backend folder:
   ```bash
   cd backend
(Optional) Create and activate a virtual environment:

python -m venv venv
source venv/bin/activate   # Linux/Mac
venv\Scripts\activate      # Windows


Install dependencies:

pip install fastapi uvicorn sqlalchemy asyncpg python-dotenv pydantic python-jose passlib


Run the backend server:

uvicorn main:app --host 0.0.0.0 --port 8000 --reload


API will be available at: http://localhost:8000

Interactive docs: http://localhost:8000/docs

## 📱 Frontend (Kotlin Android)
🔧 Setup

Open frontend/ in Android Studio.

Make sure RetrofitClient.kt points to your backend:

private const val BASE_URL = "http://<your-ip>:8000"


Use http://10.0.2.2:8000 if running on Android Emulator.

Use your local IP if testing on a physical device.

Use deployed URL (see below) if backend is on cloud.

Build and run the app on emulator or physical device.

🌍 Connecting Frontend & Backend

The Kotlin app makes HTTP requests to the FastAPI backend using Retrofit.

Ensure your backend is running before starting the app.

If you want to expose the backend publicly for mobile testing, you can use ngrok:

ngrok http 8000


This will give you a public URL (e.g., https://xxxx.ngrok.io) that you can use in RetrofitClient.kt.
