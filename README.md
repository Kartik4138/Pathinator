# Pathinator 🗺️

Pathinator is a full-stack project featuring a route-planning Android application powered by a robust FastAPI backend. The backend manages user authentication, sessions, and location data, while the native Android frontend provides a seamless and responsive user experience.

---

## 📋 Table of Contents

- [About The Project](#about-the-project)
- [✨ Features](#-features)
- [🛠️ Tech Stack](#️-tech-stack)
- [📂 Project Structure](#-project-structure)
- [🚀 Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Backend Setup (FastAPI)](#backend-setup-fastapi)
  - [Frontend Setup (Android)](#frontend-setup-android)
- [🌍 Connecting Frontend & Backend](#-connecting-frontend--backend)
- [📄 License](#-license)

---

## About The Project

This project demonstrates a modern mobile application architecture. The Kotlin-based Android app communicates with a Python FastAPI server via a REST API to perform actions like user registration, login, and storing/retrieving path data.

---

## ✨ Features

* **User Authentication**: Secure user registration and login functionality.
* **Session Management**: JWT-based authentication to manage user sessions.
* **Interactive Map**: Users can view and interact with a map to plan routes.
* **RESTful API**: Clean, documented, and efficient API built with FastAPI.

---

## 🛠️ Tech Stack

| Component      | Technology                                                                                                                              |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| **Backend** | [Python](https://www.python.org/), [FastAPI](https://fastapi.tiangolo.com/), [SQLAlchemy](https://www.sqlalchemy.org/), [Uvicorn](https://www.uvicorn.org/), [PostgreSQL](https://www.postgresql.org/) |
| **Frontend** | [Kotlin](https://kotlinlang.org/), [Android SDK](https://developer.android.com/), [Retrofit2](https://square.github.io/retrofit/), [Google Maps API](https://developers.google.com/maps)           |
| **Database** | [PostgreSQL](https://www.postgresql.org/) (via `asyncpg`)                                                                                |

---

## 📂 Project Structure

The repository is organized into two main directories for the frontend and backend applications.

```
Pathinator/
│
├── frontend/           # Kotlin Android application source code
│   └── .gitignore
│
├── backend/            # FastAPI backend application source code
│   └── .gitignore
│
└── README.md           # This file
```
> Each folder contains its own `.gitignore` file tailored for its specific language and framework.

---

## 🚀 Getting Started

Follow these instructions to get a local copy of the project up and running on your machine for development and testing purposes.

### Prerequisites

Make sure you have the following software installed:
* Python 3.8+
* Android Studio
* PostgreSQL Server

### Backend Setup (FastAPI)

1.  **Navigate to the backend directory:**
    ```bash
    cd backend
    ```

2.  **(Optional) Create and activate a virtual environment:**
    * **Linux/Mac:**
        ```bash
        python3 -m venv venv
        source venv/bin/activate
        ```
    * **Windows:**
        ```bash
        python -m venv venv
        venv\Scripts\activate
        ```

3.  **Install dependencies from `requirements.txt`:**
    > **Note**: It's a best practice to freeze your dependencies. Create a `requirements.txt` file by running `pip freeze > requirements.txt` in your terminal after installing the packages below.
    ```bash
    pip install -r requirements.txt
    ```
    If the file doesn't exist, install packages manually and create it:
    ```bash
    pip install fastapi uvicorn sqlalchemy asyncpg python-dotenv pydantic python-jose passlib[bcrypt]
    pip freeze > requirements.txt
    ```

4.  **Set up environment variables:**
    Create a file named `.env` in the `backend` directory by copying the example template:
    ```bash
    cp .env.example .env
    ```
    Now, edit the `.env` file with your database credentials and a secret key for JWT.
    ```ini
    # .env
    DATABASE_URL="postgresql+asyncpg://user:password@host:port/dbname"
    SECRET_KEY="your-super-secret-key-for-jwt"
    ALGORITHM="HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES=30
    ```

5.  **Run the backend server:**
    The `--reload` flag enables hot-reloading for development.
    ```bash
    uvicorn main:app --host 0.0.0.0 --port 8000 --reload
    ```
    Your API is now live and accessible:
    * **API URL**: `http://localhost:8000`
    * **Interactive Docs (Swagger UI)**: `http://localhost:8000/docs`

### Frontend Setup (Android)

1.  **Open the project:**
    Launch Android Studio and select `Open` to open the `frontend/` directory.

2.  **Add Google Maps API Key:**
    Navigate to `frontend/app/src/main/AndroidManifest.xml` and insert your Google Maps API key in the specified metadata tag.
    ```xml
    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="YOUR_API_KEY_HERE" />
    ```

3.  **Configure the Backend URL:**
    Open the `RetrofitClient.kt` file and set the `BASE_URL` constant to point to your running backend instance.
    ```kotlin
    // private const val BASE_URL = "http://<your-ip>:8000"

    // Use this IP for the standard Android Emulator
    private const val BASE_URL = "[http://10.0.2.2:8000](http://10.0.2.2:8000)"
    ```
    * **Android Emulator**: Use `http://10.0.2.2:8000` to connect to your `localhost`.
    * **Physical Device**: Use your computer's local network IP (e.g., `http://192.168.1.10:8000`).
    * **Deployed Backend**: Use the public URL of your deployed backend.

4.  **Build and Run:**
    Sync the Gradle files and run the app on an Android emulator or a physical device.

---

## 🌍 Connecting Frontend & Backend

For testing on a physical device without deploying the backend, you can expose your local server to the internet using a tunneling service like **ngrok**.

1.  **Install ngrok** and authenticate it.

2.  **Expose your backend port:**
    ```bash
    ngrok http 8000
    ```

3.  **Update your frontend:**
    `ngrok` will provide a public HTTPS URL (e.g., `https://random-string.ngrok.io`). Copy this URL and set it as the `BASE_URL` in your `RetrofitClient.kt` file.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` file for more information.
