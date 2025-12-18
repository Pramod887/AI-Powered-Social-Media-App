package com.example.socialapp;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

/**
 * Utility class for handling user profile navigation
 * Provides easy integration for username clicks throughout the app
 */
public class UserProfileUtils {

    /**
     * Set up a TextView to be clickable and navigate to user profile
     * @param context The current context
     * @param usernameTextView The TextView containing the username
     * @param userId The user ID to navigate to
     */
    public static void setupUsernameClick(Context context, TextView usernameTextView, String userId) {
        if (usernameTextView == null || userId == null) return;
        
        usernameTextView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserProfileActivity.class);
            intent.putExtra("userId", userId);
            context.startActivity(intent);
        });
    }

    /**
     * Set up a TextView to be clickable and navigate to user profile
     * Also sets the text of the TextView to the username
     * @param context The current context
     * @param usernameTextView The TextView to set up
     * @param userId The user ID to navigate to
     * @param username The username to display
     */
    public static void setupUsernameClick(Context context, TextView usernameTextView, String userId, String username) {
        if (usernameTextView == null || userId == null) return;
        
        // Set the username text
        if (username != null && !username.isEmpty()) {
            usernameTextView.setText(username);
        }
        
        // Make it clickable
        setupUsernameClick(context, usernameTextView, userId);
    }

    /**
     * Navigate to a user's profile programmatically
     * @param context The current context
     * @param userId The user ID to navigate to
     */
    public static void navigateToUserProfile(Context context, String userId) {
        if (context == null || userId == null) return;
        
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.putExtra("userId", userId);
        context.startActivity(intent);
    }

    /**
     * Set up a View to be clickable and navigate to user profile
     * Useful for when the username is inside a custom layout
     * @param context The current context
     * @param view The View to make clickable
     * @param userId The user ID to navigate to
     */
    public static void setupViewClick(Context context, View view, String userId) {
        if (view == null || userId == null) return;
        
        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserProfileActivity.class);
            intent.putExtra("userId", userId);
            context.startActivity(intent);
        });
    }
}
