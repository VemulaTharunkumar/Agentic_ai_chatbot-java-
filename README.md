# Agentic AI System

A multi-agent AI system that processes user prompts using specialized AI agents.
The system includes a modern React frontend and a Java Spring Boot backend orchestrating multiple intelligent agents.

## 🚀 Project Overview

This project demonstrates an **Agentic AI architecture** where different AI agents collaborate to solve user queries.

Each agent has a specific responsibility such as planning, researching, coding, and reviewing results.

The system uses:

* React frontend (Vite)
* Java Spring Boot backend
* Multi-agent architecture
* NLP routing
* Modular agent system

---

## 🧠 Architecture

User Prompt → NLP Router → Planner Agent → Research Agent → Coder Agent → Critic Agent → Final Response

Agents communicate through a central **Orchestrator Controller**.

---

## 🤖 Agents in the System

### Planner Agent

Breaks the user prompt into actionable steps.

### Research Agent

Fetches relevant information using APIs and knowledge sources.

### Coder Agent

Generates code or solutions based on the research.

### Critic Agent

Reviews and improves the generated response.

---

## 📁 Project Structure

```
Agentic-AI-System
│
├── backend-java
│   ├── src
│   │   └── main
│   │       └── java
│   │           └── com.agenticai
│   └── pom.xml
│
├── frontend
│   ├── src
│   ├── components
│   └── vite.config.js
│
└── README.md
```

---

## ⚙️ Backend Setup

Go to backend folder:

```
cd backend-java
```

### Run Java Spring Boot server

```
mvn spring-boot:run
```

Backend will start at:

```
http://localhost:8080
```

---

## 💻 Frontend Setup

Go to frontend folder:

```
cd frontend
```

Install dependencies:

```
npm install
```

Run React app:

```
npm run dev
```

Frontend will run at:

```
http://localhost:5173
```

---

## 🔌 Tech Stack

Frontend:

* React
* Vite
* CSS

Backend:

* Java
* Spring Boot
* Groq API

Database:

* SQL Database

---

## ✨ Features

* Multi-agent AI system
* Modular architecture
* NLP prompt routing
* Modern React interface
* Extensible agent framework
* Backend API integration

---

## 📌 Future Improvements

* Add memory with SQL database
* Add authentication
* Deploy frontend and backend
* Add agent visualization
* Add streaming responses

---

## 👨‍💻 Author

Developed as an AI systems project demonstrating **Agentic AI architecture and multi-agent orchestration**.
