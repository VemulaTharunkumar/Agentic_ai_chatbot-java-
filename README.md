# Agentic AI System

## Overview

Agentic AI System is a **multi-agent AI application** built using **Python and Streamlit** that analyzes user prompts, plans solutions, performs research, generates code, and reviews outputs through a coordinated set of AI agents.

The system demonstrates how multiple AI agents collaborate in a structured workflow. It also integrates **MongoDB Atlas** to store prompt history and conversations.

This project demonstrates concepts from **Agentic AI, multi-agent systems, and modern AI application architecture**, making it a strong **portfolio or resume project**.

---

## Features

### 1. Multi-Agent Architecture

The system uses multiple specialized agents:

* **Planner Agent** – Breaks down the user prompt into tasks.
* **Research Agent** – Gathers relevant information.
* **Coder Agent** – Generates code or structured solutions.
* **Critic Agent** – Reviews and improves the output.

These agents work sequentially to simulate **AI reasoning and collaboration**.

---

### 2. Prompt History Storage

User prompts and AI responses are stored in **MongoDB Atlas**.

Features:

* Stores prompts and responses
* Maintains conversation records
* Limits storage to the **latest 15 conversations**
* Automatically removes older conversations when the limit is exceeded

Each conversation includes:

* conversation_id
* title
* messages
* created_at
* updated_at

---

### 3. Chat Interface

The system provides a **chat-based interface** built with Streamlit.

Capabilities:

* Users can enter prompts through the chat interface
* AI responses appear in the main UI
* The system processes prompts using the **Agentic AI pipeline**

---

### 4. MongoDB Integration

MongoDB Atlas is used as the database for storing conversations.

Collection used:

* **conversations**

Example conversation document:

```json id="i4ubnb"
{
  "conversation_id": "conv_001",
  "title": "Binary Search Explanation",
  "messages": [
    {
      "prompt": "Explain binary search",
      "response": "Binary search works by dividing the array..."
    }
  ],
  "created_at": "2026-03-11T19:30:00",
  "updated_at": "2026-03-11T19:35:00"
}
```

---

## System Architecture

User Prompt
↓
NLP Router
↓
Planner Agent
↓
Research Agent
↓
Coder Agent
↓
Critic Agent
↓
Final Response

---

## Project Structure

```id="xfopfa"
agentic-ai-system
│
├── app.py
├── database.py
│
├── agents
│   ├── planner.py
│   ├── research.py
│   ├── coder.py
│   └── critic.py
│
├── router
│   └── nlp_router.py
├──ui
│   ├── dashboard.py
│   ├── nlp_router.py
|
├── requirements.txt
└── README.md
```

---

## Technologies Used

* **Python**
* **Streamlit**
* **MongoDB Atlas**
* **PyMongo**
* **Multi-Agent AI Architecture**

---

## Installation

### 1. Clone the Repository

git clone https://github.com/yourusername/agentic-ai-system.git
cd agentic-ai-system


### 2. Create Virtual Environment

python -m venv venv

Activate the environment:

Windows:

venv\Scripts\activate

Linux / Mac:

source venv/bin/activate


### 3. Install Dependencies

pip install -r requirements.txt


## Configuration

### 4. Add API Keys

Create a `.env` file in the root directory of the project.

Project structure:

agentic-ai-system
│
├── app.py
├── requirements.txt
├── database.py
├── agents
└── .env


Add your API keys inside `.env`:

OPENAI_API_KEY=your_openai_api_key


Load the API key in your Python code:

from dotenv import load_dotenv
import os

load_dotenv()

api_key = os.getenv("OPENAI_API_KEY")


### 5. Configure MongoDB

Create a MongoDB Atlas cluster and obtain the connection string.

Update the connection string in `database.py`:

MONGO_URI = "your_mongodb_connection_string"

### 6. Run the Application

streamlit run app.py

The application will open automatically in your browser.


## Security Note

Add `.env` to `.gitignore` to prevent exposing API keys.

.env
venv/
__pycache__/

## Usage

1. Open the application.
2. Enter a prompt in the chat interface.
3. The Agentic AI pipeline processes the request.
4. View the AI-generated response.

---

## Future Improvements

Planned improvements include:

* User authentication system
* Clickable conversation history
* Chat session restoration
* Agent reasoning visualization
* Vector database integration
* Retrieval-Augmented Generation (RAG)
* Cloud deployment

---

## Author

Tharun Kumar

---

## License

This project is intended for **educational and research purposes**.
