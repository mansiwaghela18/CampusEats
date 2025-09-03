package com.example.shashankscreen;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

public class OwnerDashboardActivity extends AppCompatActivity {

    private GridLayout floorGrid;
    private CardView profileCard, mainCard;
    private TextView greetingText, subtitleText;
    private FloatingActionButton fabRefresh;
    private ImageView profileImage;

    private String[] floorNames = {"Floor 1", "Floor 2", "Floor 3", "Floor 4"};
    private int[] floorIcons = {
            R.drawable.stairs,         // Main Cafe icon
            R.drawable.stairs,       // Quick Bites icon
            R.drawable.stairs,      // Tasty Dining icon
            R.drawable.stairs       // Rooftop Cafe icon
    };
    private String[] floorDescriptions = {
            "Main Cafe",
            "Quick Bites",
            "Tasty Dining",
            "Rooftop Cafe"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        // Initialize views
        initializeViews();

        // Start entrance animations
        startEntranceAnimations();

        // Generate floor buttons dynamically
        generateFloorButtons();

        // Setup floating action button
        setupFloatingActionButton();

        // Setup logout button
        setupLogoutButton();
    }

    private void initializeViews() {
        floorGrid = findViewById(R.id.floor_grid);
        profileCard = findViewById(R.id.profile_card);
        mainCard = findViewById(R.id.main_card);
        greetingText = findViewById(R.id.greeting_text);
        subtitleText = findViewById(R.id.subtitle_text);
        fabRefresh = findViewById(R.id.fab_refresh);
        profileImage = findViewById(R.id.profile_image);
    }

    private void startEntranceAnimations() {
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
    }

    private void generateFloorButtons() {
        // Clear any existing views
        floorGrid.removeAllViews();

        for (int i = 0; i < floorNames.length; i++) {
            final int index = i;
            CardView floorCard = createFloorCard(floorNames[i], floorDescriptions[i], floorIcons[i]);

            // Create GridLayout params for each card
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 2, 1f);
            params.rowSpec = GridLayout.spec(i / 2);

            // Set margins for spacing
            int margin = dpToPx(8);
            params.setMargins(margin, margin, margin, margin);

            floorCard.setLayoutParams(params);

            // Add staggered animation for cards
            floorCard.setAlpha(0f);
            floorCard.setTranslationY(100f);

            new Handler().postDelayed(() -> {
                ObjectAnimator alpha = ObjectAnimator.ofFloat(floorCard, "alpha", 0f, 1f);
                ObjectAnimator translation = ObjectAnimator.ofFloat(floorCard, "translationY", 100f, 0f);
                alpha.setDuration(500);
                translation.setDuration(500);
                alpha.setInterpolator(new DecelerateInterpolator());
                translation.setInterpolator(new DecelerateInterpolator());

                AnimatorSet animSet = new AnimatorSet();
                animSet.playTogether(alpha, translation);
                animSet.start();
            }, 800 + (index * 100));

            floorGrid.addView(floorCard);
        }
    }

    private CardView createFloorCard(String floorName, String description, int iconRes) {
        CardView cardView = new CardView(this);
        cardView.setCardElevation(dpToPx(6));
        cardView.setRadius(dpToPx(16));
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setUseCompatPadding(true);

        // Create linear layout for card content
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(16), dpToPx(20), dpToPx(16), dpToPx(20));
        layout.setGravity(android.view.Gravity.CENTER);

        // Create gradient background
        GradientDrawable gradient = new GradientDrawable();
        gradient.setColors(new int[]{Color.parseColor("#FFF8F3"), Color.WHITE});
        gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        gradient.setCornerRadius(dpToPx(16));
        layout.setBackground(gradient);

        // Icon
        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)));
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#fc9432"));
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        // Floor name
        TextView nameText = new TextView(this);
        nameText.setText(floorName);
        nameText.setTextSize(16);
        nameText.setTypeface(nameText.getTypeface(), Typeface.BOLD);
        nameText.setTextColor(Color.parseColor("#333333"));
        nameText.setGravity(android.view.Gravity.CENTER);
        nameText.setPadding(0, dpToPx(8), 0, dpToPx(4));

        // Description
        TextView descText = new TextView(this);
        descText.setText(description);
        descText.setTextSize(12);
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
                animateCardClick(cardView, () -> navigateToFloorMenu(floorName));
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

    private void setupFloatingActionButton() {
        fabRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rotate animation for refresh button
                ObjectAnimator rotation = ObjectAnimator.ofFloat(fabRefresh, "rotation", 0f, 360f);
                rotation.setDuration(500);
                rotation.setInterpolator(new AccelerateDecelerateInterpolator());
                rotation.start();

                // Regenerate buttons with animation
                new Handler().postDelayed(() -> {
                    generateFloorButtons();
                }, 250);
            }
        });
    }

    // UPDATED METHOD - Navigate to specific floor activity based on floor name
    private void navigateToFloorMenu(String floorName) {
        Intent intent;

        // Navigate to the appropriate activity based on floor name
        switch (floorName) {
            case "Floor 1":
                intent = new Intent(this, OwnerFloor1MenuActivity.class);
                break;
            case "Floor 2":
                intent = new Intent(this, OwnerFloor2MenuActivity.class);
                break;
            case "Floor 3":
                intent = new Intent(this, OwnerFloor3MenuActivity.class);
                break;
            case "Floor 4":
                intent = new Intent(this, OwnerFloor4MenuActivity.class);
                break;
            default:
                // Default to Floor 1 if floor name doesn't match
                intent = new Intent(this, OwnerFloor1MenuActivity.class);
                break;
        }

        // Pass floor name as extra data (optional, in case the activities need it)
        intent.putExtra("floor_name", floorName);
        startActivity(intent);

        // Add custom transition
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void setupLogoutButton() {
        Button btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(this, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Subtle profile image rotation animation
        if (profileImage != null) {
            ObjectAnimator profileRotation = ObjectAnimator.ofFloat(profileImage, "rotation", 0f, 5f, -5f, 0f);
            profileRotation.setDuration(2000);
            profileRotation.setRepeatCount(ObjectAnimator.INFINITE);
            profileRotation.setRepeatMode(ObjectAnimator.REVERSE);
            profileRotation.start();
        }
    }
}