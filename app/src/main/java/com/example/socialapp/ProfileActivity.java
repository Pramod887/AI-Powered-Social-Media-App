package com.example.socialapp;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final int REQUEST_GALLERY = 101;

    private ImageView ivProfilePicture;
    private EditText etUsername;
    private Button btnSaveProfile, btnChangePhoto;
    private RecyclerView rvUserPosts;
    private TextView tvPostCount, tvLikeCount, tvCommentCount;
    private LinearLayout emptyPostsView;
    private Toolbar toolbar;

    private UserPostAdapter userPostAdapter;
    private List<Post> userPosts;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String currentUserId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadUserProfile();
        loadUserPosts();

        btnChangePhoto.setOnClickListener(v -> changeProfilePhoto());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void initViews() {
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        etUsername = findViewById(R.id.etUsername);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        rvUserPosts = findViewById(R.id.rvUserPosts);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvCommentCount = findViewById(R.id.tvCommentCount);
        emptyPostsView = findViewById(R.id.emptyPostsView);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("👤 Profile");
        }
    }

    private void setupRecyclerView() {
        userPosts = new ArrayList<>();
        userPostAdapter = new UserPostAdapter(this, userPosts);
        rvUserPosts.setLayoutManager(new GridLayoutManager(this, 3));
        rvUserPosts.setAdapter(userPostAdapter);
    }

    private void openPostDetail(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("postId", post.getId());
        intent.putExtra("canDelete", true);
        startActivity(intent);
    }

    private void showPostOptions(Post post) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Post Options")
                .setItems(new String[]{"View Post", "Delete Post"}, (dialog, which) -> {
                    if (which == 0) {
                        openPostDetail(post);
                    } else if (which == 1) {
                        confirmDeletePost(post);
                    }
                })
                .show();
    }

    private void confirmDeletePost(Post post) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePost(post))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePost(Post post) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        // Delete from Firestore
        db.collection(Constants.COLLECTION_POSTS).document(post.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Delete image from Storage if exists
                    if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                        StorageReference imageRef = storage.getReferenceFromUrl(post.getImageUrl());
                        imageRef.delete();
                    }

                    Toast.makeText(this, "Post deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserProfile() {
        FirebaseHelper.getCurrentUser(new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                updateUI();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ProfileActivity.this,
                        "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (currentUser != null) {
            if (currentUser.getUsername() != null) {
                etUsername.setText(currentUser.getUsername());
            }

            if (currentUser.getProfileImageUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getProfileImageUrl())
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.ic_person)
                        .into(ivProfilePicture);
            }
        }
    }

    private void loadUserPosts() {
        db.collection(Constants.COLLECTION_POSTS)
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error loading user posts", e);
                        return;
                    }

                    userPosts.clear();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setId(doc.getId());
                                userPosts.add(post);
                            }
                        }
                    }

                    Log.d(TAG, "Loaded " + userPosts.size() + " posts");
                    userPostAdapter.notifyDataSetChanged();
                    updatePostStats();
                    updateEmptyState();
                });
    }

    private void updatePostStats() {
        tvPostCount.setText(String.valueOf(userPosts.size()));

        // Calculate total likes and comments from embedded lists
        calculateStatsFromEmbeddedLists();
    }

    private void calculateStatsFromEmbeddedLists() {
        int totalLikes = 0;
        int totalComments = 0;

        Log.d(TAG, "=== CALCULATING STATS FROM EMBEDDED LISTS ===");

        for (int i = 0; i < userPosts.size(); i++) {
            Post post = userPosts.get(i);

            // Count likes from the likes list
            int postLikes = 0;
            if (post.getLikes() != null) {
                postLikes = post.getLikes().size();
                totalLikes += postLikes;
            }

            // Count comments from the comment count
            int postComments = post.getCommentCount();
            totalComments += postComments;

            Log.d(TAG, "Post " + (i+1) + " - ID: " + post.getId() +
                    ", Likes: " + postLikes + ", Comments: " + postComments);
        }

        Log.d(TAG, "TOTAL - Likes: " + totalLikes + ", Comments: " + totalComments);

        // Update UI
        tvLikeCount.setText(String.valueOf(totalLikes));
        tvCommentCount.setText(String.valueOf(totalComments));
    }

    private void updateEmptyState() {
        if (userPosts.isEmpty()) {
            emptyPostsView.setVisibility(android.view.View.VISIBLE);
            rvUserPosts.setVisibility(android.view.View.GONE);
        } else {
            emptyPostsView.setVisibility(android.view.View.GONE);
            rvUserPosts.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void changeProfilePhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    Bitmap bitmap = ImageUtils.getBitmapFromUri(this, imageUri);

                    // Handle rotation
                    int orientation = ImageUtils.getImageOrientation(this, imageUri);
                    if (orientation != 0) {
                        bitmap = ImageUtils.rotateBitmap(bitmap, orientation);
                    }

                    // Resize for profile picture
                    bitmap = ImageUtils.resizeBitmap(bitmap, 512, 512);

                    ivProfilePicture.setImageBitmap(bitmap);
                    uploadProfileImage(bitmap);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error loading image: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadProfileImage(Bitmap bitmap) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        btnChangePhoto.setEnabled(false);

        byte[] data = ImageUtils.bitmapToByteArray(bitmap, Constants.IMAGE_COMPRESSION_QUALITY);

        StorageReference profileRef = storage.getReference()
                .child(Constants.STORAGE_PROFILE_IMAGES + "/" + currentUserId + ".jpg");

        profileRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveProfileImageUrl(uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    btnChangePhoto.setEnabled(true);
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfileImageUrl(String imageUrl) {
        db.collection(Constants.COLLECTION_USERS).document(currentUserId)
                .update("profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    btnChangePhoto.setEnabled(true);
                    Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();

                    if (currentUser != null) {
                        currentUser.setProfileImageUrl(imageUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    btnChangePhoto.setEnabled(true);
                    Toast.makeText(this, "Failed to save profile picture",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfile() {
        String username = etUsername.getText().toString().trim();
        if (username.isEmpty()) {
            etUsername.setError("Username cannot be empty");
            return;
        }

        if (username.length() < 3) {
            etUsername.setError("Username must be at least 3 characters");
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveProfile.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("userId", currentUserId);

        db.collection(Constants.COLLECTION_USERS).document(currentUserId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();

                    if (currentUser != null) {
                        currentUser.setUsername(username);
                    }
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
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