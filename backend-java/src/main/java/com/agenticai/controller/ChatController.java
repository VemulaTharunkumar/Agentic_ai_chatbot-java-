package com.agenticai.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agenticai.controller.dto.TaskRequest;
import com.agenticai.model.ChatHistory;
import com.agenticai.repository.ChatHistoryRepository;
import com.agenticai.service.OrchestratorService;
import com.google.gson.Gson;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatHistoryRepository chatHistoryRepository;
    private final OrchestratorService orchestratorService;

    private final Gson gson = new Gson();

    public ChatController(
            ChatHistoryRepository chatHistoryRepository,
            OrchestratorService orchestratorService) {

        this.chatHistoryRepository = chatHistoryRepository;
        this.orchestratorService = orchestratorService;
    }

    // ============================================================
    // CHAT HISTORY
    // ============================================================

    @GetMapping("/history/{username}")
    public ResponseEntity<?> getHistory(
            @PathVariable String username) {

        try {

            List<ChatHistory> history =
                    chatHistoryRepository
                            .findByUserIdOrderByIsPinnedDescTimestampDesc(
                                    username,
                                    PageRequest.of(0, 50)
                            );

            return ResponseEntity.ok(
                    Map.of("history", history)
            );

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            Map.of(
                                    "detail",
                                    e.getMessage()
                            )
                    );
        }
    }

    // ============================================================
    // DELETE CHAT
    // ============================================================

    @DeleteMapping("/history/{chatId}")
    public ResponseEntity<?> deleteChat(
            @PathVariable String chatId) {

        try {

            if (chatHistoryRepository.existsById(chatId)) {

                chatHistoryRepository.deleteById(chatId);

                return ResponseEntity.ok(
                        Map.of(
                                "status",
                                "success",
                                "message",
                                "Chat deleted"
                        )
                );

            } else {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                Map.of(
                                        "detail",
                                        "Chat not found or deletion failed"
                                )
                        );
            }

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            Map.of(
                                    "detail",
                                    e.getMessage()
                            )
                    );
        }
    }

    // ============================================================
    // RENAME CHAT
    // ============================================================

    @PutMapping("/history/{chatId}/rename")
    public ResponseEntity<?> renameChat(
            @PathVariable String chatId,
            @RequestBody Map<String, String> body) {

        try {

            ChatHistory chat =
                    chatHistoryRepository
                            .findById(chatId)
                            .orElse(null);

            if (chat != null) {

                chat.setTitle(body.get("title"));

                chatHistoryRepository.save(chat);

                return ResponseEntity.ok(
                        Map.of(
                                "status",
                                "success",
                                "message",
                                "Chat renamed"
                        )
                );

            } else {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                Map.of(
                                        "detail",
                                        "Chat not found"
                                )
                        );
            }

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            Map.of(
                                    "detail",
                                    e.getMessage()
                            )
                    );
        }
    }

    // ============================================================
    // PIN CHAT
    // ============================================================

    @PutMapping("/history/{chatId}/pin")
    public ResponseEntity<?> pinChat(
            @PathVariable String chatId,
            @RequestBody Map<String, Boolean> body) {

        try {

            boolean isPinned =
                    Boolean.TRUE.equals(body.get("isPinned"));

            ChatHistory chat =
                    chatHistoryRepository
                            .findById(chatId)
                            .orElse(null);

            if (chat == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                Map.of(
                                        "detail",
                                        "Chat not found"
                                )
                        );
            }

            if (isPinned) {

                long pinnedCount =
                        chatHistoryRepository
                                .countByUserIdAndIsPinnedTrue(
                                        chat.getUserId()
                                );

                if (pinnedCount >= 10) {

                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(
                                    Map.of(
                                            "detail",
                                            "Maximum of 10 pinned chats allowed"
                                    )
                            );
                }
            }

            chat.setPinned(isPinned);

            chatHistoryRepository.save(chat);

            return ResponseEntity.ok(
                    Map.of(
                            "status",
                            "success",
                            "message",
                            "Chat pin toggled"
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            Map.of(
                                    "detail",
                                    e.getMessage()
                            )
                    );
        }
    }

    // ============================================================
    // MAIN TASK ENDPOINT
    // ============================================================

    @PostMapping("/task")
    public ResponseEntity<?> runTask(
            @RequestBody TaskRequest req) {

        String userTask = req.getTask();

        if (userTask == null ||
                userTask.trim().isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                            Map.of(
                                    "detail",
                                    "Task cannot be empty"
                            )
                    );
        }

        try {

            // ====================================================
            // STEP 1
            // LLM ROUTER
            // ====================================================

            System.out.println();
            System.out.println("======================================");
            System.out.println("           LLM ROUTER");
            System.out.println("======================================");

            OrchestratorService.RoutingDecision decision =
                    orchestratorService.route(userTask);

            String intent =
                    decision.getIntent();

            boolean needsWeb =
                    decision.isNeedsWeb();

            boolean needsCode =
                    decision.isNeedsCode();

            String complexity =
                    decision.getComplexity();

            System.out.println(
                    "Intent     : " + intent
            );

            System.out.println(
                    "Needs Web  : " + needsWeb
            );

            System.out.println(
                    "Needs Code : " + needsCode
            );

            System.out.println(
                    "Complexity : " + complexity
            );

            System.out.println(
                    "======================================"
            );


            // ====================================================
            // STEP 2
            // GET LLM SELECTED AGENTS
            // ====================================================

            List<OrchestratorService.AgentRoute> routes =
                    decision.getAgents();

            List<String> agents =
                    new ArrayList<>();

            for (OrchestratorService.AgentRoute route : routes) {

                agents.add(route.getName());
            }

            System.out.println(
                    "Agents selected: " + agents
            );


            // ====================================================
            // STEP 3
            // EXECUTE AGENTS SEQUENTIALLY
            // ====================================================

            Map<String, String> outputs =
                    new HashMap<>();

            String currentTask = userTask;

            String finalAnswer = "";


            // ----------------------------------------------------
            // NO AGENTS = GREETING / SIMPLE RESPONSE
            // ----------------------------------------------------

            if (routes.isEmpty()) {

                finalAnswer =
                        "👋 Hi! How can I help you today?";

            } else {

                // ------------------------------------------------
                // EXECUTE IN LLM SELECTED ORDER
                // ------------------------------------------------

                for (
                        OrchestratorService.AgentRoute route
                        : routes
                ) {

                    String agentName =
                            route.getName();

                    System.out.println();
                    System.out.println(
                            ">>> Running agent: "
                                    + agentName
                    );

                    System.out.println(
                            ">>> Order: "
                                    + route.getOrder()
                    );


                    // ============================================
                    // EXECUTE CURRENT AGENT
                    // ============================================

                    String output =
                            orchestratorService.executeAgent(
                                    agentName,
                                    currentTask
                            );

                    if (output == null) {
                        output = "";
                    }

                    output = output.trim();


                    // ============================================
                    // SAVE OUTPUT
                    // ============================================

                    outputs.put(
                            agentName,
                            output
                    );


                    // ============================================
                    // PASS OUTPUT TO NEXT AGENT
                    // ============================================

                    if (!output.isEmpty()) {

                        currentTask =
                                buildNextAgentTask(
                                        userTask,
                                        agentName,
                                        output,
                                        outputs
                                );
                    }


                    // ============================================
                    // FINAL ANSWER
                    // ============================================

                    finalAnswer = output;


                    System.out.println(
                            ">>> "
                                    + agentName
                                    + " completed"
                    );
                }
            }


            // ====================================================
            // STEP 4
            // FORMAT FINAL RESPONSE
            // ====================================================

            if ("CODE".equalsIgnoreCase(intent)
                    && outputs.containsKey("coder")) {

                String code =
                        outputs.get("coder").trim();

                /*
                 * If coder did not provide markdown,
                 * wrap it so frontend can render it.
                 */

                /*
                 * The coder agent usually returns markdown formatted code blocks.
                 * We do not need to wrap it manually.
                 */

                /*
                 * If planner exists, show planner
                 * followed by code.
                 */

                String plannerOutput =
                        outputs.getOrDefault(
                                "planner",
                                ""
                        ).trim();

                if (!plannerOutput.isEmpty()) {

                    finalAnswer =
                            plannerOutput
                                    + "\n\n"
                                    + code;

                } else {

                    finalAnswer = code;
                }

            } else {

                /*
                 * For non-code tasks, use the output
                 * of the last agent.
                 */

                finalAnswer =
                        removeCodeCompletely(
                                finalAnswer
                        );
            }


            // ====================================================
            // STEP 5
            // SAVE CHAT HISTORY
            // ====================================================

            String agentsStr =
                    String.join(", ", agents);

            String outputsJson =
                    gson.toJson(outputs);

            ChatHistory chatRecord =
                    new ChatHistory(
                            req.getUsername(),
                            userTask,
                            agentsStr,
                            finalAnswer,
                            outputsJson
                    );

            chatRecord =
                    chatHistoryRepository.save(
                            chatRecord
                    );


            // ====================================================
            // STEP 6
            // MAX 10 CHATS
            // ====================================================

            long totalChats =
                    chatHistoryRepository
                            .countByUserId(
                                    req.getUsername()
                            );

            if (totalChats > 10) {

                List<ChatHistory> oldestUnpinned =
                        chatHistoryRepository
                                .findByUserIdAndIsPinnedFalseOrderByTimestampAsc(
                                        req.getUsername(),
                                        PageRequest.of(0, 1)
                                );

                if (!oldestUnpinned.isEmpty()) {

                    chatHistoryRepository.delete(
                            oldestUnpinned.get(0)
                    );

                } else {

                    List<ChatHistory> oldestOverall =
                            chatHistoryRepository
                                    .findByUserIdOrderByTimestampAsc(
                                            req.getUsername(),
                                            PageRequest.of(0, 1)
                                    );

                    if (!oldestOverall.isEmpty()) {

                        chatHistoryRepository.delete(
                                oldestOverall.get(0)
                        );
                    }
                }
            }


            // ====================================================
            // STEP 7
            // RESPONSE
            // ====================================================

            Map<String, Object> result =
                    new HashMap<>();

            result.put(
                    "intent",
                    intent
            );

            result.put(
                    "agents",
                    agents
            );

            result.put(
                    "routing",
                    decision
            );

            result.put(
                    "outputs",
                    outputs
            );

            result.put(
                    "final_answer",
                    finalAnswer
            );

            result.put(
                    "is_code",
                    needsCode
            );

            result.put(
                    "chat_id",
                    chatRecord.get_id()
            );


            System.out.println();
            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "          TASK COMPLETED"
            );

            System.out.println(
                    "======================================"
            );


            return ResponseEntity.ok(result);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            Map.of(
                                    "detail",
                                    e.getMessage() != null
                                            ? e.getMessage()
                                            : "Internal server error"
                            )
                    );
        }
    }


    // ============================================================
    // BUILD NEXT AGENT INPUT
    // ============================================================

    private String buildNextAgentTask(
            String originalTask,
            String previousAgent,
            String previousOutput,
            Map<String, String> outputs) {

        StringBuilder task =
                new StringBuilder();

        task.append(
                "ORIGINAL USER REQUEST:\n"
        );

        task.append(
                originalTask
        );

        task.append(
                "\n\n"
        );

        task.append(
                "PREVIOUS AGENT: "
        );

        task.append(
                previousAgent
        );

        task.append(
                "\n\n"
        );

        task.append(
                "PREVIOUS AGENT OUTPUT:\n"
        );

        task.append(
                previousOutput
        );

        task.append(
                "\n\n"
        );

        task.append(
                "You are the next agent in the pipeline."
        );

        task.append(
                "\nUse the previous agent's work as input."
        );

        task.append(
                "\nImprove, extend, validate, or transform it "
                        + "according to your role."
        );

        return task.toString();
    }


    // ============================================================
    // REMOVE CODE FROM NON-CODE RESPONSES
    // ============================================================

    private String removeCodeCompletely(
            String text) {

        if (text == null) {
            return "";
        }

        text =
                text.replaceAll(
                        "(?s)```.*?```",
                        ""
                );

        text =
                text.replaceAll(
                        "`.*?`",
                        ""
                );

        return text.trim();
    }
}