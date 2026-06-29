package com.agenticai.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OrchestratorService {

    private final GroqClientService groqClientService;

    public OrchestratorService(GroqClientService groqClientService) {
        this.groqClientService = groqClientService;
    }

    private static final List<String> CODE_KEYWORDS = Arrays.asList(
            "code", "program", "python", "java", "script", "algorithm",
            "compile", "function", "class", "logic"
    );

    private static final List<String> RESEARCH_KEYWORDS = Arrays.asList(
            "latest", "news", "research", "study", "source", "trend", "paper", "report"
    );

    private static final List<String> REVIEW_KEYWORDS = Arrays.asList(
            "review", "evaluate", "feedback", "rate", "critic", "analysis"
    );

    private static final List<String> GREETING_PATTERNS = Arrays.asList(
            "^hi$", "^hello$", "^hey$", "^yo$", "^sup$",
            "^hii+$", "^heyy+$",
            "^good morning$", "^good afternoon$", "^good evening$", "^good night$",
            "^hello there$", "^hey there$", "^hi there$",
            "^how are you$", "^how r u$", "^how are you doing$",
            "^how's it going$", "^how is it going$",
            "^what's up$", "^wassup$", "^what up$",
            "^nice to meet you$", "^pleased to meet you$",
            "^good to see you$", "^long time no see$",
            "^hey bot$", "^hello bot$", "^hi bot$",
            "^hi everyone$", "^hello everyone$",
            "^hey buddy$", "^hey friend$"
    );

    public String detectIntent(String text) {
        String lowerText = text.toLowerCase().trim();

        for (String patternStr : GREETING_PATTERNS) {
            Pattern pattern = Pattern.compile(patternStr);
            if (pattern.matcher(lowerText).find()) {
                return "GREETING"; // the python file returned "RESEARCH" for greetings in the intent check, but let's stick to GREETING and map to research agents if needed
            }
        }

        if (CODE_KEYWORDS.stream().anyMatch(lowerText::contains)) {
            return "CODE";
        }

        if (RESEARCH_KEYWORDS.stream().anyMatch(lowerText::contains)) {
            return "RESEARCH";
        }

        if (REVIEW_KEYWORDS.stream().anyMatch(lowerText::contains)) {
            return "REVIEW";
        }

        return "GENERAL";
    }

    public List<String> selectAgents(String intent) {
        if ("GREETING".equals(intent)) {
            return Arrays.asList("research");
        }
        if ("CODE".equals(intent)) {
            return Arrays.asList("planner", "research", "coder", "critic");
        }
        if ("RESEARCH".equals(intent)) {
            return Arrays.asList("planner", "research", "critic");
        }
        if ("REVIEW".equals(intent)) {
            return Arrays.asList("planner", "critic");
        }
        return Arrays.asList("research");
    }

    public String runPlanner(String task) {
        return groqClientService.think("Answer ONLY in steps,pictures and explanation. NEVER write code.", task);
    }

    public String runResearch(String task) {
        return groqClientService.think("Provide short factual research summary. NO code.", task);
    }

    public String runCoder(String task) {
        return groqClientService.think("Write ONLY executable code. NO explanation text.", task);
    }

    public String runCritic(String task) {
        String rolePrompt = "You are a STRICT expert reviewer.\n" +
                "RULES:\n" +
                "- NEVER reply with 'OK', 'Fine', or short answers\n" +
                "- Minimum 6 detailed sections REQUIRED\n" +
                "- Be critical, honest, and specific\n\n" +
                "Return feedback in this exact format:\n" +
                "Strengths:\n" +
                "- ...\n" +
                "Weaknesses:\n" +
                "- ...\n" +
                "Missing Points:\n" +
                "- ...\n" +
                "Accuracy Review:\n" +
                "- ...\n" +
                "Improvements:\n" +
                "- ...\n" +
                "Score out of 10:\n" +
                "- ...\n" +
                "Interview Verdict:\n" +
                "- ...\n";
        return groqClientService.think(rolePrompt, "Critically evaluate this response:\n\n" + task);
    }
}
