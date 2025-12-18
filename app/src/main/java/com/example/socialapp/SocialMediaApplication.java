package com.example.socialapp;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class SocialMediaApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Initialize other SDKs here
        // Example: DeepAR.initialize(this);
    }
}