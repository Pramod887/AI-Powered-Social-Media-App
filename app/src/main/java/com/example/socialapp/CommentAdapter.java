package com.example.socialapp;

import android.content.Context;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private Context context;
    private List<Comment> commentList;
    private SimpleDateFormat dateFormat;
    private FirebaseFirestore db;

    public CommentAdapter(Context context, List<Comment> commentList) {
        this.context = context;
        this.commentList = commentList;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        try {
            Comment comment = commentList.get(position);

            // Set username with fallback
            String username = comment.getUsername();
            if (username == null || username.isEmpty()) {
                username = "Anonymous";
            }
            holder.tvUsername.setText(username);
            
            // Set comment text
            holder.tvCommentText.setText(comment.getText());

            // Load profile image
            loadProfileImage(comment.getUserId(), holder.ivUserProfile);

            // Format timestamp
            if (comment.getTimestamp() != null) {
                String formattedDate = dateFormat.format(comment.getTimestamp().toDate());
                holder.tvTimestamp.setText(formattedDate);
            } else {
                holder.tvTimestamp.setText("Just now");
            }

            // Set up like button click listener with simple functionality
            if (holder.btnLikeComment != null) {
                holder.btnLikeComment.setOnClickListener(v -> handleCommentLike(holder));
            }
        } catch (Exception e) {
            // Handle any binding errors gracefully
            if (holder.tvUsername != null) holder.tvUsername.setText("Error");
            if (holder.tvCommentText != null) holder.tvCommentText.setText("Unable to load comment");
            if (holder.tvTimestamp != null) holder.tvTimestamp.setText("--");
        }
    }

    private void loadProfileImage(String userId, ImageView profileImageView) {
        try {
            if (userId != null && !userId.isEmpty()) {
                // Fetch user's profile image from Firestore
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

    private void handleCommentLike(CommentViewHolder holder) {
        try {
            // Toggle like state
            boolean isLiked = holder.btnLikeComment.isSelected();
            holder.btnLikeComment.setSelected(!isLiked);
            
            if (!isLiked) {
                // Like - change icon to filled heart
                if (holder.ivLikeIcon != null) {
                    holder.ivLikeIcon.setImageResource(R.drawable.ic_heart_filled);
                    holder.ivLikeIcon.setColorFilter(context.getResources().getColor(R.color.like_color));
                }
                
                // Update like count
                if (holder.tvLikeCount != null) {
                    try {
                        int currentLikes = Integer.parseInt(holder.tvLikeCount.getText().toString());
                        holder.tvLikeCount.setText(String.valueOf(currentLikes + 1));
                    } catch (Exception e) {
                        holder.tvLikeCount.setText("1");
                    }
                }
            } else {
                // Unlike - change icon back to outline heart
                if (holder.ivLikeIcon != null) {
                    holder.ivLikeIcon.setImageResource(R.drawable.ic_heart_outline);
                    holder.ivLikeIcon.setColorFilter(context.getResources().getColor(R.color.white));
                }
                
                // Update like count
                if (holder.tvLikeCount != null) {
                    try {
                        int currentLikes = Integer.parseInt(holder.tvLikeCount.getText().toString());
                        if (currentLikes > 0) {
                            holder.tvLikeCount.setText(String.valueOf(currentLikes - 1));
                        }
                    } catch (Exception e) {
                        holder.tvLikeCount.setText("0");
                    }
                }
            }
        } catch (Exception e) {
            // Handle any errors gracefully
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserProfile, ivLikeIcon;
        TextView tvUsername, tvCommentText, tvTimestamp, tvLikeCount;
        View btnLikeComment, btnReply;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                ivUserProfile = itemView.findViewById(R.id.ivUserProfile);
                ivLikeIcon = itemView.findViewById(R.id.ivLikeIcon);
                tvUsername = itemView.findViewById(R.id.tvUsername);
                tvCommentText = itemView.findViewById(R.id.tvComment);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
                tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
                btnLikeComment = itemView.findViewById(R.id.btnLikeComment);
                btnReply = itemView.findViewById(R.id.btnReply);
            } catch (Exception e) {
                // Handle binding errors gracefully
            }
        }
    }
}