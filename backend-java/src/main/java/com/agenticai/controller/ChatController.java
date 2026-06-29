package com.agenticai.controller;

import com.agenticai.controller.dto.TaskRequest;
import com.agenticai.model.ChatHistory;
import com.agenticai.repository.ChatHistoryRepository;
import com.agenticai.service.OrchestratorService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatHistoryRepository chatHistoryRepository;
    private final OrchestratorService orchestratorService;

    public ChatController(ChatHistoryRepository chatHistoryRepository, OrchestratorService orchestratorService) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.orchestratorService = orchestratorService;
    }

    @GetMapping("/history/{username}")
    public ResponseEntity<?> getHistory(@PathVariable String username) {
        try {
            List<ChatHistory> history = chatHistoryRepository.findByUserIdOrderByIsPinnedDescTimestampDesc(username, PageRequest.of(0, 50));
            return ResponseEntity.ok(Map.of("history", history));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("detail", e.getMessage()));
        }
    }

    @DeleteMapping("/history/{chatId}")
    public ResponseEntity<?> deleteChat(@PathVariable String chatId) {
        try {
            if (chatHistoryRepository.existsById(chatId)) {
                chatHistoryRepository.deleteById(chatId);
                return ResponseEntity.ok(Map.of("status", "success", "message", "Chat deleted"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "Chat not found or deletion failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("detail", e.getMessage()));
        }
    }

    @PutMapping("/history/{chatId}/rename")
    public ResponseEntity<?> renameChat(@PathVariable String chatId, @RequestBody Map<String, String> body) {
        try {
            ChatHistory chat = chatHistoryRepository.findById(chatId).orElse(null);
            if (chat != null) {
                chat.setTitle(body.get("title"));
                chatHistoryRepository.save(chat);
                return ResponseEntity.ok(Map.of("status", "success", "message", "Chat renamed"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "Chat not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("detail", e.getMessage()));
        }
    }

    @PutMapping("/history/{chatId}/pin")
    public ResponseEntity<?> pinChat(@PathVariable String chatId, @RequestBody Map<String, Boolean> body) {
        try {
            boolean isPinned = body.get("isPinned");
            ChatHistory chat = chatHistoryRepository.findById(chatId).orElse(null);
            if (chat != null) {
                if (isPinned) {
                    long pinnedCount = chatHistoryRepository.countByUserIdAndIsPinnedTrue(chat.getUserId());
                    if (pinnedCount >= 10) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("detail", "Maximum of 10 pinned chats allowed"));
                    }
                }
                chat.setPinned(isPinned);
                chatHistoryRepository.save(chat);
                return ResponseEntity.ok(Map.of("status", "success", "message", "Chat pin toggled"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "Chat not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("detail", e.getMessage()));
        }
    }

    @PostMapping("/task")
    public ResponseEntity<?> runTask(@RequestBody TaskRequest req) {
        String userTask = req.getTask();
        if (userTask == null || userTask.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("detail", "Task cannot be empty"));
        }

        try {
            String intent = orchestratorService.detectIntent(userTask);
            List<String> agents = orchestratorService.selectAgents(intent);
            Map<String, String> outputs = new HashMap<>();

            String finalAnswer = "";
            boolean isCode = false;

            if ("GREETING".equals(intent)) {
                String greet = orchestratorService.runResearch(userTask);
                finalAnswer = greet != null && !greet.isEmpty() ? greet : "👋 Hi! How can I help you today?";
            } else {
                if (agents.contains("planner")) {
                    outputs.put("planner", orchestratorService.runPlanner(userTask));
                }
                if (agents.contains("research")) {
                    outputs.put("research", orchestratorService.runResearch(userTask));
                }
                if (agents.contains("coder")) {
                    outputs.put("coder", orchestratorService.runCoder(userTask));
                }
                if (agents.contains("critic")) {
                    String base = outputs.getOrDefault("coder", outputs.getOrDefault("planner", outputs.getOrDefault("research", "")));
                    String criticText = orchestratorService.runCritic(base);
                    if (criticText == null || criticText.isEmpty()) {
                        outputs.put("critic", "No critic feedback available.");
                    } else {
                        outputs.put("critic", criticText); // Note: Could trim lines like Python version if needed
                    }
                }

                if ("CODE".equals(intent) && outputs.containsKey("coder")) {
                    String rawCode = outputs.get("coder").trim();
                    rawCode = rawCode.replaceAll("^```[a-zA-Z]*\\s*", "");
                    rawCode = rawCode.replaceAll("\\s*```$", "");
                    finalAnswer = rawCode.trim();
                    isCode = true;
                } else {
                    finalAnswer = outputs.getOrDefault("planner", outputs.getOrDefault("research", ""));
                    finalAnswer = removeCodeCompletely(finalAnswer);
                }
            }

            String agentsStr = String.join(", ", agents);
            ChatHistory chatRecord = new ChatHistory(req.getUsername(), userTask, agentsStr, finalAnswer);
            chatRecord = chatHistoryRepository.save(chatRecord);

            // Enforce maximum of 10 chats per user
            long totalChats = chatHistoryRepository.countByUserId(req.getUsername());
            if (totalChats > 10) {
                List<ChatHistory> oldestUnpinned = chatHistoryRepository.findByUserIdAndIsPinnedFalseOrderByTimestampAsc(req.getUsername(), PageRequest.of(0, 1));
                if (!oldestUnpinned.isEmpty()) {
                    chatHistoryRepository.delete(oldestUnpinned.get(0));
                } else {
                    List<ChatHistory> oldestOverall = chatHistoryRepository.findByUserIdOrderByTimestampAsc(req.getUsername(), PageRequest.of(0, 1));
                    if (!oldestOverall.isEmpty()) {
                        chatHistoryRepository.delete(oldestOverall.get(0));
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("intent", intent);
            result.put("agents", agents);
            result.put("outputs", outputs);
            result.put("final_answer", finalAnswer);
            result.put("is_code", isCode);
            result.put("chat_id", chatRecord.get_id());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("detail", e.getMessage()));
        }
    }

    private String removeCodeCompletely(String text) {
        if (text == null) return "";
        text = text.replaceAll("```.*?```", "");
        text = text.replaceAll("`.*?`", "");
        String[] patterns = {
                "(?m)^.*def .*", "(?m)^.*class .*", "(?m)^.*import .*",
                "(?m)^.*public .*", "(?m)^.*static .*", "(?m)^.*if __name__.*",
                "(?m)^.*print\\(.*", "(?m)^.*;.*", "\\{.*?\\}"
        };
        for (String pattern : patterns) {
            text = text.replaceAll(pattern, "");
        }
        return text.trim();
    }
}
