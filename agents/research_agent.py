from agents.base_agent import BaseAgent

class ResearchAgent(BaseAgent):
    def __init__(self):
        super().__init__(
            "Provide short factual research summary. NO code."
        )

    def run(self, topic):
        return self.think(topic)
