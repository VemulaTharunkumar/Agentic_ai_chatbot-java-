package com.agenticai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_history")
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private String _id; // mapped to match mongodb "_id" convention in JSON

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "VARCHAR(255)")
    private String title;

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false;

    @Column(columnDefinition = "VARCHAR(255)")
    private String agent;

    @Column(columnDefinition = "LONGTEXT")
    private String response;

    private LocalDateTime timestamp;

    public ChatHistory() {
    }

    public ChatHistory(String userId, String prompt, String agent, String response) {
        this.userId = userId;
        this.prompt = prompt;
        this.title = prompt; // Default title is the prompt
        this.agent = agent;
        this.response = response;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

