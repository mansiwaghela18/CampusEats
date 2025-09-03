package com.example.shashankscreen;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
    }
}