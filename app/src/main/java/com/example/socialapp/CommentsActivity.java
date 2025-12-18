package com.example.socialapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {

    private RecyclerView rvComments;
    private EditText etComment;
    private Button btnSendComment;

    private CommentAdapter commentAdapter;
    private List<Comment> commentList;
    private String postId;
    private String currentUsername;
    private FirebaseFirestore db;
    private ListenerRegistration commentsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        postId = getIntent().getStringExtra("postId");
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        loadCurrentUser();
        loadComments();

        btnSendComment.setOnClickListener(v -> addComment());
    }

    private void initViews() {
        rvComments = findViewById(R.id.rvComments);
        etComment = findViewById(R.id.etComment);
        btnSendComment = findViewById(R.id.btnSendComment);
    }

    private void setupRecyclerView() {
        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
    }

    private void loadCurrentUser() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Fetch current user's username from Firestore
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUsername = documentSnapshot.getString("username");
                        if (currentUsername == null || currentUsername.isEmpty()) {
                            currentUsername = "User"; // Fallback username
                        }
                    } else {
                        currentUsername = "User"; // Fallback username
                    }
                })
                .addOnFailureListener(e -> {
                    currentUsername = "User"; // Fallback username
                });
    }

    private void loadComments() {
        commentsListener = db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error loading comments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            Comment comment = dc.getDocument().toObject(Comment.class);
                            
                            if (comment != null) {
                                // Ensure username is set
                                if (comment.getUsername() == null || comment.getUsername().isEmpty()) {
                                    comment.setUsername("Anonymous");
                                }

                                switch (dc.getType()) {
                                    case ADDED:
                                        commentList.add(comment);
                                        commentAdapter.notifyItemInserted(commentList.size() - 1);
                                        rvComments.scrollToPosition(commentList.size() - 1);
                                        break;
                                    case MODIFIED:
                                        // Handle comment updates if needed
                                        break;
                                    case REMOVED:
                                        // Handle comment removal if needed
                                        break;
                                }
                            }
                        }
                    }
                });
    }

    private void addComment() {
        String commentText = etComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUsername == null || currentUsername.isEmpty()) {
            currentUsername = "User"; // Fallback
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> comment = new HashMap<>();
        comment.put("userId", currentUserId);
        comment.put("username", currentUsername); // Use actual username
        comment.put("text", commentText);
        comment.put("timestamp", Timestamp.now());

        // Add comment to subcollection
        db.collection("posts").document(postId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Comment added successfully!", Toast.LENGTH_SHORT).show();
                    etComment.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Update comment count
        db.collection("posts").document(postId)
                .update("commentCount", FieldValue.increment(1))
                .addOnFailureListener(e -> {
                    // Handle comment count update failure
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentsListener != null) {
            commentsListener.remove();
        }
    }
}
