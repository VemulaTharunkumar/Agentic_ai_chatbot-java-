import os
from datetime import datetime
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure
from bson.objectid import ObjectId
from dotenv import load_dotenv
import certifi
# Load environment variables
load_dotenv()

# MongoDB Configuration
MONGO_URI = os.getenv("MONGO_URI")
DB_NAME = "agentic_ai_db"
COLLECTION_NAME = "chat_history"
print("Mongo URI:", MONGO_URI)

def get_db_collection():
    """
    Connects to MongoDB and returns the chat_history collection.
    """
    try:
        client = MongoClient(
         MONGO_URI,
        serverSelectionTimeoutMS=5000,
        tls=True,
        tlsCAFile=certifi.where()
        )
        # Verify connection
        client.admin.command('ping')
        db = client[DB_NAME]
        return db[COLLECTION_NAME]
    except ConnectionFailure as e:
        print(f"MongoDB connection failed: {e}")
        return None
    except Exception as e:
        print(f"An error occurred connecting to MongoDB: {e}")
        return None

def get_user_collection():
    """ Connects to MongoDB and returns the users collection """
    try:
        client = MongoClient(
            MONGO_URI,
            serverSelectionTimeoutMS=5000,
            tls=True,
            tlsCAFile=certifi.where()
        )

        # ✅ Verify connection
        client.admin.command('ping')

        db = client[DB_NAME]
        return db["users"]

    except Exception as e:
        print(f"MongoDB connection failed (users): {e}")
        return None

def verify_user(username: str, password: str) -> bool:
    print("🔥 verify_user CALLED")   # DEBUG
    collection = get_user_collection()
    if collection is None:
        print("❌ Collection is None")
        return False
        
    user = collection.find_one({"username": username})
    if user:
        print("✅ User exists")
        return user.get("password") == password
    return False

def create_user(username: str, password: str) -> bool:
    print("🔥 create_user CALLED")   # DEBUG
    collection = get_user_collection()
    if collection is None:
        print("❌ Collection is None")
        return False
        
    user = collection.find_one({"username": username})
    if user:
        print("❌ User already exists")
        return False
    
    print("🆕 Creating new user")
    new_user = {
        "username": username,
        "password": password,
        "created_at": datetime.utcnow()
    }

    try:
        collection.insert_one(new_user)
        print("✅ User inserted")
        return True
    except Exception as e:
        print(f"❌ Insert failed: {e}")
        return False

def save_chat_history(user_id: str, prompt: str, agent: str, response: str):
    """
    Saves a chat record to the MongoDB database.

    Args:
        user_id (str): Unique identifier for the user session.
        prompt (str): The user's input prompt.
        agent (str): The agent(s) that generated the response.
        response (str): The final generated response.
    """
    collection = get_db_collection()
    if collection is not None:
        chat_record = {
            "user_id": user_id,
            "prompt": prompt,
            "agent": agent,
            "response": response,
            "timestamp": datetime.utcnow()
        }
        try:
            result = collection.insert_one(chat_record)
            return str(result.inserted_id)
        except Exception as e:
            print(f"Failed to save chat history: {e}")
            return None
    return None

def get_recent_chats(user_id: str, limit: int = 10):
    """
    Retrieves the most recent chat histories for a specific user.

    Args:
        user_id (str): Unique identifier for the user session.
        limit (int): Maximum number of records to return.

    Returns:
        list: A list of chat history documents.
    """
    collection = get_db_collection()
    if collection is not None:
        try:
            # Sort by timestamp descending to get the most recent, then limit
            cursor = collection.find({"user_id": user_id}).sort("timestamp", -1).limit(limit)
            return list(cursor)
        except Exception as e:
            print(f"Failed to retrieve chat history: {e}")
            return []
    return []

def delete_chat(chat_id: str):
    """
    Deletes a specific chat record by its ID.
    """
    collection = get_db_collection()
    if collection is not None:
        try:
            result = collection.delete_one({"_id": ObjectId(chat_id)})
            return result.deleted_count > 0
        except Exception as e:
            print(f"Failed to delete chat: {e}")
            return False
    return False
