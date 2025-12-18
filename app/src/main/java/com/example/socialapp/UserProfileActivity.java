package com.example.socialapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    private String targetUserId;
    private String currentUserId;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Components
    private ImageView ivProfileImage;
    private TextView tvUsername, tvPostCount, tvLikesReceived, tvCommentsMade;
    private MaterialButton btnFollow, btnMessage;
    private RecyclerView rvUserPosts;
    private UserPostAdapter userPostAdapter;
    private List<Post> userPosts;
    private View emptyPostsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Get the target user ID from intent
        targetUserId = getIntent().getStringExtra("userId");
        if (targetUserId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        // Initialize UI
        initViews();
        setupToolbar();
        loadUserProfile();
        loadUserStats();
        loadUserPosts();
        setupButtons();
    }

    private void initViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvUsername = findViewById(R.id.tvUsername);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvLikesReceived = findViewById(R.id.tvLikesReceived);
        tvCommentsMade = findViewById(R.id.tvCommentsMade);
        btnFollow = findViewById(R.id.btnFollow);
        btnMessage = findViewById(R.id.btnMessage);
        rvUserPosts = findViewById(R.id.rvUserPosts);
        emptyPostsView = findViewById(R.id.emptyPostsView);

        // Setup RecyclerView
        userPosts = new ArrayList<>();
        userPostAdapter = new UserPostAdapter(this, userPosts);
        rvUserPosts.setLayoutManager(new LinearLayoutManager(this));
        rvUserPosts.setAdapter(userPostAdapter);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("👤 User Profile");
        }
    }

    private void loadUserProfile() {
        db.collection("users").document(targetUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        // Set username
                        if (username != null && !username.isEmpty()) {
                            tvUsername.setText(username);
                        } else {
                            tvUsername.setText("Unknown User");
                        }

                        // Load profile image
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(ivProfileImage);
                        } else {
                            ivProfileImage.setImageResource(R.drawable.ic_person);
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading user profile: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadUserStats() {
        // Load post count
        db.collection(Constants.COLLECTION_POSTS)
                .whereEqualTo("userId", targetUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int postCount = querySnapshot.size();
                    tvPostCount.setText(String.valueOf(postCount));
                })
                .addOnFailureListener(e -> {
                    tvPostCount.setText("0");
                });

        // Load total likes received (sum of all likes on user's posts)
        db.collection(Constants.COLLECTION_POSTS)
                .whereEqualTo("userId", targetUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalLikes = 0;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        List<String> likes = (List<String>) doc.get("likes");
                        if (likes != null) {
                            totalLikes += likes.size();
                        }
                    }
                    tvLikesReceived.setText(String.valueOf(totalLikes));
                })
                .addOnFailureListener(e -> {
                    tvLikesReceived.setText("0");
                });

        // Load comments made count
        db.collection(Constants.COLLECTION_COMMENTS)
                .whereEqualTo("userId", targetUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int commentCount = querySnapshot.size();
                    tvCommentsMade.setText(String.valueOf(commentCount));
                })
                .addOnFailureListener(e -> {
                    tvCommentsMade.setText("0");
                });
    }

    private void loadUserPosts() {
        db.collection(Constants.COLLECTION_POSTS)
                .whereEqualTo("userId", targetUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10) // Limit to recent 10 posts
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    userPosts.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setId(doc.getId());
                            userPosts.add(post);
                        }
                    }
                    userPostAdapter.notifyDataSetChanged();
                    
                    // Show/hide empty state
                    if (userPosts.isEmpty()) {
                        emptyPostsView.setVisibility(View.VISIBLE);
                        rvUserPosts.setVisibility(View.GONE);
                    } else {
                        emptyPostsView.setVisibility(View.GONE);
                        rvUserPosts.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading posts: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    emptyPostsView.setVisibility(View.VISIBLE);
                    rvUserPosts.setVisibility(View.GONE);
                });
    }

    private void setupButtons() {
        // Hide follow button if viewing own profile
        if (targetUserId.equals(currentUserId)) {
            btnFollow.setVisibility(View.GONE);
            btnMessage.setVisibility(View.GONE);
        } else {
            // Check if already following
            checkFollowStatus();
            
            btnFollow.setOnClickListener(v -> toggleFollow());
            btnMessage.setOnClickListener(v -> openChat());
        }
    }

    private void checkFollowStatus() {
        if (currentUserId == null) return;
        
        db.collection("followers")
                .document(targetUserId)
                .collection("userFollowers")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        btnFollow.setText("Unfollow");
                        btnFollow.setBackgroundTintList(getColorStateList(R.color.error_red));
                    } else {
                        btnFollow.setText("Follow");
                        btnFollow.setBackgroundTintList(getColorStateList(R.color.primary_color));
                    }
                })
                .addOnFailureListener(e -> {
                    btnFollow.setText("Follow");
                    btnFollow.setBackgroundTintList(getColorStateList(R.color.primary_color));
                });
    }

    private void toggleFollow() {
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to follow users", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isFollowing = btnFollow.getText().equals("Unfollow");
        
        if (isFollowing) {
            // Unfollow
            db.collection("followers")
                    .document(targetUserId)
                    .collection("userFollowers")
                    .document(currentUserId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        btnFollow.setText("Follow");
                        btnFollow.setBackgroundTintList(getColorStateList(R.color.primary_color));
                        Toast.makeText(this, "Unfollowed", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error unfollowing: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Follow
            db.collection("followers")
                    .document(targetUserId)
                    .collection("userFollowers")
                    .document(currentUserId)
                    .set(new java.util.HashMap<>())
                    .addOnSuccessListener(aVoid -> {
                        btnFollow.setText("Unfollow");
                        btnFollow.setBackgroundTintList(getColorStateList(R.color.error_red));
                        Toast.makeText(this, "Followed", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error following: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void openChat() {
        // Navigate to chat activity (you can implement this later)
        Toast.makeText(this, "Chat feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
