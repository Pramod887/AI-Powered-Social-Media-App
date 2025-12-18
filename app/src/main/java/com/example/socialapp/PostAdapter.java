package com.example.socialapp;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> posts;
    private FirebaseFirestore db;
    private String currentUserId;

    public PostAdapter(Context context, List<Post> posts) {
        this.context = context;
        this.posts = posts;
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        try {
            Post post = posts.get(position);

            holder.tvUsername.setText(post.getUsername());
            holder.tvUsernameCaption.setText(post.getUsername());
            holder.tvCaption.setText(post.getCaption());
            holder.tvLikeCount.setText(post.getLikes() != null ?
                    post.getLikes().size() + " likes" : "0 likes");
            holder.tvCommentCount.setText(post.getCommentCount() + " comments");

            // Load post image
            Glide.with(context)
                    .load(post.getImageUrl())
                    .into(holder.ivPostImage);

            // Load profile image with fallback
            loadProfileImage(post.getUserId(), holder.ivUserProfile);

            // Check if current user liked this post
            boolean isLiked = post.getLikes() != null &&
                    post.getLikes().contains(currentUserId);
            updateLikeButtonState(holder.btnLike, isLiked);

            // Set click listeners
            holder.btnLike.setOnClickListener(v -> toggleLike(post, holder));
            holder.btnComment.setOnClickListener(v -> openComments(post));
            
            // Make usernames clickable to open user profile
            holder.tvUsername.setOnClickListener(v -> openUserProfile(post.getUserId()));
            holder.tvUsernameCaption.setOnClickListener(v -> openUserProfile(post.getUserId()));
            holder.ivUserProfile.setOnClickListener(v -> openUserProfile(post.getUserId()));
            
        } catch (Exception e) {
            // Handle binding errors gracefully
            holder.tvUsername.setText("Error");
            holder.tvCaption.setText("Unable to load post");
            holder.tvLikeCount.setText("0 likes");
            holder.tvCommentCount.setText("0 comments");
        }
    }

    private void loadProfileImage(String userId, ImageView profileImageView) {
        try {
            if (userId != null && !userId.isEmpty()) {
                // First try to load from the profileImageUrl if available
                db.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            try {
                                if (documentSnapshot.exists()) {
                                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                        // Load profile image with circular crop
                                        Glide.with(context)
                                                .load(profileImageUrl)
                                                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                                .placeholder(R.drawable.ic_person)
                                                .error(R.drawable.ic_person)
                                                .into(profileImageView);
                                    } else {
                                        // No profile image, show default person icon
                                        profileImageView.setImageResource(R.drawable.ic_person);
                                    }
                                } else {
                                    // User document doesn't exist, show default
                                    profileImageView.setImageResource(R.drawable.ic_person);
                                }
                            } catch (Exception e) {
                                profileImageView.setImageResource(R.drawable.ic_person);
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Error loading user data, show default
                            profileImageView.setImageResource(R.drawable.ic_person);
                        });
            } else {
                // No userId, show default
                profileImageView.setImageResource(R.drawable.ic_person);
            }
        } catch (Exception e) {
            profileImageView.setImageResource(R.drawable.ic_person);
        }
    }

    private void updateLikeButtonState(MaterialButton likeButton, boolean isLiked) {
        try {
            likeButton.setSelected(isLiked);
            if (isLiked) {
                likeButton.setIconResource(R.drawable.ic_heart_filled);
                likeButton.setIconTintResource(R.color.like_color);
            } else {
                likeButton.setIconResource(R.drawable.ic_heart_outline);
                likeButton.setIconTintResource(R.color.white);
            }
        } catch (Exception e) {
            // Fallback to basic state
            likeButton.setSelected(isLiked);
        }
    }

    private void toggleLike(Post post, PostViewHolder holder) {
        try {
            boolean isCurrentlyLiked = post.getLikes() != null && 
                    post.getLikes().contains(currentUserId);
            
            if (isCurrentlyLiked) {
                // Unlike
                db.collection("posts").document(post.getId())
                        .update("likes", FieldValue.arrayRemove(currentUserId));
                holder.btnLike.setSelected(false);
                holder.btnLike.setIconResource(R.drawable.ic_heart_outline);
                holder.btnLike.setIconTintResource(R.color.white);
            } else {
                // Like
                db.collection("posts").document(post.getId())
                        .update("likes", FieldValue.arrayUnion(currentUserId));
                holder.btnLike.setSelected(true);
                holder.btnLike.setIconResource(R.drawable.ic_heart_filled);
                holder.btnLike.setIconTintResource(R.color.like_color);
            }
        } catch (Exception e) {
            // Handle like toggle errors
        }
    }

    private void openComments(Post post) {
        try {
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("postId", post.getId());
            context.startActivity(intent);
        } catch (Exception e) {
            // Handle intent errors
        }
    }

    private void openUserProfile(String userId) {
        try {
            Intent intent = new Intent(context, UserProfileActivity.class);
            intent.putExtra("userId", userId);
            context.startActivity(intent);
        } catch (Exception e) {
            // Handle intent errors
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostImage, ivUserProfile;
        TextView tvUsername, tvCaption, tvLikeCount, tvCommentCount,tvUsernameCaption;
        MaterialButton btnLike, btnComment;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                ivPostImage = itemView.findViewById(R.id.ivPostImage);
                ivUserProfile = itemView.findViewById(R.id.ivUserProfile);
                tvUsername = itemView.findViewById(R.id.tvUsername);
                tvUsernameCaption = itemView.findViewById(R.id.tvUsernameCaption);
                tvCaption = itemView.findViewById(R.id.tvCaption);
                tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
                tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
                btnLike = itemView.findViewById(R.id.btnLike);
                btnComment = itemView.findViewById(R.id.btnComment);
            } catch (Exception e) {
                // Handle binding errors
            }
        }
    }
}