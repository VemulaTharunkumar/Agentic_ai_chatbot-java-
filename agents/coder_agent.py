from agents.base_agent import BaseAgent

class CoderAgent(BaseAgent):
    def __init__(self):
        super().__init__(
            "Write ONLY executable code. NO explanation text."
        )
