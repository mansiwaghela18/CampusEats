package com.example.shashankscreen;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.AlphaAnimation;
import android.view.animation.RotateAnimation;
import android.view.animation.BounceInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "Starting SplashActivity");
            setContentView(R.layout.activity_main);

            // Play splash sound
            playStartupSound();

            // Find and animate logo
            ImageView logo = findViewById(R.id.logoImage);
            TextView appTitle = findViewById(R.id.appTitle);
            TextView versionInfo = findViewById(R.id.versionInfo);
            ProgressBar loadingIndicator = findViewById(R.id.loadingIndicator);

            if (logo != null) {
                Log.d(TAG, "Logo found, starting enhanced animation");
                startEnhancedAnimation(logo);

                // Animate other elements with delays
                if (appTitle != null) {
                    animateTextElement(appTitle, 1500);
                }
                if (loadingIndicator != null) {
                    animateLoadingIndicator(loadingIndicator, 2000);
                }
                if (versionInfo != null) {
                    animateTextElement(versionInfo, 2500);
                }
            } else {
                Log.w(TAG, "Logo ImageView not found");
            }

            // Navigate to RegistrationActivity after 3 seconds
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Navigating to RegistrationActivity");
                    Intent intent = new Intent(SplashActivity.this, RegistrationActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }
            }, 3000);

        } catch (Exception e) {
            Log.e(TAG, "Critical error in SplashActivity onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    private void playStartupSound() {
        try {
            // Play a system notification sound or custom sound
            // For custom sound: place your sound file in res/raw/ folder
            // mediaPlayer = MediaPlayer.create(this, R.raw.startup_sound);

            // Using system notification sound as default
            mediaPlayer = MediaPlayer.create(this, R.raw.startup_sha);

            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                        mediaPlayer = null;
                    }
                });
                mediaPlayer.start();
                Log.d(TAG, "Startup sound played successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to play startup sound: " + e.getMessage());
        }
    }

    private void startEnhancedAnimation(ImageView logo) {
        try {
            // Create a combination of animations
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.setInterpolator(new BounceInterpolator());

            // Fade in animation
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(1000);
            fadeIn.setStartOffset(200);

            // Scale animation (zoom in effect)
            ScaleAnimation scaleIn = new ScaleAnimation(
                    0.3f, 1.0f, // X scale: from 30% to 100%
                    0.3f, 1.0f, // Y scale: from 30% to 100%
                    Animation.RELATIVE_TO_SELF, 0.5f, // Pivot X: center
                    Animation.RELATIVE_TO_SELF, 0.5f  // Pivot Y: center
            );
            scaleIn.setDuration(1200);
            scaleIn.setStartOffset(300);

            // Subtle rotation for dynamic effect
            RotateAnimation rotate = new RotateAnimation(
                    -5f, 5f, // Rotate from -5° to +5°
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotate.setDuration(800);
            rotate.setStartOffset(800);
            rotate.setRepeatCount(1);
            rotate.setRepeatMode(Animation.REVERSE);

            // Add all animations to the set
            animationSet.addAnimation(fadeIn);
            animationSet.addAnimation(scaleIn);
            animationSet.addAnimation(rotate);

            // Set animation listener for additional effects
            animationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    Log.d(TAG, "Enhanced animation started");
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Log.d(TAG, "Enhanced animation completed");
                    // Add a subtle pulse effect after main animation
                    startPulseAnimation(logo);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // Not used
                }
            });

            logo.startAnimation(animationSet);

        } catch (Exception e) {
            Log.e(TAG, "Enhanced animation failed: " + e.getMessage());
            // Fallback to simple fade in
            Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            logo.startAnimation(fadeIn);
        }
    }

    private void animateTextElement(TextView textView, long delay) {
        try {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(800);
            fadeIn.setStartOffset(delay);
            fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

            textView.startAnimation(fadeIn);
        } catch (Exception e) {
            Log.e(TAG, "Text animation failed: " + e.getMessage());
        }
    }

    private void animateLoadingIndicator(ProgressBar progressBar, long delay) {
        try {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(600);
            fadeIn.setStartOffset(delay);

            progressBar.startAnimation(fadeIn);
        } catch (Exception e) {
            Log.e(TAG, "Loading indicator animation failed: " + e.getMessage());
        }
    }

    private void startPulseAnimation(ImageView logo) {
        try {
            // Create a gentle pulse effect
            ScaleAnimation pulse = new ScaleAnimation(
                    1.0f, 1.05f, // X scale: from 100% to 105%
                    1.0f, 1.05f, // Y scale: from 100% to 105%
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            pulse.setDuration(800);
            pulse.setRepeatCount(1);
            pulse.setRepeatMode(Animation.REVERSE);

            logo.startAnimation(pulse);
        } catch (Exception e) {
            Log.e(TAG, "Pulse animation failed: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up media player
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "Error releasing media player: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop sound if activity is paused
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }
}