package com.example.socialapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "LoginActivity";

    // UI Components
    private EditText etEmail, etPassword, etFirstName, etUsername;
    private Button btnLogin, btnRegister, btnGoogleSignIn;
    private TextView tvSwitchToRegister, tvWelcomeTitle, tvWelcomeSubtitle;
    private LinearLayout layoutAdditionalFields;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    // State
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etFirstName = findViewById(R.id.etFirstName);
        etUsername = findViewById(R.id.etUsername);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvSwitchToRegister = findViewById(R.id.tvSwitchToRegister);
        tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle);
        tvWelcomeSubtitle = findViewById(R.id.tvWelcomeSubtitle);
        layoutAdditionalFields = findViewById(R.id.layoutAdditionalFields);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (validateInputs()) {
                loginWithEmailPassword();
            }
        });

        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) {
                checkUsernameAndRegister();
            }
        });

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        tvSwitchToRegister.setOnClickListener(v -> switchAuthMode());
    }

    private boolean validateInputs() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        // Additional validation for registration mode
        if (!isLoginMode) {
            String firstName = etFirstName.getText().toString().trim();
            String username = etUsername.getText().toString().trim();

            if (firstName.isEmpty()) {
                etFirstName.setError("First name is required");
                etFirstName.requestFocus();
                return false;
            }

            if (firstName.length() < 2) {
                etFirstName.setError("First name must be at least 2 characters");
                etFirstName.requestFocus();
                return false;
            }

            if (username.isEmpty()) {
                etUsername.setError("Username is required");
                etUsername.requestFocus();
                return false;
            }

            if (username.length() < 3) {
                etUsername.setError("Username must be at least 3 characters");
                etUsername.requestFocus();
                return false;
            }

            if (!username.matches("^[a-zA-Z0-9_]+$")) {
                etUsername.setError("Username can only contain letters, numbers, and underscores");
                etUsername.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void loginWithEmailPassword() {
        showProgress(true);
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Login failed";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Login error", task.getException());
                    }
                });
    }

    private void checkUsernameAndRegister() {
        showProgress(true);
        String username = etUsername.getText().toString().trim().toLowerCase();

        // Check if username already exists
        db.collection("usernames")
                .document(username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            showProgress(false);
                            etUsername.setError("Username is already taken");
                            etUsername.requestFocus();
                        } else {
                            // Username is available, proceed with registration
                            registerWithEmailPassword();
                        }
                    } else {
                        showProgress(false);
                        Toast.makeText(this, "Failed to check username availability", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Username check error", task.getException());
                    }
                });
    }

    private void registerWithEmailPassword() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            createUserProfile(user);
                        } else {
                            showProgress(false);
                            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showProgress(false);
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Registration error", task.getException());
                    }
                });
    }

    private void createUserProfile(FirebaseUser user) {
        String firstName = etFirstName.getText().toString().trim();
        String username = etUsername.getText().toString().trim().toLowerCase();
        String email = user.getEmail();

        // Create user profile data
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("uid", user.getUid());
        userProfile.put("firstName", firstName);
        userProfile.put("username", username);
        userProfile.put("email", email);
        userProfile.put("profilePicture", "");
        userProfile.put("bio", "");
        userProfile.put("joinedDate", System.currentTimeMillis());
        userProfile.put("isVerified", false);
        userProfile.put("followersCount", 0);
        userProfile.put("followingCount", 0);
        userProfile.put("postsCount", 0);

        // Create username reservation
        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", user.getUid());
        usernameData.put("createdAt", System.currentTimeMillis());

        // Save user profile and reserve username
        db.collection("users").document(user.getUid())
                .set(userProfile)
                .addOnCompleteListener(profileTask -> {
                    if (profileTask.isSuccessful()) {
                        // Reserve the username
                        db.collection("usernames").document(username)
                                .set(usernameData)
                                .addOnCompleteListener(usernameTask -> {
                                    showProgress(false);
                                    if (usernameTask.isSuccessful()) {
                                        Toast.makeText(this, "Registration successful! Welcome " + firstName + "!", Toast.LENGTH_LONG).show();
                                        navigateToHome();
                                    } else {
                                        Toast.makeText(this, "Profile created but username reservation failed", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Username reservation error", usernameTask.getException());
                                        navigateToHome();
                                    }
                                });
                    } else {
                        showProgress(false);
                        Toast.makeText(this, "Failed to create user profile", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Profile creation error", profileTask.getException());
                        // Delete the user account if profile creation failed
                        user.delete();
                    }
                });
    }

    private void signInWithGoogle() {
        showProgress(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                showProgress(false);
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Google sign-in error", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkAndCreateGoogleUserProfile(user, account);
                        }
                    } else {
                        showProgress(false);
                        Toast.makeText(this, "Google authentication failed", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Firebase auth error", task.getException());
                    }
                });
    }

    private void checkAndCreateGoogleUserProfile(FirebaseUser user, GoogleSignInAccount account) {
        // Check if user profile already exists
        db.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (!document.exists()) {
                            // Create profile for new Google user
                            createGoogleUserProfile(user, account);
                        } else {
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            navigateToHome();
                        }
                    } else {
                        Toast.makeText(this, "Failed to check user profile", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Profile check error", task.getException());
                    }
                });
    }

    private void createGoogleUserProfile(FirebaseUser user, GoogleSignInAccount account) {
        String displayName = account.getDisplayName();
        String firstName = displayName != null ? displayName.split(" ")[0] : "User";
        String email = user.getEmail();
        String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";

        // Generate a unique username from email
        String baseUsername = email != null ? email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase() : "user";
        generateUniqueUsername(baseUsername, uniqueUsername -> {
            // Create user profile data
            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("uid", user.getUid());
            userProfile.put("firstName", firstName);
            userProfile.put("username", uniqueUsername);
            userProfile.put("email", email);
            userProfile.put("profilePicture", photoUrl);
            userProfile.put("bio", "");
            userProfile.put("joinedDate", System.currentTimeMillis());
            userProfile.put("isVerified", false);
            userProfile.put("followersCount", 0);
            userProfile.put("followingCount", 0);
            userProfile.put("postsCount", 0);
            userProfile.put("loginMethod", "google");

            // Create username reservation
            Map<String, Object> usernameData = new HashMap<>();
            usernameData.put("uid", user.getUid());
            usernameData.put("createdAt", System.currentTimeMillis());

            // Save user profile and reserve username
            db.collection("users").document(user.getUid())
                    .set(userProfile)
                    .addOnCompleteListener(profileTask -> {
                        if (profileTask.isSuccessful()) {
                            db.collection("usernames").document(uniqueUsername)
                                    .set(usernameData)
                                    .addOnCompleteListener(usernameTask -> {
                                        if (usernameTask.isSuccessful()) {
                                            Toast.makeText(this, "Welcome " + firstName + "!", Toast.LENGTH_LONG).show();
                                            navigateToHome();
                                        } else {
                                            Toast.makeText(this, "Profile created successfully", Toast.LENGTH_SHORT).show();
                                            navigateToHome();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Google profile creation error", profileTask.getException());
                        }
                    });
        });
    }

    private void generateUniqueUsername(String baseUsername, UsernameCallback callback) {
        checkUsernameExists(baseUsername, exists -> {
            if (!exists) {
                callback.onResult(baseUsername);
            } else {
                // Try with random numbers
                String newUsername = baseUsername + (int)(Math.random() * 1000);
                generateUniqueUsername(newUsername, callback);
            }
        });
    }

    private void checkUsernameExists(String username, UsernameExistsCallback callback) {
        db.collection("usernames").document(username.toLowerCase())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onResult(task.getResult().exists());
                    } else {
                        callback.onResult(true); // Assume exists on error to be safe
                    }
                });
    }

    private void switchAuthMode() {
        isLoginMode = !isLoginMode;

        if (isLoginMode) {
            btnLogin.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.GONE);
            layoutAdditionalFields.setVisibility(View.GONE);
            tvSwitchToRegister.setText("Don't have an account? Register");
            tvWelcomeTitle.setText("Welcome Back!");
            tvWelcomeSubtitle.setText("Sign in to continue");
        } else {
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.VISIBLE);
            layoutAdditionalFields.setVisibility(View.VISIBLE);
            tvSwitchToRegister.setText("Already have an account? Login");
            tvWelcomeTitle.setText("Create Account");
            tvWelcomeSubtitle.setText("Join our community today");
        }

        // Clear any previous errors
        etEmail.setError(null);
        etPassword.setError(null);
        etFirstName.setError(null);
        etUsername.setError(null);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnRegister.setEnabled(!show);
        btnGoogleSignIn.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
        etFirstName.setEnabled(!show);
        etUsername.setEnabled(!show);
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        if (mAuth.getCurrentUser() != null) {
            navigateToHome();
        }
    }

    // Callback interfaces
    private interface UsernameCallback {
        void onResult(String username);
    }

    private interface UsernameExistsCallback {
        void onResult(boolean exists);
    }
}