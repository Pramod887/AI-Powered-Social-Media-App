package com.example.socialapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class PostDetailActivity extends AppCompatActivity {

    private ImageView ivPostImage;
    private TextView tvPostContent, tvUsername, tvTimestamp, tvLikeCount, tvCommentCount;
    private Toolbar toolbar;
    private android.widget.Button btnComment;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String postId;
    private Post currentPost;
    private boolean canDelete = false;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get post ID from intent
        postId = getIntent().getStringExtra("postId");
        canDelete = getIntent().getBooleanExtra("canDelete", false);

        if (postId == null) {
            Toast.makeText(this, "Error: Post not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadPostDetails();
    }

    private void initViews() {
        ivPostImage = findViewById(R.id.ivPostImage);
        tvPostContent = findViewById(R.id.tvPostContent);
        tvUsername = findViewById(R.id.tvUsername);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvCommentCount = findViewById(R.id.tvCommentCount);
        toolbar = findViewById(R.id.toolbar);
        btnComment = findViewById(R.id.btnComment);
        btnComment.setOnClickListener(v -> openComments());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Post Details");
        }
    }

    private void loadPostDetails() {
        db.collection(Constants.COLLECTION_POSTS).document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentPost = documentSnapshot.toObject(Post.class);
                        if (currentPost != null) {
                            currentPost.setId(documentSnapshot.getId());
                            updateUI();

                            // Check if current user can delete this post
                            canDelete = currentPost.getUserId().equals(currentUserId);
                            invalidateOptionsMenu(); // Refresh menu
                        }
                    } else {
                        Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading post: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateUI() {
        if (currentPost == null) return;

        // Set post content (try different possible method names)
        String content = null;
        try {
            // Try common method names for post content
            if (currentPost.getCaption() != null && !currentPost.getCaption().isEmpty()) {
                content = currentPost.getCaption();
            }
        } catch (Exception e) {
            // If getText() doesn't exist, try other common names
            try {
                if (currentPost.getCaption() != null && !currentPost.getCaption().isEmpty()) {
                    content = currentPost.getCaption();
                }
            } catch (Exception ex) {
                // Handle case where neither method exists
                content = null;
            }
        }

        if (content != null && !content.isEmpty()) {
            tvPostContent.setText(content);
            tvPostContent.setVisibility(android.view.View.VISIBLE);
        } else {
            tvPostContent.setVisibility(android.view.View.GONE);
        }

        // Set post image
        if (currentPost.getImageUrl() != null && !currentPost.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentPost.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(ivPostImage);
            ivPostImage.setVisibility(android.view.View.VISIBLE);
        } else {
            ivPostImage.setVisibility(android.view.View.GONE);
        }

        // Set username and timestamp
        tvUsername.setText(currentPost.getUsername() != null ?
                currentPost.getUsername() : "Unknown User");

        if (currentPost.getTimestamp() != null) {
            // Simple timestamp formatting - you can customize this
            tvTimestamp.setText(android.text.format.DateUtils.getRelativeTimeSpanString(
                    currentPost.getTimestamp().toDate().getTime()));
        }

        // Set like count
        int likeCount = currentPost.getLikes() != null ? currentPost.getLikes().size() : 0;
        tvLikeCount.setText(likeCount + " likes");

        // Set comment count
        tvCommentCount.setText(currentPost.getCommentCount() + " comments");
    }

    private void openComments() {
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra("postId", postId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (canDelete) {
            getMenuInflater().inflate(R.menu.menu_post_detail, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_delete) {
            confirmDeletePost();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void confirmDeletePost() {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePost())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePost() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        // Delete from Firestore
        db.collection(Constants.COLLECTION_POSTS).document(postId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Delete image from Storage if exists
                    if (currentPost.getImageUrl() != null && !currentPost.getImageUrl().isEmpty()) {
                        StorageReference imageRef = storage.getReferenceFromUrl(currentPost.getImageUrl());
                        imageRef.delete();
                    }

                    // Delete comments for this post
                    db.collection(Constants.COLLECTION_COMMENTS)
                            .whereEqualTo("postId", postId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    doc.getReference().delete();
                                }
                            });

                    // Delete likes for this post
                    db.collection(Constants.COLLECTION_LIKES)
                            .whereEqualTo("postId", postId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    doc.getReference().delete();
                                }
                            });

                    Toast.makeText(this, "Post deleted successfully", Toast.LENGTH_SHORT).show();

                    // Return to previous screen
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete post: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}