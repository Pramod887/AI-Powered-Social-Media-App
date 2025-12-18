package com.example.socialapp;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIService {

    private static final String DEEPAI_API_KEY = "YOUR_DEEPAI_KEY"; // replace with your key
    private static final String DEEPAI_URL = "https://api.deepai.org/api/image-captioning";

    public interface CaptionCallback {
        void onSuccess(String caption);
        void onError(String error);
    }

    public static void generateCaption(Bitmap bitmap, CaptionCallback callback) {
        try {
            // Convert bitmap to byte array
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            byte[] byteArray = stream.toByteArray();
            stream.close();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                            "image",
                            "image.jpg",
                            RequestBody.create(byteArray, MediaType.parse("image/jpeg"))
                    )
                    .build();

            Request request = new Request.Builder()
                    .url(DEEPAI_URL)
                    .addHeader("api-key", DEEPAI_API_KEY)
                    .post(requestBody)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                Handler mainHandler = new Handler(Looper.getMainLooper());

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                    Log.e("AIService", "Request failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String jsonString = response.body().string();
                    Log.d("AIService", "DeepAI Response: " + jsonString);
                    try {
                        JSONObject obj = new JSONObject(jsonString);
                        String caption = obj.optString("output", Constants.DEFAULT_CAPTION);
                        mainHandler.post(() -> callback.onSuccess(caption));
                    } catch (JSONException e) {
                        mainHandler.post(() -> callback.onError("JSON parse error"));
                        Log.e("AIService", "JSON parse error: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onError(e.getMessage());
            Log.e("AIService", "Bitmap processing error: " + e.getMessage());
        }
    }
}
