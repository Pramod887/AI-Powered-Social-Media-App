package com.example.socialapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AIChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etMessage;
    private FloatingActionButton fabSend;
    private View emptyState;
    private View btnBack;
    private View btnMenu;
    private View btnAttachment;
    private View btnQuickQuestion;
    private View btnGetHelp;
    private View typingIndicator;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private OkHttpClient httpClient;

    // Hugging Face API configuration
    private static final String API_URL = "https://api.perplexity.ai/chat/completions";
    private static final String API_TOKEN = "YOUR_PERPLEXITY_API_KEY"; // You'll need to get this from huggingface.co

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aichat);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupHttpClient();
        setupSendButton();

        // Show welcome message
        addWelcomeMessage();
    }

    private void initializeViews() {
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        fabSend = findViewById(R.id.fabSend);
        emptyState = findViewById(R.id.emptyState);
        btnBack = findViewById(R.id.btnBack);
        btnMenu = findViewById(R.id.btnMenu);
        btnAttachment = findViewById(R.id.btnAttachment);
        btnQuickQuestion = findViewById(R.id.btnQuickQuestion);
        btnGetHelp = findViewById(R.id.btnGetHelp);
        typingIndicator = findViewById(R.id.typingIndicator);
    }

    private void setupToolbar() {
        // Toolbar is now handled by the custom layout
        // No need to set up action bar since we have custom buttons
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);
    }

    private void setupHttpClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    private void setupSendButton() {
        fabSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Setup new button listeners
        btnBack.setOnClickListener(v -> finish());
        btnMenu.setOnClickListener(v -> showMenu());
        btnAttachment.setOnClickListener(v -> attachFile());
        btnQuickQuestion.setOnClickListener(v -> askQuickQuestion());
        btnGetHelp.setOnClickListener(v -> getHelp());
    }

    private void addWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage(
                "Hello! I'm your AI assistant. How can I help you today? 😊",
                false,
                System.currentTimeMillis()
        );
        chatMessages.add(welcomeMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        emptyState.setVisibility(View.GONE);
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        // Add user message
        ChatMessage userMessage = new ChatMessage(messageText, true, System.currentTimeMillis());
        chatMessages.add(userMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChat.scrollToPosition(chatMessages.size() - 1);

        // Clear input
        etMessage.setText("");

        // Show loading
        showLoading(true);

        // Send API request
        sendAIRequest(messageText);
    }
    private void sendAIRequest(String message) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("model", "sonar"); // Using the universally supported model

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", "You are a helpful assistant."));
            messages.put(new JSONObject().put("role", "user").put("content", message));
            payload.put("messages", messages);

            RequestBody body = RequestBody.create(
                    payload.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(API_URL) // should be "https://api.perplexity.ai/chat/completions"
                    .addHeader("Authorization", "Bearer " + API_TOKEN)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        handleError("Connection failed: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> showLoading(false));
                    if (!response.isSuccessful()) {
                        String msg = response.body() != null ? response.body().string() : "";
                        runOnUiThread(() -> handleError("API error " + response.code() + ": " + msg));
                        return;
                    }
                    try {
                        String resBody = response.body().string();
                        JSONObject json = new JSONObject(resBody);
                        JSONArray choices = json.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            String aiText = choices.getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");
                            runOnUiThread(() -> addAIResponse(aiText));
                        } else {
                            runOnUiThread(() -> handleError("No response from AI."));
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> handleError("Parsing error: " + e.getMessage()));
                    }
                }
            });
        } catch (JSONException e) {
            showLoading(false);
            handleError("JSON error: " + e.getMessage());
        }
    }


//    private void sendAIRequest(String message) {
//        try {
//            JSONObject payload = new JSONObject();
//            payload.put("inputs", message);
//            payload.put("parameters", new JSONObject().put("max_length", 100));
//
//            RequestBody body = RequestBody.create(
//                    payload.toString(),
//                    MediaType.get("application/json; charset=utf-8")
//            );
//
//            Request request = new Request.Builder()
//                    .url(API_URL)
//                    .addHeader("Authorization", "Bearer " + API_TOKEN)
//                    .addHeader("Content-Type", "application/json")
//                    .post(body)
//                    .build();
//
//            httpClient.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(Call call, IOException e) {
//                    runOnUiThread(() -> {
//                        showLoading(false);
//                        handleError("Connection failed. Please check your internet connection.");
//                    });
//                }
//
//                @Override
//                public void onResponse(Call call, Response response) throws IOException {
//                    runOnUiThread(() -> showLoading(false));
//
//                    if (!response.isSuccessful()) {
//                        runOnUiThread(() -> handleError("API request failed. Please try again."));
//                        return;
//                    }
//
//                    try {
//                        String responseBody = response.body().string();
//                        JSONArray jsonArray = new JSONArray(responseBody);
//
//                        if (jsonArray.length() > 0) {
//                            JSONObject firstResult = jsonArray.getJSONObject(0);
//                            String aiResponse = firstResult.getString("generated_text");
//
//                            // Remove the original input from the response
//                            if (aiResponse.startsWith(message)) {
//                                aiResponse = aiResponse.substring(message.length()).trim();
//                            }
//
//                            // If response is empty, provide a fallback
//                            if (aiResponse.isEmpty()) {
//                                aiResponse = "I understand. Could you tell me more about that?";
//                            }
//
//                            String finalAiResponse = aiResponse;
//                            runOnUiThread(() -> addAIResponse(finalAiResponse));
//                        } else {
//                            runOnUiThread(() -> handleError("No response from AI. Please try again."));
//                        }
//                    } catch (JSONException e) {
//                        runOnUiThread(() -> handleError("Failed to parse AI response."));
//                    }
//                }
//            });
//
//        } catch (JSONException e) {
//            showLoading(false);
//            handleError("Failed to create request.");
//        }
//    }

    private void addAIResponse(String response) {
        ChatMessage aiMessage = new ChatMessage(response, false, System.currentTimeMillis());
        chatMessages.add(aiMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChat.scrollToPosition(chatMessages.size() - 1);
    }

    private void handleError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

        // Add error message to chat
        ChatMessage errorMsg = new ChatMessage(
                "Sorry, I'm having trouble responding right now. Please try again later. 😔",
                false,
                System.currentTimeMillis()
        );
        chatMessages.add(errorMsg);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChat.scrollToPosition(chatMessages.size() - 1);
    }

    private void showLoading(boolean show) {
        typingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        fabSend.setEnabled(!show);
    }

    private void showMenu() {
        // TODO: Implement menu options
        Toast.makeText(this, "Menu options coming soon", Toast.LENGTH_SHORT).show();
    }

    private void attachFile() {
        // TODO: Implement file attachment
        Toast.makeText(this, "File attachment coming soon", Toast.LENGTH_SHORT).show();
    }

    private void askQuickQuestion() {
        etMessage.setText("What can you help me with?");
        etMessage.requestFocus();
    }

    private void getHelp() {
        etMessage.setText("I need help with...");
        etMessage.requestFocus();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}