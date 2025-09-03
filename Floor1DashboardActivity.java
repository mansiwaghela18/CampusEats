package com.example.shashankscreen;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Floor1DashboardActivity extends AppCompatActivity {

    private static final String TAG = "Floor1Dashboard";

    private GridLayout optionsGrid;
    private CardView profileCard, mainCard;
    private TextView greetingText, subtitleText;
    private FloatingActionButton fabRefresh;
    private ImageView profileImage;
    private AppCompatButton btnLogout;

    // Floor 1 specific management options - Only one option now
    private String[] optionNames = {"Floor 1"};
    private int[] optionIcons = {
            R.drawable.stairs     // Floor icon as requested
    };
    private String[] optionDescriptions = {
            "Main Cafe"
    };

    // Floor-specific information
    private static final String FLOOR_NAME = "Floor 1";
    private static final String FLOOR_DESCRIPTION = "Main Cafe";
    private static final String FLOOR_EMAIL = "floor1@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() started for " + FLOOR_NAME);

        try {
            setContentView(R.layout.activity_floor1_dashboard);

            // Verify user authorization
            if (!verifyUserAccess()) {
                return; // User not authorized, redirected to login
            }

            // Initialize views
            initializeViews();

            // Start entrance animations
            startEntranceAnimations();

            // Generate management option buttons
            generateManagementOptions();

            // Setup floating action button
            setupFloatingActionButton();

            // Setup logout button
            setupLogoutButton();

            Log.d(TAG, "onCreate() completed successfully for " + FLOOR_NAME);

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate(): " + e.getMessage(), e);
            showToast("Error initializing " + FLOOR_NAME + " Dashboard: " + e.getMessage());
        }
    }

    private boolean verifyUserAccess() {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser == null) {
                Log.w(TAG, "No authenticated user found, redirecting to login");
                LoginActivity.performLogout(this);
                return false;
            }

            String userEmail = currentUser.getEmail();
            Log.d(TAG, "Verifying access for user: " + userEmail + " to " + FLOOR_NAME);

            // Check if user is authorized for this floor
            if (!FLOOR_EMAIL.equalsIgnoreCase(userEmail)) {
                Log.w(TAG, "Unauthorized access attempt to " + FLOOR_NAME + " by user: " + userEmail);
                showToast("You are not authorized to access " + FLOOR_NAME);

                // Redirect to login
                LoginActivity.performLogout(this);
                return false;
            }

            Log.d(TAG, "Access verified for " + FLOOR_NAME + " user: " + userEmail);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error verifying user access: " + e.getMessage(), e);
            LoginActivity.performLogout(this);
            return false;
        }
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views for " + FLOOR_NAME);

        try {
            optionsGrid = findViewById(R.id.options_grid);
            profileCard = findViewById(R.id.profile_card);
            mainCard = findViewById(R.id.main_card);
            greetingText = findViewById(R.id.greeting_text);
            subtitleText = findViewById(R.id.subtitle_text);
            fabRefresh = findViewById(R.id.fab_refresh);
            profileImage = findViewById(R.id.profile_image);
            btnLogout = findViewById(R.id.btnLogout);

            if (optionsGrid == null || profileCard == null || mainCard == null ||
                    greetingText == null || subtitleText == null || fabRefresh == null ||
                    profileImage == null || btnLogout == null) {
                throw new RuntimeException("One or more views not found in layout");
            }

            Log.d(TAG, "All views initialized successfully for " + FLOOR_NAME);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            throw e;
        }
    }

    private void startEntranceAnimations() {
        try {
            // Profile animation with bounce
            ObjectAnimator profileScale = ObjectAnimator.ofFloat(profileCard, "scaleX", 0f, 1.2f, 1f);
            ObjectAnimator profileScaleY = ObjectAnimator.ofFloat(profileCard, "scaleY", 0f, 1.2f, 1f);
            profileScale.setDuration(900);
            profileScaleY.setDuration(900);
            profileScale.setInterpolator(new BounceInterpolator());
            profileScaleY.setInterpolator(new BounceInterpolator());

            AnimatorSet profileAnimSet = new AnimatorSet();
            profileAnimSet.playTogether(profileScale, profileScaleY);
            profileAnimSet.start();

            // Greeting text slide and fade in
            new Handler().postDelayed(() -> {
                ObjectAnimator greetingAlpha = ObjectAnimator.ofFloat(greetingText, "alpha", 0f, 1f);
                ObjectAnimator greetingTranslation = ObjectAnimator.ofFloat(greetingText, "translationY", 50f, 0f);
                greetingAlpha.setDuration(600);
                greetingTranslation.setDuration(600);
                greetingAlpha.setInterpolator(new DecelerateInterpolator());
                greetingTranslation.setInterpolator(new DecelerateInterpolator());

                AnimatorSet greetingAnimSet = new AnimatorSet();
                greetingAnimSet.playTogether(greetingAlpha, greetingTranslation);
                greetingAnimSet.start();
            }, 200);

            // Subtitle animation
            new Handler().postDelayed(() -> {
                ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(subtitleText, "alpha", 0f, 1f);
                ObjectAnimator subtitleTranslation = ObjectAnimator.ofFloat(subtitleText, "translationY", 30f, 0f);
                subtitleAlpha.setDuration(500);
                subtitleTranslation.setDuration(500);

                AnimatorSet subtitleAnimSet = new AnimatorSet();
                subtitleAnimSet.playTogether(subtitleAlpha, subtitleTranslation);
                subtitleAnimSet.start();
            }, 400);

            // Main card slide up animation
            new Handler().postDelayed(() -> {
                ObjectAnimator cardAlpha = ObjectAnimator.ofFloat(mainCard, "alpha", 0f, 1f);
                ObjectAnimator cardTranslation = ObjectAnimator.ofFloat(mainCard, "translationY", 200f, 0f);
                cardAlpha.setDuration(700);
                cardTranslation.setDuration(700);
                cardAlpha.setInterpolator(new DecelerateInterpolator());
                cardTranslation.setInterpolator(new DecelerateInterpolator());

                AnimatorSet cardAnimSet = new AnimatorSet();
                cardAnimSet.playTogether(cardAlpha, cardTranslation);
                cardAnimSet.start();
            }, 600);

            // FAB animation
            new Handler().postDelayed(() -> {
                ObjectAnimator fabScale = ObjectAnimator.ofFloat(fabRefresh, "scaleX", 0f, 1f);
                ObjectAnimator fabScaleY = ObjectAnimator.ofFloat(fabRefresh, "scaleY", 0f, 1f);
                fabScale.setDuration(400);
                fabScaleY.setDuration(400);
                fabScale.setInterpolator(new BounceInterpolator());
                fabScaleY.setInterpolator(new BounceInterpolator());

                AnimatorSet fabAnimSet = new AnimatorSet();
                fabAnimSet.playTogether(fabScale, fabScaleY);
                fabAnimSet.start();
            }, 1000);

        } catch (Exception e) {
            Log.e(TAG, "Error in startEntranceAnimations: " + e.getMessage(), e);
        }
    }

    private void generateManagementOptions() {
        try {
            // Clear any existing views
            optionsGrid.removeAllViews();

            // Since we only have one option, we'll center it in the grid
            for (int i = 0; i < optionNames.length; i++) {
                final int index = i;
                CardView optionCard = createOptionCard(optionNames[i], optionDescriptions[i], optionIcons[i]);

                // Create GridLayout params for centered single card
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = dpToPx(200); // Fixed width for better centering
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(0, 2); // Span across both columns to center
                params.rowSpec = GridLayout.spec(0);

                // Set margins for spacing and centering
                int margin = dpToPx(16);
                params.setMargins(margin, margin, margin, margin);
                params.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

                optionCard.setLayoutParams(params);

                // Add animation for the card
                optionCard.setAlpha(0f);
                optionCard.setTranslationY(100f);

                new Handler().postDelayed(() -> {
                    ObjectAnimator alpha = ObjectAnimator.ofFloat(optionCard, "alpha", 0f, 1f);
                    ObjectAnimator translation = ObjectAnimator.ofFloat(optionCard, "translationY", 100f, 0f);
                    alpha.setDuration(500);
                    translation.setDuration(500);
                    alpha.setInterpolator(new DecelerateInterpolator());
                    translation.setInterpolator(new DecelerateInterpolator());

                    AnimatorSet animSet = new AnimatorSet();
                    animSet.playTogether(alpha, translation);
                    animSet.start();
                }, 800);

                optionsGrid.addView(optionCard);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in generateManagementOptions: " + e.getMessage(), e);
        }
    }

    private CardView createOptionCard(String optionName, String description, int iconRes) {
        CardView cardView = new CardView(this);
        cardView.setCardElevation(dpToPx(8));
        cardView.setRadius(dpToPx(20));
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setUseCompatPadding(true);

        // Create linear layout for card content
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(32));
        layout.setGravity(android.view.Gravity.CENTER);

        // Create gradient background
        GradientDrawable gradient = new GradientDrawable();
        gradient.setColors(new int[]{Color.parseColor("#FFF8F3"), Color.WHITE});
        gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        gradient.setCornerRadius(dpToPx(20));
        layout.setBackground(gradient);

        // Icon - larger for single card
        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dpToPx(64), dpToPx(64)));
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#fc9432"));
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        // Option name - larger text for emphasis
        TextView nameText = new TextView(this);
        nameText.setText(optionName);
        nameText.setTextSize(18);
        nameText.setTypeface(nameText.getTypeface(), Typeface.BOLD);
        nameText.setTextColor(Color.parseColor("#333333"));
        nameText.setGravity(android.view.Gravity.CENTER);
        nameText.setPadding(0, dpToPx(12), 0, dpToPx(8));

        // Description
        TextView descText = new TextView(this);
        descText.setText(description);
        descText.setTextSize(14);
        descText.setTextColor(Color.parseColor("#666666"));
        descText.setGravity(android.view.Gravity.CENTER);

        // Add views to layout
        layout.addView(icon);
        layout.addView(nameText);
        layout.addView(descText);
        cardView.addView(layout);

        // Add click animation and listener
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateCardClick(cardView, () -> handleOptionClick(optionName));
            }
        });

        return cardView;
    }

    private void animateCardClick(View view, Runnable onComplete) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        animSet.start();
    }

    private void handleOptionClick(String optionName) {
        Log.d(TAG, "Option clicked: " + optionName + " on " + FLOOR_NAME);

        try {
            Intent intent;

            switch (optionName.trim()) {
                case "Floor 1":
                    // Navigate to Floor 1 Menu Management
                    intent = new Intent(this, OwnerFloor1MenuActivity.class);
                    intent.putExtra("floor_name", FLOOR_NAME);
                    startActivity(intent);
                    break;

                default:
                    showToast("Feature not implemented yet for " + FLOOR_NAME);
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling option click: " + e.getMessage(), e);
            showToast("Error: " + e.getMessage());
        }
    }

    private void setupFloatingActionButton() {
        try {
            fabRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Rotate animation for refresh button
                    ObjectAnimator rotation = ObjectAnimator.ofFloat(fabRefresh, "rotation", 0f, 360f);
                    rotation.setDuration(500);
                    rotation.setInterpolator(new AccelerateDecelerateInterpolator());
                    rotation.start();

                    // Refresh the management options
                    new Handler().postDelayed(() -> {
                        generateManagementOptions();
                    }, 250);

                    showToast(FLOOR_NAME + " Dashboard Refreshed!");
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error setting up FAB: " + e.getMessage(), e);
        }
    }

    private void setupLogoutButton() {
        try {
            btnLogout.setOnClickListener(v -> {
                Log.d(TAG, "Logout clicked from " + FLOOR_NAME);

                animateCardClick(btnLogout, () -> {
                    // Perform logout
                    LoginActivity.performLogout(this);
                });
            });

        } catch (Exception e) {
            Log.e(TAG, "Error setting up logout button: " + e.getMessage(), e);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Verify user access on resume
            if (!verifyUserAccess()) {
                return;
            }

            // Subtle profile image rotation animation
            if (profileImage != null) {
                ObjectAnimator profileRotation = ObjectAnimator.ofFloat(profileImage, "rotation", 0f, 5f, -5f, 0f);
                profileRotation.setDuration(2000);
                profileRotation.setRepeatCount(ObjectAnimator.INFINITE);
                profileRotation.setRepeatMode(ObjectAnimator.REVERSE);
                profileRotation.start();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            // Double-check user authorization
            verifyUserAccess();
        } catch (Exception e) {
            Log.e(TAG, "Error in onStart: " + e.getMessage(), e);
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to login - stay on dashboard or show exit confirmation
        showToast("Use logout button to exit " + FLOOR_NAME + " Dashboard");
    }
}