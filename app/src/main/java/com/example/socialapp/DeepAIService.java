package com.example.socialapp;

import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class DeepAIService {
    private static final String TAG = "DeepAIService";
    private static final String DEEPAI_API_KEY = "31ac6f58-8c20-4718-aeb4-b84f69033c43"; // Replace with your actual API key
    private static final String DEEPAI_BASE_URL = "https://api.deepai.org/api/";
    private static final String TEXT_IMPROVEMENT_ENDPOINT = "text-generator";

    private static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface TextImprovementCallback {
        void onSuccess(String improvedText);
        void onError(String error);
    }

    public static void improveText(String originalText, TextImprovementCallback callback) {
        // Create a prompt for text improvement
        String prompt = "Improve this social media caption to make it more engaging, creative, and appealing while keeping the original meaning. Make it suitable for Instagram/social media with proper hashtags:\n\n" + originalText;

        RequestBody formBody = new FormBody.Builder()
                .add("text", prompt)
                .build();

        Request request = new Request.Builder()
                .url(DEEPAI_BASE_URL + TEXT_IMPROVEMENT_ENDPOINT)
                .addHeader("Api-Key", DEEPAI_API_KEY)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (jsonResponse.has("output")) {
                            String improvedText = jsonResponse.getString("output");
                            // Clean up the response and limit length
                            improvedText = cleanupImprovedText(improvedText, originalText);
                            callback.onSuccess(improvedText);
                        } else {
                            callback.onError("Invalid response format");
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "API Error: " + response.code() + " - " + errorBody);
                        callback.onError("API Error: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Response parsing error: " + e.getMessage());
                    callback.onError("Response parsing error: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    private static String cleanupImprovedText(String improvedText, String originalText) {
        // Remove any unwanted prefixes or suffixes that might be added by the AI
        improvedText = improvedText.trim();

        // Remove common AI response prefixes
        String[] prefixesToRemove = {
                "Here's an improved version:",
                "Improved caption:",
                "Here's a better version:",
                "Enhanced caption:",
                "Here's the improved text:"
        };

        for (String prefix : prefixesToRemove) {
            if (improvedText.toLowerCase().startsWith(prefix.toLowerCase())) {
                improvedText = improvedText.substring(prefix.length()).trim();
            }
        }

        // Limit caption length for social media (Instagram limit is around 2200 characters)
        if (improvedText.length() > 500) {
            improvedText = improvedText.substring(0, 497) + "...";
        }

        // If the improved text is too short or seems invalid, return original
        if (improvedText.length() < 10) {
            return originalText;
        }

        return improvedText;
    }

    // Alternative method using a different DeepAI endpoint for text enhancement
    public static void enhanceTextCreativity(String originalText, TextImprovementCallback callback) {
        String prompt = "Rewrite this social media post to be more creative, engaging, and viral-worthy. Add relevant emojis and hashtags:\n\n" + originalText;

        RequestBody formBody = new FormBody.Builder()
                .add("text", prompt)
                .build();

        Request request = new Request.Builder()
                .url(DEEPAI_BASE_URL + "text-generator")
                .addHeader("Api-Key", DEEPAI_API_KEY)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (jsonResponse.has("output")) {
                            String enhancedText = jsonResponse.getString("output");
                            enhancedText = cleanupImprovedText(enhancedText, originalText);
                            callback.onSuccess(enhancedText);
                        } else {
                            callback.onError("Invalid response format");
                        }
                    } else {
                        callback.onError("API Error: " + response.code());
                    }
                } catch (Exception e) {
                    callback.onError("Response parsing error: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    // Method to generate hashtags for the caption
    public static void generateHashtags(String caption, TextImprovementCallback callback) {
        String prompt = "Generate 5-10 relevant and trending hashtags for this social media caption:\n\n" + caption;

        RequestBody formBody = new FormBody.Builder()
                .add("text", prompt)
                .build();

        Request request = new Request.Builder()
                .url(DEEPAI_BASE_URL + TEXT_IMPROVEMENT_ENDPOINT)
                .addHeader("Api-Key", DEEPAI_API_KEY)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (jsonResponse.has("output")) {
                            String hashtags = jsonResponse.getString("output");
                            callback.onSuccess(caption + "\n\n" + hashtags);
                        } else {
                            callback.onError("Invalid response format");
                        }
                    } else {
                        callback.onError("API Error: " + response.code());
                    }
                } catch (Exception e) {
                    callback.onError("Response parsing error: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }
}