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
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class UserPostAdapter extends RecyclerView.Adapter<UserPostAdapter.UserPostViewHolder> {

    private Context context;
    private List<Post> posts;

    public UserPostAdapter(Context context, List<Post> posts) {
        this.context = context;
        this.posts = posts;
    }

    @NonNull
    @Override
    public UserPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_post, parent, false);
        return new UserPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserPostViewHolder holder, int position) {
        try {
            Post post = posts.get(position);

            // Set caption
            holder.tvCaption.setText(post.getCaption());

            // Load post image
            Glide.with(context)
                    .load(post.getImageUrl())
                    .into(holder.ivPostImage);

            // Set like count
            holder.tvLikeCount.setText(post.getLikes() != null ?
                    post.getLikes().size() + " likes" : "0 likes");

            // Set comment count
            holder.tvCommentCount.setText(post.getCommentCount() + " comments");

            // Open post detail on image or card click
            View.OnClickListener openDetailListener = v -> {
                try {
                    android.content.Intent intent = new android.content.Intent(context, PostDetailActivity.class);
                    intent.putExtra("postId", post.getId());
                    context.startActivity(intent);
                } catch (Exception e) {
                    // Handle intent errors
                }
            };
            holder.ivPostImage.setOnClickListener(openDetailListener);
            holder.itemView.setOnClickListener(openDetailListener);

        } catch (Exception e) {
            // Handle binding errors gracefully
            holder.tvCaption.setText("Unable to load post");
            holder.tvLikeCount.setText("0 likes");
            holder.tvCommentCount.setText("0 comments");
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class UserPostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostImage;
        TextView tvCaption, tvLikeCount, tvCommentCount;
        MaterialButton btnLike, btnComment;

        UserPostViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                ivPostImage = itemView.findViewById(R.id.ivPostImage);
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