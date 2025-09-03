package com.example.shashankscreen;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Enable offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
