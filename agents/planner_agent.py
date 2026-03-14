from agents.base_agent import BaseAgent

class PlannerAgent(BaseAgent):
    def __init__(self):
        super().__init__(
            "Answer ONLY in steps,pictures and explanation. NEVER write code."
        )
