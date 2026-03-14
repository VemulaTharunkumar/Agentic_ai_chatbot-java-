# nlp_router.py
# Central NLP Intent Router for Agent Selection
# Rule-based + Extendable (ML / Embeddings ready)

import re

CODE_KEYWORDS = [
    "code", "program", "python", "java", "script", "algorithm",
    "compile", "function", "class", "logic"
]

RESEARCH_KEYWORDS = [
    "latest", "news", "research", "study", "source", "trend", "paper", "report"
]

REVIEW_KEYWORDS = [
    "review", "evaluate", "feedback", "rate", "critic", "analysis"
]

GREETING_PATTERNS = [
    # Simple greetings
    r"^hi$", r"^hello$", r"^hey$", r"^yo$", r"^sup$",
    r"^hii+$", r"^heyy+$",

    # Time-based greetings
    r"^good morning$", r"^good afternoon$", r"^good evening$", r"^good night$",

    # Polite greetings
    r"^hello there$", r"^hey there$", r"^hi there$",

    # How-are-you style
    r"^how are you$", r"^how r u$", r"^how are you doing$",
    r"^how's it going$", r"^how is it going$",
    r"^what's up$", r"^wassup$", r"^what up$",

    # Friendly / casual chat
    r"^nice to meet you$", r"^pleased to meet you$",
    r"^good to see you$", r"^long time no see$",

    # Bot-specific greetings
    r"^hey bot$", r"^hello bot$", r"^hi bot$",

    # Multiple words / casual typing
    r"^hi everyone$", r"^hello everyone$",
    r"^hey buddy$", r"^hey friend$"
]



def detect_intent(text: str) -> str:
    """
    Returns one of:
    GREETING, CODE, RESEARCH, REVIEW, GENERAL
    """
    text = text.lower().strip()

    if any(word in text for word in GREETING_PATTERNS):
        return "RESEARCH"

    # Code intent
    if any(word in text for word in CODE_KEYWORDS):
        return "CODE"

    # Research intent
    if any(word in text for word in RESEARCH_KEYWORDS):
        return "RESEARCH"

    # Review intent
    if any(word in text for word in REVIEW_KEYWORDS):
        return "REVIEW"

    return "GENERAL"


def select_agents(intent: str):
    """
    Decide which agents to run based on intent
    """

    if intent == "GREETING":
        return ["research"]

    if intent == "CODE":
        return ["planner", "research", "coder", "critic"]

    if intent == "RESEARCH":
        return ["planner", "research", "critic"]

    if intent == "REVIEW":
        return ["planner", "critic"]

    # GENERAL
    return ["research"]
