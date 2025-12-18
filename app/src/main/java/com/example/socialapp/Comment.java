package com.example.socialapp;

import com.google.firebase.Timestamp;

public class Comment {
    private String userId;
    private String username;
    private String text;
    private Timestamp timestamp;

    // Default constructor required for Firestore
    public Comment() {
    }

    public Comment(String userId, String username, String text, Timestamp timestamp) {
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}