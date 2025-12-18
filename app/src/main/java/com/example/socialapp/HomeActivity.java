package com.example.socialapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvPosts;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private FirebaseFirestore db;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyStateView;
    private BottomNavigationView bottomNavigation;
    private MaterialToolbar toolbar;
    private SharedPreferences sharedPreferences;
    private static final String PREF_THEME = "theme_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupBottomNavigation();
        loadPosts();
        setupSwipeRefresh();
    }

    private void initViews() {
        rvPosts = findViewById(R.id.rvPosts);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        emptyStateView = findViewById(R.id.emptyStateView);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        toolbar = findViewById(R.id.toolbar);
        
        // Initialize new UI elements
        View btnGetStarted = findViewById(R.id.btnGetStarted);
        View btnCreatePost = findViewById(R.id.btnCreatePost);
        View btnNotifications = findViewById(R.id.btnNotifications);
        View btnProfile = findViewById(R.id.btnProfile);

        // Set up click listeners
        btnGetStarted.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePostActivity.class);
            startActivity(intent);
        });
        
        btnCreatePost.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePostActivity.class);
            startActivity(intent);
        });
        
        btnNotifications.setOnClickListener(v -> {
            // TODO: Implement notifications
            Toast.makeText(this, "Notifications coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }

    private void setupRecyclerView() {
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(this, postList);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postAdapter);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_create) {
                Intent intent = new Intent(this, CreatePostActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_ai_chat) {
                Intent intent = new Intent(this, AIChatActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadPosts();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadPosts() {
        db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        showErrorToast("Error loading posts: " + e.getMessage());
                        return;
                    }

                    if (snapshots != null) {
                        postList.clear();
                        for (var doc : snapshots) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setId(doc.getId());
                                postList.add(post);
                            }
                        }
                        postAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    }
                });
    }

    private void updateEmptyState() {
        if (postList.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            rvPosts.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            rvPosts.setVisibility(View.VISIBLE);
        }
    }

    private void showComingSoonToast(String feature) {
        Toast.makeText(this, feature + " feature coming soon! 🚀", Toast.LENGTH_SHORT).show();
    }

    private void showErrorToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_profile) {
            // Navigate to ProfileActivity
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_notifications) {
            showComingSoonToast("Notifications");
            return true;
        } else if (id == R.id.menu_theme_light) {
            applyTheme(AppCompatDelegate.MODE_NIGHT_NO);
            return true;
        } else if (id == R.id.menu_theme_dark) {
            applyTheme(AppCompatDelegate.MODE_NIGHT_YES);
            return true;
        } else if (id == R.id.menu_theme_system) {
            applyTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            return true;
        } else if (id == R.id.menu_contact_us) {
            showContactUsDialog();
            return true;
        } else if (id == R.id.menu_privacy_policy) {
            showPrivacyPolicyDialog();
            return true;
        } else if (id == R.id.menu_logout) {
            showLogoutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void applyTheme(int themeMode) {
        // Save theme preference
        sharedPreferences.edit().putInt(PREF_THEME, themeMode).apply();
        
        // Apply theme
        AppCompatDelegate.setDefaultNightMode(themeMode);
        
        Toast.makeText(this, "Theme applied! Restart app for full effect.", Toast.LENGTH_SHORT).show();
    }

    private void showContactUsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Contact Us")
                .setMessage("Get in touch with us:\n\n" +
                        "📧 Email: pr75090@gmail.com\n" +
                        "📞 Phone: +91 8105021332\n\n" +
                        "We're here to help!")
                .setPositiveButton("Send Email", (dialog, which) -> {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:pr75090@gmail.com"));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Social App Support");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello, I need help with...");
                    
                    if (emailIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(emailIntent);
                    } else {
                        Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Call", (dialog, which) -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:+918105021332"));
                    startActivity(callIntent);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showPrivacyPolicyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setMessage("Privacy Policy for Social App\n\n" +
                        "Last updated: " + new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date()) + "\n\n" +
                        "1. Information We Collect\n" +
                        "• Profile information (username, profile picture)\n" +
                        "• Posts and comments you create\n" +
                        "• Usage data and preferences\n\n" +
                        "2. How We Use Your Information\n" +
                        "• To provide and maintain our service\n" +
                        "• To notify you about changes to our service\n" +
                        "• To provide customer support\n\n" +
                        "3. Data Security\n" +
                        "• We implement appropriate security measures\n" +
                        "• Your data is stored securely on Firebase servers\n\n" +
                        "4. Contact Information\n" +
                        "For privacy concerns, contact us at:\n" +
                        "pr75090@gmail.com\n\n" +
                        "By using this app, you agree to this privacy policy.")
                .setPositiveButton("I Understand", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset bottom navigation selection
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        // Show bottom navigation if hidden
        bottomNavigation.animate()
                .translationY(0)
                .setDuration(200)
                .start();
    }

    @Override
    public void onBackPressed() {
        // If RecyclerView is not at top, scroll to top first
        LinearLayoutManager layoutManager = (LinearLayoutManager) rvPosts.getLayoutManager();
        if (layoutManager != null && layoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
            rvPosts.smoothScrollToPosition(0);
            return;
        }

        // Otherwise, exit app with confirmation
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Exit", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("Cancel", null)
                .show();
    }
}