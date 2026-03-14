from agents.planner_agent import PlannerAgent
from agents.research_agent import ResearchAgent
from agents.coder_agent import CoderAgent
from agents.critic_agent import CriticAgent
import re

class Orchestrator:
    def __init__(self):
        self.planner = PlannerAgent()
        self.research = ResearchAgent()
        self.coder = CoderAgent()
        self.critic = CriticAgent()

    # Only generate code if user EXPLICITLY asks
    def is_code_task(self, goal):
        keywords = ["code", "program", "logic", "script", "implementation"]
        return any(word in goal.lower() for word in keywords)

    # HARD REMOVE EVERYTHING THAT LOOKS LIKE CODE
    def remove_code_completely(self, text):
        # Remove fenced code blocks
        text = re.sub(r"```.*?```", "", text, flags=re.DOTALL)

        # Remove inline code
        text = re.sub(r"`.*?`", "", text)

        # Remove Python / Java patterns
        code_patterns = [
            r"(?m)^.*def .*",
            r"(?m)^.*class .*",
            r"(?m)^.*import .*",
            r"(?m)^.*public .*",
            r"(?m)^.*static .*",
            r"(?m)^.*if __name__.*",
            r"(?m)^.*print\(.*",
            r"(?m)^.*;.*",
            r"\{.*?\}",
        ]

        for pattern in code_patterns:
            text = re.sub(pattern, "", text)

        # Remove extra empty lines
        text = "\n".join(line for line in text.splitlines() if line.strip())

        return text.strip()

    def run(self, goal):
        print("\n🎯 USER TASK:", goal)

        research = self.research.run(goal)

        # CODE MODE (ONLY IF USER ASKED)
        if self.is_code_task(goal):
            result = self.coder.think(goal)

        # TEXT MODE — FORCE STEP ANSWER
        else:
            result = self.planner.think(
                f"Give ONLY steps and explanation. NEVER include code. Task: {goal}"
            )
            result = self.remove_code_completely(result)

        critique = self.critic.think(result)

        # Removed self.memory.save as memory is undefined in Orchestrator

        print("\n🌐 RESEARCH:\n", research)
        print("\n✅ FINAL ANSWER:\n", result)
        print("\n🧪 FEEDBACK:\n", critique)

        return result
