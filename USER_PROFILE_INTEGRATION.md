# User Profile Integration Guide

This guide shows how to integrate the user profile feature that allows users to click on usernames anywhere in the app to navigate to that user's profile page.

## Features

The user profile feature includes:
- **Profile Picture**: Displays user's profile image from Firebase Storage
- **Username**: Shows the user's username from Firestore
- **Statistics**: Displays number of posts, likes received, and comments made
- **Follow/Unfollow**: Allows users to follow/unfollow other users
- **Recent Posts**: Shows the user's recent posts
- **Message Button**: Placeholder for future chat functionality

## Files Created

1. **`activity_user_profile.xml`** - Clean UI layout for user profiles
2. **`UserProfileActivity.java`** - Activity to display user profiles and handle interactions
3. **`UserProfileUtils.java`** - Utility class for easy integration

## Integration Examples

### 1. Basic Username Click Setup

```java
// In any Activity or Adapter
TextView tvUsername = findViewById(R.id.tvUsername);
UserProfileUtils.setupUsernameClick(this, tvUsername, userId);
```

### 2. Setup Username with Text

```java
// Set username text and make it clickable
TextView tvUsername = findViewById(R.id.tvUsername);
UserProfileUtils.setupUsernameClick(this, tvUsername, userId, username);
```

### 3. Programmatic Navigation

```java
// Navigate to user profile programmatically
UserProfileUtils.navigateToUserProfile(this, userId);
```

### 4. Custom View Click

```java
// Make any view clickable to navigate to user profile
View userContainer = findViewById(R.id.userContainer);
UserProfileUtils.setupViewClick(this, userContainer, userId);
```

## Integration in Existing Components

### PostAdapter (Already Implemented)

The `PostAdapter` already includes username click functionality:

```java
// In onBindViewHolder method
holder.tvUsername.setOnClickListener(v -> openUserProfile(post.getUserId()));
holder.tvUsernameCaption.setOnClickListener(v -> openUserProfile(post.getUserId()));
holder.ivUserProfile.setOnClickListener(v -> openUserProfile(post.getUserId()));

private void openUserProfile(String userId) {
    try {
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.putExtra("userId", userId);
        context.startActivity(intent);
    } catch (Exception e) {
        // Handle intent errors
    }
}
```

### CommentAdapter Integration

Add to your `CommentAdapter`:

```java
// In onBindViewHolder method
holder.tvUsername.setOnClickListener(v -> {
    Intent intent = new Intent(context, UserProfileActivity.class);
    intent.putExtra("userId", comment.getUserId());
    context.startActivity(intent);
});
```

### HomeActivity Integration

```java
// In HomeActivity or any other activity
private void setupUsernameClick(TextView tvUsername, String userId) {
    UserProfileUtils.setupUsernameClick(this, tvUsername, userId);
}
```

## Database Structure

The feature expects the following Firestore collections:

### Users Collection
```
users/{userId}
├── username: String
├── profileImageUrl: String
└── ... (other user fields)
```

### Posts Collection
```
posts/{postId}
├── userId: String
├── username: String
├── caption: String
├── imageUrl: String
├── likes: Array<String>
├── commentCount: Number
└── timestamp: Timestamp
```

### Comments Collection
```
comments/{commentId}
├── userId: String
├── username: String
├── text: String
├── postId: String
└── timestamp: Timestamp
```

### Followers Collection
```
followers/{targetUserId}/userFollowers/{followerId}
└── (empty document to indicate following relationship)
```

## Usage Examples

### Example 1: In a RecyclerView Adapter

```java
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    User user = users.get(position);
    
    // Set username text
    holder.tvUsername.setText(user.getUsername());
    
    // Make username clickable
    UserProfileUtils.setupUsernameClick(context, holder.tvUsername, user.getId());
    
    // Or use the combined method
    UserProfileUtils.setupUsernameClick(context, holder.tvUsername, user.getId(), user.getUsername());
}
```

### Example 2: In an Activity

```java
public class SomeActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_some);
        
        TextView tvUsername = findViewById(R.id.tvUsername);
        String userId = "user123";
        String username = "john_doe";
        
        UserProfileUtils.setupUsernameClick(this, tvUsername, userId, username);
    }
}
```

### Example 3: Custom Layout Click

```java
// Make entire user card clickable
View userCard = findViewById(R.id.userCard);
UserProfileUtils.setupViewClick(this, userCard, userId);
```

## Styling

The user profile UI uses the following color scheme:
- **Primary Color**: `@color/primary_color` (#6366F1)
- **Error Red**: `@color/error_red` (#FFF44336)
- **Blue**: `@color/blue_500` (#FF2196F3)

## Error Handling

The implementation includes comprehensive error handling:
- Graceful fallbacks for missing user data
- Default profile images when profile pictures are unavailable
- Toast messages for network errors
- Safe navigation with null checks

## Future Enhancements

Potential improvements:
1. **Chat Integration**: Implement the message button functionality
2. **Followers/Following Count**: Add follower statistics
3. **Profile Editing**: Allow users to edit their own profile
4. **Block/Report**: Add user moderation features
5. **Push Notifications**: Notify users when someone follows them

## Testing

To test the integration:
1. Create test users in Firebase
2. Add posts and comments
3. Click on usernames throughout the app
4. Verify navigation to user profiles
5. Test follow/unfollow functionality
6. Check statistics accuracy

## Troubleshooting

Common issues:
- **Profile not loading**: Check if userId is correct and user exists in Firestore
- **Images not showing**: Verify profileImageUrl is valid and accessible
- **Statistics not updating**: Ensure database queries are working correctly
- **Navigation errors**: Check if UserProfileActivity is declared in AndroidManifest.xml
