package com.agenticai.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class OrchestratorService {

    private final GroqClientService groqClientService;

    private final Gson gson = new Gson();

    public OrchestratorService(GroqClientService groqClientService) {
        this.groqClientService = groqClientService;
    }

    // ============================================================
    // LLM ROUTER
    // ============================================================

    public RoutingDecision route(String userRequest) {

        String routerPrompt = """
                You are an intelligent AI router.

                Your job is to understand the user's request and
                decide which AI agents are required.

                IMPORTANT:
                DO NOT use keyword matching.
                Understand the meaning and intention of the request.

                AVAILABLE AGENTS:

                planner
                - Breaks complex problems into steps.
                - Creates an execution plan.

                research
                - Performs research.
                - Handles factual questions.
                - Handles latest/current information.
                - Handles comparisons and technical documentation.

                coder
                - Writes code.
                - Debugs code.
                - Fixes code.
                - Designs software implementations.

                critic
                - Reviews answers.
                - Reviews code.
                - Finds mistakes.
                - Suggests improvements.

                ROUTING RULES:

                Simple greeting:
                -> no agents

                Simple factual question:
                -> research

                Complex problem:
                -> planner may be needed

                Coding request:
                -> coder

                Complex coding request:
                -> planner -> coder -> critic

                Coding request requiring research:
                -> planner -> research -> coder -> critic

                Research request:
                -> research

                Research requiring evaluation:
                -> research -> critic

                Review/debug/evaluate request:
                -> critic

                Use ONLY the agents that are actually necessary.

                Return ONLY valid JSON.

                EXACT FORMAT:

                {
                  "intent": "GREETING | GENERAL | CODE | RESEARCH | REVIEW",
                  "agents": [
                    {
                      "name": "planner | research | coder | critic",
                      "order": 1
                    }
                  ],
                  "needsWeb": false,
                  "needsCode": false,
                  "complexity": "LOW | MEDIUM | HIGH"
                }
                """;

        try {

            String response =
                    groqClientService.think(routerPrompt, userRequest);

            System.out.println("\n========== LLM ROUTER ==========");
            System.out.println(response);
            System.out.println("=================================\n");

            return parseRoutingResponse(response);

        } catch (Exception e) {

            System.err.println(
                    "Router error: " + e.getMessage()
            );

            return fallbackRouting();
        }
    }

    // ============================================================
    // PARSE LLM JSON
    // ============================================================

    private RoutingDecision parseRoutingResponse(String response) {

        String json = cleanJson(response);

        JsonObject root =
                JsonParser.parseString(json).getAsJsonObject();

        RoutingDecision decision = new RoutingDecision();

        // Intent
        if (root.has("intent")) {
            decision.setIntent(
                    root.get("intent").getAsString()
            );
        } else {
            decision.setIntent("GENERAL");
        }

        // needsWeb
        if (root.has("needsWeb")) {
            decision.setNeedsWeb(
                    root.get("needsWeb").getAsBoolean()
            );
        }

        // needsCode
        if (root.has("needsCode")) {
            decision.setNeedsCode(
                    root.get("needsCode").getAsBoolean()
            );
        }

        // complexity
        if (root.has("complexity")) {
            decision.setComplexity(
                    root.get("complexity").getAsString()
            );
        } else {
            decision.setComplexity("LOW");
        }

        // Agents
        List<AgentRoute> agents = new ArrayList<>();

        if (root.has("agents")) {

            JsonArray agentArray =
                    root.getAsJsonArray("agents");

            for (int i = 0; i < agentArray.size(); i++) {

                JsonObject agentObject =
                        agentArray.get(i).getAsJsonObject();

                AgentRoute agent = new AgentRoute();

                agent.setName(
                        agentObject
                                .get("name")
                                .getAsString()
                );

                agent.setOrder(
                        agentObject
                                .get("order")
                                .getAsInt()
                );

                if (isValidAgent(agent.getName())) {
                    agents.add(agent);
                }
            }
        }

        // Sort according to LLM-selected execution order
        agents.sort(
                Comparator.comparingInt(
                        AgentRoute::getOrder
                )
        );

        decision.setAgents(agents);

        return decision;
    }

    // ============================================================
    // CLEAN JSON
    // ============================================================

    private String cleanJson(String response) {

        if (response == null || response.isBlank()) {
            throw new RuntimeException(
                    "LLM returned empty router response"
            );
        }

        String result = response.trim();

        // Remove ```json
        if (result.startsWith("```json")) {
            result = result.substring(7);
        }

        // Remove ```
        else if (result.startsWith("```")) {
            result = result.substring(3);
        }

        if (result.endsWith("```")) {
            result = result.substring(
                    0,
                    result.length() - 3
            );
        }

        return result.trim();
    }

    // ============================================================
    // VALID AGENTS
    // ============================================================

    private boolean isValidAgent(String agent) {

        return "planner".equals(agent)
                || "research".equals(agent)
                || "coder".equals(agent)
                || "critic".equals(agent);
    }

    // ============================================================
    // FALLBACK
    // ============================================================

    private RoutingDecision fallbackRouting() {

        RoutingDecision decision =
                new RoutingDecision();

        decision.setIntent("GENERAL");
        decision.setNeedsWeb(false);
        decision.setNeedsCode(false);
        decision.setComplexity("LOW");

        return decision;
    }

    // ============================================================
    // AGENT EXECUTION
    // ============================================================

    public String executeAgent(
            String agentName,
            String task) {

        switch (agentName) {

            case "planner":
                return runPlanner(task);

            case "research":
                return runResearch(task);

            case "coder":
                return runCoder(task);

            case "critic":
                return runCritic(task);

            default:
                throw new IllegalArgumentException(
                        "Unknown agent: " + agentName
                );
        }
    }

    // ============================================================
    // PLANNER
    // ============================================================

    public String runPlanner(String task) {

        return groqClientService.think(
                """
                You are the planning agent.

                Analyze the task.

                Create a clear step-by-step execution plan.

                Do NOT write implementation code.

                Focus on:
                - requirements
                - approach
                - steps
                - expected result
                """,
                task
        );
    }

    // ============================================================
    // RESEARCH
    // ============================================================

    public String runResearch(String task) {

        return groqClientService.think(
                """
                You are the research agent.

                Analyze the task and provide accurate
                factual information.

                Focus on:
                - important facts
                - relevant concepts
                - comparisons
                - technical information
                - current information when required

                Do not unnecessarily write code.
                """,
                task
        );
    }

    // ============================================================
    // CODER
    // ============================================================

    public String runCoder(String task) {

        return groqClientService.think(
                """
                You are the coding agent.

                Solve the programming task.

                Write correct executable code.

                Follow the requested programming language.

                Include necessary imports.

                Make the solution production-quality
                when appropriate.
                """,
                task
        );
    }

    // ============================================================
    // CRITIC
    // ============================================================

    public String runCritic(String task) {

        return groqClientService.think(
                """
                You are a strict expert critic.

                Carefully review the provided work.

                Check:

                1. Correctness
                2. Logical errors
                3. Missing requirements
                4. Technical problems
                5. Edge cases
                6. Improvements
                7. Overall quality

                Return:

                Strengths:
                - ...

                Weaknesses:
                - ...

                Missing Points:
                - ...

                Accuracy Review:
                - ...

                Improvements:
                - ...

                Score out of 10:
                - ...

                Final Verdict:
                - ...
                """,
                task
        );
    }

    // ============================================================
    // ROUTING DECISION
    // ============================================================

    public static class RoutingDecision {

        private String intent;

        private List<AgentRoute> agents =
                new ArrayList<>();

        private boolean needsWeb;

        private boolean needsCode;

        private String complexity;

        public String getIntent() {
            return intent;
        }

        public void setIntent(String intent) {
            this.intent = intent;
        }

        public List<AgentRoute> getAgents() {
            return agents;
        }

        public void setAgents(List<AgentRoute> agents) {
            this.agents = agents;
        }

        public boolean isNeedsWeb() {
            return needsWeb;
        }

        public void setNeedsWeb(boolean needsWeb) {
            this.needsWeb = needsWeb;
        }

        public boolean isNeedsCode() {
            return needsCode;
        }

        public void setNeedsCode(boolean needsCode) {
            this.needsCode = needsCode;
        }

        public String getComplexity() {
            return complexity;
        }

        public void setComplexity(String complexity) {
            this.complexity = complexity;
        }
    }

    // ============================================================
    // AGENT ROUTE
    // ============================================================

    public static class AgentRoute {

        private String name;

        private int order;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }
    }
}