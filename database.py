import os
from datetime import datetime
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure
from bson.objectid import ObjectId
from dotenv import load_dotenv
import certifi
import bcrypt

# Load environment variables
load_dotenv()

# MongoDB Configuration
MONGO_URI = os.getenv("MONGO_URI")
DB_NAME = "agentic_ai_db"

# Collections
CHAT_COLLECTION = "chat_history"
USER_COLLECTION = "users"

print("Mongo URI:", MONGO_URI)


# -------------------- DB CONNECTION --------------------
def get_database():
    try:
        client = MongoClient(
            MONGO_URI,
            serverSelectionTimeoutMS=5000,
            tls=True,
            tlsCAFile=certifi.where()
        )
        client.admin.command('ping')
        return client[DB_NAME]
    except ConnectionFailure as e:
        print(f"❌ MongoDB connection failed: {e}")
        return None
    except Exception as e:
        print(f"❌ Unexpected DB error: {e}")
        return None


def get_chat_collection():
    db = get_database()
    return db[CHAT_COLLECTION] if db is not None else None   # ✅ FIXED


def get_user_collection():
    db = get_database()
    return db[USER_COLLECTION] if db is not None else None   # ✅ FIXED


# -------------------- USER AUTH --------------------
def get_or_create_user(username: str, password: str):
    print("🔥 get_or_create_user CALLED")

    collection = get_user_collection()
    if collection is None:
        print("❌ User collection not found")
        return None

    user = collection.find_one({"username": username})

    if user:
        print("✅ User exists")

        # 🔐 Check hashed password
        if bcrypt.checkpw(password.encode(), user["password"]):
            return str(user["_id"])
        else:
            print("❌ Incorrect password")
            return None

    else:
        print("🆕 Creating new user")

        hashed_password = bcrypt.hashpw(password.encode(), bcrypt.gensalt())

        new_user = {
            "username": username,
            "password": hashed_password,
            "created_at": datetime.utcnow()
        }

        try:
            result = collection.insert_one(new_user)
            print("✅ User inserted")
            return str(result.inserted_id)
        except Exception as e:
            print(f"❌ Insert failed: {e}")
            return None


# -------------------- SAVE CHAT --------------------
def save_chat_history(user_id: str, prompt: str, agent: str, response: str):
    collection = get_chat_collection()

    if collection is None:
        print("❌ Chat collection not found")
        return None

    try:
        chat_record = {
            "user_id": ObjectId(user_id),   # ✅ FIXED
            "prompt": prompt,
            "agent": agent,
            "response": response,
            "timestamp": datetime.utcnow()
        }

        result = collection.insert_one(chat_record)
        return str(result.inserted_id)

    except Exception as e:
        print(f"❌ Failed to save chat history: {e}")
        return None


# -------------------- GET CHATS --------------------
def get_recent_chats(user_id: str, limit: int = 10):
    collection = get_chat_collection()

    if collection is None:
        print("❌ Chat collection not found")
        return []

    try:
        chats = collection.find(
            {"user_id": ObjectId(user_id)}   # ✅ FIXED
        ).sort("timestamp", -1).limit(limit)

        return list(chats)

    except Exception as e:
        print(f"❌ Failed to retrieve chat history: {e}")
        return []


# -------------------- DELETE CHAT --------------------
def delete_chat(chat_id: str):
    collection = get_chat_collection()

    if collection is None:
        print("❌ Chat collection not found")
        return False

    try:
        result = collection.delete_one({"_id": ObjectId(chat_id)})
        return result.deleted_count > 0

    except Exception as e:
        print(f"❌ Failed to delete chat: {e}")
        return False