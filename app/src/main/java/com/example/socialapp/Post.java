package com.example.socialapp;

import com.google.firebase.Timestamp;

import java.util.List;

public class Post {
    private String id;
    private String userId;
    private String username;
    private String imageUrl;
    private String profileImageUrl; // New field for profile image
    private String caption;
    private List<String> likes;
    private int commentCount;
    private Timestamp timestamp;

    public Post() {}

    public Post(String userId, String username, String imageUrl, String profileImageUrl, String caption,
                List<String> likes, int commentCount, Timestamp timestamp) {
        this.userId = userId;
        this.username = username;
        this.imageUrl = imageUrl;
        this.profileImageUrl = profileImageUrl;
        this.caption = caption;
        this.likes = likes;
        this.commentCount = commentCount;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public List<String> getLikes() { return likes; }
    public void setLikes(List<String> likes) { this.likes = likes; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
