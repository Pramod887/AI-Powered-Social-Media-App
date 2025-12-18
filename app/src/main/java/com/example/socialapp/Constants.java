package com.example.socialapp;

public class Constants {
    // Firebase Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_POSTS = "posts";
    public static final String COLLECTION_COMMENTS = "comments";

    // Storage References
    public static final String STORAGE_POSTS = "posts";
    public static final String STORAGE_PROFILE_IMAGES = "profile_images";

    // Hugging Face API
    public static final String HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/Salesforce/blip-image-captioning-base";
    public static final String HUGGING_FACE_API_KEY = "YOUR_HUGGING_FACE_API_KEY"; // Replace with your key

    // Image Settings
    public static final int MAX_IMAGE_WIDTH = 1024;
    public static final int MAX_IMAGE_HEIGHT = 1024;
    public static final int IMAGE_COMPRESSION_QUALITY = 80;

    // Request Codes
    public static final int REQUEST_CAMERA = 100;
    public static final int REQUEST_GALLERY = 101;
    public static final int REQUEST_PERMISSIONS = 200;

    // Default Values
    public static final String DEFAULT_USERNAME = "User";
    public static final String DEFAULT_CAPTION = "A beautiful moment captured";



    // DeepAR Constants
    public static final String DEEPAR_LICENSE_KEY = "YOUR_DEEPAR_LICENSE_KEY_HERE";
    public static final String[] DEEPAR_FILTERS = {
            "None",
            "Viking Helmet",
            "Makeup Look",
            "Sunglasses",
            "Beard",
            "Hair Color",
            "Face Mask",
            "Animal Ears",
            "Flower Crown",
            "Neon Glasses"
    };

    // DeepAI Constants
    public static final String DEEPAI_API_KEY = "YOUR_DEEPAI_API_KEY_HERE";
    public static final String DEEPAI_BASE_URL = "https://api.deepai.org/api/";

    // Caption improvement prompts
    public static final String CAPTION_IMPROVEMENT_PROMPT =
            "Improve this social media caption to make it more engaging, creative, and appealing while keeping the original meaning. Make it suitable for Instagram/social media with proper hashtags and emojis:";

    public static final String HASHTAG_GENERATION_PROMPT =
            "Generate 5-10 relevant and trending hashtags for this social media caption:";

    // UI Constants
    public static final int ANIMATION_DURATION = 300;
    public static final int FILTER_PROCESSING_DELAY = 2000;
    public static final int CAPTION_MAX_LENGTH = 500;

    // Error Messages
    public static final String ERROR_NO_IMAGE = "Please select an image first";
    public static final String ERROR_NO_INTERNET = "No internet connection";
    public static final String ERROR_FILTER_FAILED = "Filter application failed";
    public static final String ERROR_CAPTION_FAILED = "Caption improvement failed";
    public static final String ERROR_UPLOAD_FAILED = "Upload failed";

    // Success Messages
    public static final String SUCCESS_FILTER_APPLIED = "Filter applied successfully!";
    public static final String SUCCESS_CAPTION_IMPROVED = "Caption improved!";
    public static final String SUCCESS_POST_UPLOADED = "Post uploaded successfully!";
    public static final String COLLECTION_LIKES = "likes";
}
