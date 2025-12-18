package com.example.socialapp;
public class User {
    private String userId;
    private String username;
    private String email;
    private String profileImageUrl;
    private String phoneNumber;
    private String bio;

    public User() {}

    public User(String userId, String username, String email, String profileImageUrl, String phoneNumber) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.phoneNumber = phoneNumber;
    }

    public User(String userId, String username, String email, String profileImageUrl, String phoneNumber, String bio) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.phoneNumber = phoneNumber;
        this.bio = bio;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
