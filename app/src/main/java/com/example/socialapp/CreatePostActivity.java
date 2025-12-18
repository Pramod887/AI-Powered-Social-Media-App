package com.example.socialapp;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.Timestamp;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CreatePostActivity extends AppCompatActivity {

    private ImageView ivPreview;
    private EditText etCaption;
    private Button btnCamera, btnGallery, btnPost, btnApplyFilter;
    private MaterialButton btnImproveCaption;
    private MaterialCardView improveCaptionCard, captionCard;
    private LinearLayout resizeHandle;
    private ProgressBar progressBar;

    private Bitmap selectedBitmap;
    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUsername = Constants.DEFAULT_USERNAME;
    private String currentProfileImageUrl = "";
    private ARFilterManager arFilterManager;

    // For resize functionality
    private boolean isResizing = false;
    private float startY = 0f;
    private int initialHeight = 0;
    private int minHeight = 0;
    private int maxHeight = 0;

    // Perplexity AI configuration
    private static final String PERPLEXITY_API_URL = "https://api.perplexity.ai/chat/completions";
    private static final String PERPLEXITY_API_TOKEN = "YOUR_PERPLEXITY_API_KEY";
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        arFilterManager = new ARFilterManager(this);

        initViews();
        setupHttpClient();
        setupClickListeners();
        setupCaptionWatcher();
        checkPermissions();
        loadUserData();

        // Initialize AR filters
        arFilterManager.initialize();
    }

    private void initViews() {
        ivPreview = findViewById(R.id.ivPreview);
        etCaption = findViewById(R.id.etCaption);
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);
        btnPost = findViewById(R.id.btnPost);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnImproveCaption = findViewById(R.id.btnImproveCaption);
        improveCaptionCard = btnImproveCaption.getParent() instanceof MaterialCardView ?
                (MaterialCardView) btnImproveCaption.getParent() : null;
        captionCard = findViewById(R.id.captionCard);
        resizeHandle = findViewById(R.id.resizeHandle);
        progressBar = findViewById(R.id.progressBar);

        // Ensure caption is scrollable
        try {
            etCaption.setVerticalScrollBarEnabled(true);
            etCaption.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
            etCaption.setMovementMethod(android.text.method.ScrollingMovementMethod.getInstance());
            etCaption.setOnTouchListener((v, event) -> {
                // When user scrolls inside EditText, prevent parent (ScrollView) from intercepting
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
        } catch (Exception ignored) {}

        // Setup resize functionality
        setupResizeHandle();
    }

    private void setupHttpClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private void setupClickListeners() {
        btnCamera.setOnClickListener(v -> openCamera());
        btnGallery.setOnClickListener(v -> openGallery());
        btnPost.setOnClickListener(v -> uploadPost());
        btnApplyFilter.setOnClickListener(v -> showFilterOptions());
        btnImproveCaption.setOnClickListener(v -> improveCaptionWithAI());
    }

    private void setupCaptionWatcher() {
        etCaption.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show improve caption button when user starts typing
                if (s.length() > 0) {
                    showImproveCaptionButton(true);
                } else {
                    showImproveCaptionButton(false);
                }

                // Auto-resize based on content
                autoResizeEditText();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void autoResizeEditText() {
        // Get line count and adjust height accordingly
        int lineCount = etCaption.getLineCount();
        int maxLines = 5;
        int minLines = 2;

        // Calculate desired lines based on content
        int desiredLines = Math.max(minLines, Math.min(lineCount + 1, maxLines));

        // Set the lines programmatically
        etCaption.setMinLines(desiredLines);

        // If content exceeds max lines, enable scrolling
        if (lineCount > maxLines) {
            etCaption.setMaxLines(maxLines);
            etCaption.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
            etCaption.setMovementMethod(android.text.method.ScrollingMovementMethod.getInstance());
        } else {
            etCaption.setMovementMethod(null);
        }
    }

    private void setupResizeHandle() {
        if (resizeHandle != null && captionCard != null) {
            resizeHandle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            isResizing = true;
                            startY = event.getRawY();
                            initialHeight = captionCard.getHeight();

                            // Calculate min and max heights
                            int lineHeight = etCaption.getLineHeight();
                            minHeight = (lineHeight * 2) + etCaption.getPaddingTop() +
                                    etCaption.getPaddingBottom() + resizeHandle.getHeight();
                            maxHeight = (lineHeight * 5) + etCaption.getPaddingTop() +
                                    etCaption.getPaddingBottom() + resizeHandle.getHeight();

                            // Visual feedback
                            resizeHandle.setAlpha(0.8f);
                            return true;

                        case android.view.MotionEvent.ACTION_MOVE:
                            if (isResizing) {
                                float deltaY = event.getRawY() - startY;
                                int newHeight = (int) (initialHeight + deltaY);

                                // Constrain height within bounds
                                newHeight = Math.max(minHeight, Math.min(maxHeight, newHeight));

                                // Apply new height
                                android.view.ViewGroup.LayoutParams params = captionCard.getLayoutParams();
                                params.height = newHeight;
                                captionCard.setLayoutParams(params);

                                // Update EditText lines based on new height
                                updateEditTextLines(newHeight);
                            }
                            return true;

                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            isResizing = false;
                            resizeHandle.setAlpha(1.0f);
                            return true;
                    }
                    return false;
                }
            });

            // Add visual feedback on press
            resizeHandle.setOnClickListener(v -> {
                // Toggle between min and max height
                android.view.ViewGroup.LayoutParams params = captionCard.getLayoutParams();
                int lineHeight = etCaption.getLineHeight();
                int currentHeight = captionCard.getHeight();

                int minH = (lineHeight * 2) + etCaption.getPaddingTop() +
                        etCaption.getPaddingBottom() + resizeHandle.getHeight();
                int maxH = (lineHeight * 5) + etCaption.getPaddingTop() +
                        etCaption.getPaddingBottom() + resizeHandle.getHeight();

                if (Math.abs(currentHeight - minH) < Math.abs(currentHeight - maxH)) {
                    // Currently closer to min, expand to max
                    params.height = maxH;
                    etCaption.setMinLines(5);
                    etCaption.setMaxLines(5);
                } else {
                    // Currently closer to max, shrink to min
                    params.height = minH;
                    etCaption.setMinLines(2);
                    etCaption.setMaxLines(5);
                }

                captionCard.setLayoutParams(params);

                // Animate the change
                captionCard.animate()
                        .scaleY(1.02f)
                        .setDuration(100)
                        .withEndAction(() ->
                                captionCard.animate()
                                        .scaleY(1.0f)
                                        .setDuration(100)
                                        .start())
                        .start();
            });
        }
    }

    private void updateEditTextLines(int containerHeight) {
        if (resizeHandle != null) {
            int availableHeight = containerHeight - resizeHandle.getHeight() -
                    etCaption.getPaddingTop() - etCaption.getPaddingBottom();
            int lineHeight = etCaption.getLineHeight();
            int calculatedLines = Math.max(2, Math.min(5, availableHeight / lineHeight));

            etCaption.setMinLines(calculatedLines);
            etCaption.setMaxLines(Math.max(calculatedLines, 5));
        }
    }

    private void showImproveCaptionButton(boolean show) {
        if (improveCaptionCard != null) {
            improveCaptionCard.setVisibility(show ? View.VISIBLE : View.GONE);
        } else if (btnImproveCaption != null) {
            btnImproveCaption.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void improveCaptionWithAI() {
        String currentCaption = etCaption.getText().toString().trim();

        if (currentCaption.isEmpty()) {
            Toast.makeText(this, "Please write a caption first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        btnImproveCaption.setEnabled(false);
        btnImproveCaption.setText("Improving...");

        // Send ONLY the raw caption. All behavior is enforced via system prompt.
        sendAIRequest(currentCaption, new AIResponseCallback() {
            @Override
            public void onSuccess(String improvedCaption) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnImproveCaption.setEnabled(true);
                    btnImproveCaption.setText("Improve with AI");

                    // Post-processing for 2-3 lines:
                    // - Normalize CRLF to LF
                    // - Trim each line
                    // - Remove surrounding quotes
                    // - Cap to 3 lines and reasonable total length
                    String normalized = improvedCaption
                            .replace("\r\n", "\n")
                            .replace("\r", "\n")
                            .trim();

                    if ((normalized.startsWith("\"") && normalized.endsWith("\"")) ||
                        (normalized.startsWith("“") && normalized.endsWith("”"))) {
                        normalized = normalized.substring(1, Math.max(1, normalized.length() - 1)).trim();
                    }

                    // Split to lines, trim each, remove empty lines
                    String[] rawLines = normalized.split("\n");
                    java.util.ArrayList<String> lines = new java.util.ArrayList<>();
                    for (String line : rawLines) {
                        String clean = line.replaceAll("\\s+", " ").trim();
                        if (!clean.isEmpty()) lines.add(clean);
                    }
                    // Ensure 2-3 lines: if only one line returned, try to split by punctuation
                    if (lines.size() < 2) {
                        String base = lines.isEmpty() ? normalized : lines.get(0);
                        String[] splitByPunct = base.split("(?<=[.!?]) ");
                        lines.clear();
                        for (String s : splitByPunct) {
                            String c = s.trim();
                            if (!c.isEmpty()) lines.add(c);
                            if (lines.size() >= 3) break;
                        }
                        if (lines.isEmpty()) lines.add(base);
                    }
                    // Cap to 3 lines
                    while (lines.size() > 3) lines.remove(lines.size() - 1);

                    // Cap each line length
                    final int MAX_LINE_LEN = 100; // keep lines short
                    for (int i = 0; i < lines.size(); i++) {
                        String l = lines.get(i);
                        if (l.length() > MAX_LINE_LEN) {
                            l = l.substring(0, MAX_LINE_LEN).trim();
                            lines.set(i, l);
                        }
                    }

                    // Rejoin with newlines
                    String finalText = String.join("\n", lines).trim();
                    etCaption.setText(finalText);
                    etCaption.setSelection(finalText.length());

                    Toast.makeText(CreatePostActivity.this, "Caption improved! ✨", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnImproveCaption.setEnabled(true);
                    btnImproveCaption.setText("Improve with AI");

                    Toast.makeText(CreatePostActivity.this, "Failed to improve caption: " + error, Toast.LENGTH_LONG).show();
                    Log.e("CreatePostActivity", "AI Caption Error: " + error);
                });
            }
        });
    }

    private void sendAIRequest(String message, AIResponseCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("model", "sonar");

            JSONArray messages = new JSONArray();
            // Strict system prompt to enforce 2-3 short lines
            messages.put(new JSONObject().put("role", "system").put("content",
                    "You edit social media captions.\n" +
                    "Rules:\n" +
                    "- Return ONLY the improved caption as 2–3 short lines.\n" +
                    "- Each line should be concise and punchy (<= 100 chars).\n" +
                    "- Preserve the original meaning and tone.\n" +
                    "- Do NOT add calls-to-action unless present in the original.\n" +
                    "- Do NOT mention filters, app features, brand names, or instructions.\n" +
                    "- Do NOT add emojis/hashtags unless already present in the original.\n" +
                    "- No quotes, no markdown, no bullets.\n"));
            messages.put(new JSONObject().put("role", "user").put("content", message));
            payload.put("messages", messages);
            // Keep outputs compact
            payload.put("max_tokens", 120);
            payload.put("temperature", 0.6);

            RequestBody body = RequestBody.create(
                    payload.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(PERPLEXITY_API_URL)
                    .addHeader("Authorization", "Bearer " + PERPLEXITY_API_TOKEN)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Connection failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorMsg = response.body() != null ? response.body().string() : "Unknown error";
                        callback.onError("API error " + response.code() + ": " + errorMsg);
                        return;
                    }

                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        JSONArray choices = json.optJSONArray("choices");

                        if (choices != null && choices.length() > 0) {
                            String aiResponse = choices.getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content")
                                    .trim();

                            callback.onSuccess(aiResponse);
                        } else {
                            callback.onError("No response from AI");
                        }
                    } catch (JSONException e) {
                        callback.onError("Parsing error: " + e.getMessage());
                    }
                }
            });

        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Interface for AI response callback
    private interface AIResponseCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    private void loadUserData() {
        FirebaseHelper.getCurrentUser(new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.getUsername() != null) {
                    currentUsername = user.getUsername();
                }
                if (user != null && user.getProfileImageUrl() != null) {
                    currentProfileImageUrl = user.getProfileImageUrl();
                }
            }

            @Override
            public void onError(String error) {
                // Use default username
            }
        });
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, Constants.REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "Permissions required for camera and gallery access",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private Uri photoUri;

    private void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        cameraLauncher.launch(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, Constants.REQUEST_GALLERY);
    }

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getParcelableExtra("uri");
                    if (uri != null) {
                        Log.d("nasirrr", uri.toString());
                        try {
                            ivPreview.setImageURI(uri);
                            findViewById(R.id.emptyStateLayout).setVisibility(View.GONE);
                            selectedBitmap = ImageUtils.getBitmapFromUri(this, uri);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            try {
                switch (requestCode) {
                    case Constants.REQUEST_CAMERA:
                        if (photoUri != null) {
                            selectedBitmap = ImageUtils.getBitmapFromUri(this, photoUri);
                            processSelectedImage();
                        }
                        break;

                    case Constants.REQUEST_GALLERY:
                        Uri imageUri = data.getData();
                        if (imageUri != null) {
                            selectedBitmap = ImageUtils.getBitmapFromUri(this, imageUri);

                            // Handle image rotation
                            int orientation = ImageUtils.getImageOrientation(this, imageUri);
                            if (orientation != 0) {
                                selectedBitmap = ImageUtils.rotateBitmap(selectedBitmap, orientation);
                            }

                            processSelectedImage();
                        }
                        break;
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error loading image: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processSelectedImage() {
        if (selectedBitmap != null) {
            // Resize image if too large
            selectedBitmap = ImageUtils.resizeBitmap(selectedBitmap,
                    Constants.MAX_IMAGE_WIDTH, Constants.MAX_IMAGE_HEIGHT);

            ivPreview.setImageBitmap(selectedBitmap);
            findViewById(R.id.emptyStateLayout).setVisibility(View.GONE);

            // Generate AI caption if needed
            // generateCaption();
        }
    }

    private void showFilterOptions() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] filters = arFilterManager.getAvailableFilters();

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Choose Filter")
                .setItems(filters, (dialog, which) -> {
                    applyFilter(filters[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyFilter(String filterName) {
        progressBar.setVisibility(View.VISIBLE);

        arFilterManager.applyFilter(filterName, selectedBitmap,
                new ARFilterManager.FilterCallback() {
                    @Override
                    public void onSuccess(Bitmap filteredBitmap) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            selectedBitmap = filteredBitmap;
                            ivPreview.setImageBitmap(selectedBitmap);
                            Toast.makeText(CreatePostActivity.this,
                                    "Filter applied!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(CreatePostActivity.this,
                                    "Filter failed: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void generateCaption() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            etCaption.setText(Constants.DEFAULT_CAPTION);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        AIService.generateCaption(selectedBitmap, new AIService.CaptionCallback() {
            @Override
            public void onSuccess(String caption) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    etCaption.setText(caption);
                    Log.d("CreatePostActivity", "Generated Caption: " + caption);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    etCaption.setText(Constants.DEFAULT_CAPTION);
                    Log.e("CreatePostActivity", "Caption generation failed: " + error);
                });
            }
        });
    }

    private void uploadPost() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        String caption = etCaption.getText().toString().trim();
        if (caption.isEmpty()) {
            caption = Constants.DEFAULT_CAPTION;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnPost.setEnabled(false);

        // Upload image to Firebase Storage
        StorageReference imageRef = storage.getReference()
                .child(Constants.STORAGE_POSTS + "/" + System.currentTimeMillis() + ".jpg");

        byte[] data = ImageUtils.bitmapToByteArray(selectedBitmap, Constants.IMAGE_COMPRESSION_QUALITY);

        String finalCaption = caption;
        imageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        savePostToFirestore(uri.toString(), finalCaption);
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnPost.setEnabled(true);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void savePostToFirestore(String imageUrl, String caption) {
        Map<String, Object> post = new HashMap<>();
        post.put("userId", currentUserId);
        post.put("username", currentUsername);
        post.put("imageUrl", imageUrl);
        post.put("profileImageUrl", currentProfileImageUrl);
        post.put("caption", caption);
        post.put("likes", new ArrayList<String>());
        post.put("commentCount", 0);
        post.put("timestamp", Timestamp.now());

        db.collection(Constants.COLLECTION_POSTS)
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Post uploaded successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnPost.setEnabled(true);
                    Toast.makeText(this, "Failed to save post: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}