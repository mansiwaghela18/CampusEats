package com.example.shashankscreen;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class StudentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StudentDashboard";

    private GridLayout floorGrid;
    private CardView profileCard, mainCard;
    private TextView greetingText, subtitleText, drawerStudentName, drawerStudentDetails;
    private FloatingActionButton fabRefresh;
    private ImageView profileImage, drawerProfileImage, menuButton;
    private View overlay;
    private SharedPreferences sharedPreferences;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private boolean isDrawerOpen = false;



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
        try {
            setContentView(R.layout.activity_student_dashboard);

            // Initialize SharedPreferences
            sharedPreferences = getSharedPreferences("StudentProfile", MODE_PRIVATE);

            // Initialize views
            if (!initializeViews()) {
                Log.e(TAG, "Failed to initialize views");
                return;
            }

            // Load profile data
            loadProfileData();

            // Setup navigation drawer
            setupNavigationDrawer();

            // Start entrance animations
            startEntranceAnimations();

            // Generate floor buttons dynamically
            new Handler().postDelayed(() -> {
                generateFloorButtons();
            }, 100);

            // Setup floating action button
            setupFloatingActionButton();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    // Add this method to your StudentDashboardActivity class

    private boolean initializeViews() {
        try {
            floorGrid = findViewById(R.id.floor_grid);
            profileCard = findViewById(R.id.profile_card);
            mainCard = findViewById(R.id.main_card);
            greetingText = findViewById(R.id.greeting_text);
            subtitleText = findViewById(R.id.subtitle_text);
            fabRefresh = findViewById(R.id.fab_refresh);
            profileImage = findViewById(R.id.profile_image);
            menuButton = findViewById(R.id.menu_button);

            // Navigation drawer elements
            drawerLayout = findViewById(R.id.side_drawer);
            navigationView = findViewById(R.id.navigation_view);
            overlay = findViewById(R.id.overlay);

            // Get header view from NavigationView and find views within it
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                drawerStudentName = headerView.findViewById(R.id.drawer_student_name);
                drawerStudentDetails = headerView.findViewById(R.id.drawer_student_details);
                drawerProfileImage = headerView.findViewById(R.id.drawer_profile_image);
            }

            // Check if any critical views are null
            if (floorGrid == null) {
                Log.e(TAG, "floor_grid not found in layout");
                return false;
            }
            if (greetingText == null) {
                Log.e(TAG, "greeting_text not found in layout");
                return false;
            }
            if (mainCard == null) {
                Log.e(TAG, "main_card not found in layout");
                return false;
            }
            if (drawerLayout == null) {
                Log.e(TAG, "side_drawer not found in layout");
                return false;
            }
            if (navigationView == null) {
                Log.e(TAG, "navigation_view not found in layout");
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            return false;
        }
    }

    private void loadProfileData() {
        try {
            String studentName = sharedPreferences.getString("student_name", "Student");
            String section = sharedPreferences.getString("section", "");
            String branch = sharedPreferences.getString("branch", "");

            // Update greeting text with better formatting
            if (greetingText != null) {
                if (!studentName.equals("Student")) {
                    greetingText.setText("Welcome Back, " + studentName + "!");
                } else {
                    greetingText.setText("Welcome Back!");
                }
            }

            // Update drawer information with improved text
            if (drawerStudentName != null) {
                drawerStudentName.setText(studentName);
            }

            if (drawerStudentDetails != null) {
                if (!section.isEmpty() && !branch.isEmpty()) {
                    drawerStudentDetails.setText(section + " • " + branch);
                } else {
                    drawerStudentDetails.setText("Complete your profile");
                    // Add a subtle color change to indicate incomplete profile
                    drawerStudentDetails.setTextColor(Color.parseColor("#FF9800"));
                }
            }

            // Load profile image path if exists
            String imagePath = sharedPreferences.getString("profile_image", "");
            if (!imagePath.isEmpty()) {
                // You can implement image loading from path here
                // For now, keeping default image
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading profile data: " + e.getMessage(), e);
        }
    }

    private void setupNavigationDrawer() {
        try {
            // Set drawer width to 85% of screen width for better proportions
            if (navigationView != null) {
                ViewGroup.LayoutParams params = navigationView.getLayoutParams();
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                params.width = (int) (screenWidth * 0.85f);
                navigationView.setLayoutParams(params);

                // Set navigation item selected listener
                navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem item) {
                        int id = item.getItemId();

                        if (id == R.id.menu_profile) {
                            // Handle profile menu item click
                            animateMenuItemClick(item, () -> {
                                if (drawerLayout != null) {
                                    drawerLayout.closeDrawer(navigationView);
                                }
                                // Navigate to profile activity
                                new Handler().postDelayed(() -> {
                                    try {
                                        Intent intent = new Intent(StudentDashboardActivity.this, ProfileActivity.class);
                                        startActivity(intent);
                                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error navigating to ProfileActivity: " + e.getMessage(), e);
                                    }
                                }, 250);
                            });
                            return true;

                        } else if (id == R.id.menu_logout) {
                            // Handle logout menu item click
                            animateMenuItemClick(item, () -> {
                                if (drawerLayout != null) {
                                    drawerLayout.closeDrawer(navigationView);
                                }
                                new Handler().postDelayed(() -> {
                                    performLogout();
                                }, 250);
                            });
                            return true;
                        }

                        return false;
                    }
                });
            }

            // Menu button click listener with animation
            if (menuButton != null) {
                menuButton.setOnClickListener(v -> {
                    // Add rotation animation to menu button
                    ObjectAnimator rotation = ObjectAnimator.ofFloat(menuButton, "rotation", 0f, 180f);
                    rotation.setDuration(300);
                    rotation.start();

                    if (drawerLayout != null) {
                        if (drawerLayout.isDrawerOpen(navigationView)) {
                            drawerLayout.closeDrawer(navigationView);
                        } else {
                            drawerLayout.openDrawer(navigationView);
                        }
                    }
                });
            }

            // Add drawer state listener for menu button animation
            if (drawerLayout != null) {
                drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {
                        // Optional: Add slide animation to main content
                    }

                    @Override
                    public void onDrawerOpened(View drawerView) {
                        isDrawerOpen = true;
                        // Add subtle animation to drawer profile image
                        if (drawerProfileImage != null) {
                            ObjectAnimator scale = ObjectAnimator.ofFloat(drawerProfileImage, "scaleX", 0.9f, 1f);
                            ObjectAnimator scaleY = ObjectAnimator.ofFloat(drawerProfileImage, "scaleY", 0.9f, 1f);
                            scale.setDuration(200);
                            scaleY.setDuration(200);
                            AnimatorSet animSet = new AnimatorSet();
                            animSet.playTogether(scale, scaleY);
                            animSet.start();
                        }
                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {
                        isDrawerOpen = false;
                        // Reset menu button rotation
                        if (menuButton != null) {
                            ObjectAnimator rotation = ObjectAnimator.ofFloat(menuButton, "rotation", 180f, 0f);
                            rotation.setDuration(300);
                            rotation.start();
                        }
                    }

                    @Override
                    public void onDrawerStateChanged(int newState) {}
                });
            }

            // Handle logout button in main layout (if exists)
            View btnLogout = findViewById(R.id.btnLogout);
            if (btnLogout != null) {
                btnLogout.setOnClickListener(v -> performLogout());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation drawer: " + e.getMessage(), e);
        }
    }

    private void animateMenuItemClick(MenuItem item, Runnable onComplete) {
        try {
            // Since we can't directly animate MenuItem, we'll just run the completion
            if (onComplete != null) {
                onComplete.run();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error animating menu item click: " + e.getMessage(), e);
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private void performLogout() {
        try {
            // Show a subtle logout animation
            if (drawerLayout != null) {
                ObjectAnimator fadeOut = ObjectAnimator.ofFloat(drawerLayout, "alpha", 1f, 0f);
                fadeOut.setDuration(300);
                fadeOut.start();
            }

            // Clear all profile data from SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            // Navigate to login activity with delay for smooth animation
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 300);

        } catch (Exception e) {
            Log.e(TAG, "Error performing logout: " + e.getMessage(), e);
        }
    }

    private void startEntranceAnimations() {
        try {
            // Profile animation with improved bounce
            if (profileCard != null) {
                ObjectAnimator profileScale = ObjectAnimator.ofFloat(profileCard, "scaleX", 0f, 1.1f, 1f);
                ObjectAnimator profileScaleY = ObjectAnimator.ofFloat(profileCard, "scaleY", 0f, 1.1f, 1f);
                ObjectAnimator profileAlpha = ObjectAnimator.ofFloat(profileCard, "alpha", 0f, 1f);

                profileScale.setDuration(800);
                profileScaleY.setDuration(800);
                profileAlpha.setDuration(600);

                profileScale.setInterpolator(new BounceInterpolator());
                profileScaleY.setInterpolator(new BounceInterpolator());

                AnimatorSet profileAnimSet = new AnimatorSet();
                profileAnimSet.playTogether(profileScale, profileScaleY, profileAlpha);
                profileAnimSet.start();
            }

            // Menu button animation with improved timing
            if (menuButton != null) {
                ObjectAnimator menuAlpha = ObjectAnimator.ofFloat(menuButton, "alpha", 0f, 1f);
                ObjectAnimator menuScale = ObjectAnimator.ofFloat(menuButton, "scaleX", 0f, 1.2f, 1f);
                ObjectAnimator menuScaleY = ObjectAnimator.ofFloat(menuButton, "scaleY", 0f, 1.2f, 1f);

                menuAlpha.setDuration(500);
                menuScale.setDuration(600);
                menuScaleY.setDuration(600);
                menuScale.setInterpolator(new BounceInterpolator());
                menuScaleY.setInterpolator(new BounceInterpolator());

                AnimatorSet menuAnimSet = new AnimatorSet();
                menuAnimSet.playTogether(menuAlpha, menuScale, menuScaleY);
                menuAnimSet.setStartDelay(150);
                menuAnimSet.start();
            }

            // Greeting text slide and fade in with improved timing
            new Handler().postDelayed(() -> {
                if (greetingText != null) {
                    ObjectAnimator greetingAlpha = ObjectAnimator.ofFloat(greetingText, "alpha", 0f, 1f);
                    ObjectAnimator greetingTranslation = ObjectAnimator.ofFloat(greetingText, "translationY", 30f, 0f);
                    ObjectAnimator greetingScale = ObjectAnimator.ofFloat(greetingText, "scaleX", 0.9f, 1f);

                    greetingAlpha.setDuration(600);
                    greetingTranslation.setDuration(600);
                    greetingScale.setDuration(600);

                    greetingAlpha.setInterpolator(new DecelerateInterpolator());
                    greetingTranslation.setInterpolator(new DecelerateInterpolator());

                    AnimatorSet greetingAnimSet = new AnimatorSet();
                    greetingAnimSet.playTogether(greetingAlpha, greetingTranslation, greetingScale);
                    greetingAnimSet.start();
                }
            }, 300);

            // Subtitle animation with stagger
            new Handler().postDelayed(() -> {
                if (subtitleText != null) {
                    ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(subtitleText, "alpha", 0f, 1f);
                    ObjectAnimator subtitleTranslation = ObjectAnimator.ofFloat(subtitleText, "translationY", 20f, 0f);

                    subtitleAlpha.setDuration(500);
                    subtitleTranslation.setDuration(500);

                    AnimatorSet subtitleAnimSet = new AnimatorSet();
                    subtitleAnimSet.playTogether(subtitleAlpha, subtitleTranslation);
                    subtitleAnimSet.start();
                }
            }, 500);

            // Main card slide up animation with improved easing
            new Handler().postDelayed(() -> {
                if (mainCard != null) {
                    ObjectAnimator cardAlpha = ObjectAnimator.ofFloat(mainCard, "alpha", 0f, 1f);
                    ObjectAnimator cardTranslation = ObjectAnimator.ofFloat(mainCard, "translationY", 150f, 0f);
                    ObjectAnimator cardScale = ObjectAnimator.ofFloat(mainCard, "scaleY", 0.8f, 1f);

                    cardAlpha.setDuration(700);
                    cardTranslation.setDuration(700);
                    cardScale.setDuration(700);

                    cardAlpha.setInterpolator(new DecelerateInterpolator());
                    cardTranslation.setInterpolator(new DecelerateInterpolator());
                    cardScale.setInterpolator(new DecelerateInterpolator());

                    AnimatorSet cardAnimSet = new AnimatorSet();
                    cardAnimSet.playTogether(cardAlpha, cardTranslation, cardScale);
                    cardAnimSet.start();
                }
            }, 700);

            // FAB animation with improved bounce
            new Handler().postDelayed(() -> {
                if (fabRefresh != null) {
                    ObjectAnimator fabScale = ObjectAnimator.ofFloat(fabRefresh, "scaleX", 0f, 1.3f, 1f);
                    ObjectAnimator fabScaleY = ObjectAnimator.ofFloat(fabRefresh, "scaleY", 0f, 1.3f, 1f);
                    ObjectAnimator fabAlpha = ObjectAnimator.ofFloat(fabRefresh, "alpha", 0f, 1f);

                    fabScale.setDuration(500);
                    fabScaleY.setDuration(500);
                    fabAlpha.setDuration(400);

                    fabScale.setInterpolator(new BounceInterpolator());
                    fabScaleY.setInterpolator(new BounceInterpolator());

                    AnimatorSet fabAnimSet = new AnimatorSet();
                    fabAnimSet.playTogether(fabScale, fabScaleY, fabAlpha);
                    fabAnimSet.start();
                }
            }, 1200);
        } catch (Exception e) {
            Log.e(TAG, "Error in entrance animations: " + e.getMessage(), e);
        }
    }

    private void generateFloorButtons() {
        try {
            if (floorGrid == null) {
                Log.e(TAG, "floorGrid is null");
                return;
            }

            // Clear any existing views
            floorGrid.removeAllViews();

            Log.d(TAG, "Generating " + floorNames.length + " floor buttons");

            for (int i = 0; i < floorNames.length; i++) {
                final int index = i;
                CardView floorCard = createFloorCard(floorNames[i], floorDescriptions[i], floorIcons[i]);

                if (floorCard == null) {
                    Log.e(TAG, "Failed to create floor card for index: " + i);
                    continue;
                }

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(i % 2, 1f);
                params.rowSpec = GridLayout.spec(i / 2);

                // Improved margins for better spacing
                int margin = dpToPx(6);
                params.setMargins(margin, margin, margin, margin);

                floorCard.setLayoutParams(params);

                // Enhanced staggered animation for cards
                floorCard.setAlpha(0f);
                floorCard.setTranslationY(80f);
                floorCard.setScaleX(0.8f);
                floorCard.setScaleY(0.8f);

                new Handler().postDelayed(() -> {
                    if (floorCard != null && !this.isDestroyed() && !this.isFinishing()) {
                        ObjectAnimator alpha = ObjectAnimator.ofFloat(floorCard, "alpha", 0f, 1f);
                        ObjectAnimator translation = ObjectAnimator.ofFloat(floorCard, "translationY", 80f, 0f);
                        ObjectAnimator scaleX = ObjectAnimator.ofFloat(floorCard, "scaleX", 0.8f, 1.05f, 1f);
                        ObjectAnimator scaleY = ObjectAnimator.ofFloat(floorCard, "scaleY", 0.8f, 1.05f, 1f);

                        alpha.setDuration(600);
                        translation.setDuration(600);
                        scaleX.setDuration(600);
                        scaleY.setDuration(600);

                        alpha.setInterpolator(new DecelerateInterpolator());
                        translation.setInterpolator(new DecelerateInterpolator());
                        scaleX.setInterpolator(new DecelerateInterpolator());
                        scaleY.setInterpolator(new DecelerateInterpolator());

                        AnimatorSet animSet = new AnimatorSet();
                        animSet.playTogether(alpha, translation, scaleX, scaleY);
                        animSet.start();
                    }
                }, 900 + (index * 150));

                floorGrid.addView(floorCard);
                Log.d(TAG, "Added floor card " + (i + 1) + " to grid");
            }

            // Force layout refresh
            floorGrid.requestLayout();
            Log.d(TAG, "Floor grid layout updated");

        } catch (Exception e) {
            Log.e(TAG, "Error generating floor buttons: " + e.getMessage(), e);
        }
    }


    private CardView createFloorCard(String floorName, String description, int iconRes) {
        try {
            CardView cardView = new CardView(this);
            cardView.setCardElevation(dpToPx(8));
            cardView.setRadius(dpToPx(20));
            cardView.setCardBackgroundColor(Color.WHITE);
            cardView.setUseCompatPadding(true);

            // Create linear layout for card content
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dpToPx(18), dpToPx(24), dpToPx(18), dpToPx(24));
            layout.setGravity(android.view.Gravity.CENTER);

            // Create enhanced gradient background
            GradientDrawable gradient = new GradientDrawable();
            gradient.setColors(new int[]{
                    Color.parseColor("#FFF8F3"),
                    Color.parseColor("#FFEFDB"),
                    Color.WHITE
            });
            gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            gradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
            gradient.setCornerRadius(dpToPx(20));
            layout.setBackground(gradient);

            // Create and setup the icon with improved styling
            ImageView icon = new ImageView(this);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(52), dpToPx(52));
            iconParams.setMargins(0, 0, 0, dpToPx(12));
            icon.setLayoutParams(iconParams);
            icon.setImageResource(iconRes);
            icon.setColorFilter(Color.parseColor("#FF6B35"));
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            // Enhanced floor name text
            TextView nameText = new TextView(this);
            nameText.setText(floorName);
            nameText.setTextSize(18);
            nameText.setTypeface(nameText.getTypeface(), Typeface.BOLD);
            nameText.setTextColor(Color.parseColor("#2C2C2C"));
            nameText.setGravity(android.view.Gravity.CENTER);
            nameText.setPadding(0, 0, 0, dpToPx(6));

            // Enhanced description text
            TextView descText = new TextView(this);
            descText.setText(description);
            descText.setTextSize(13);
            descText.setTextColor(Color.parseColor("#757575"));
            descText.setGravity(android.view.Gravity.CENTER);
            descText.setTypeface(descText.getTypeface(), Typeface.NORMAL);

            // Add views to layout
            layout.addView(icon);
            layout.addView(nameText);
            layout.addView(descText);
            cardView.addView(layout);

            // Improved click animation and listener
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    animateCardClickImproved(cardView, () -> navigateToFloorMenu(floorName));
                }
            });

            Log.d(TAG, "Successfully created floor card for: " + floorName);
            return cardView;

        } catch (Exception e) {
            Log.e(TAG, "Error creating floor card for " + floorName + ": " + e.getMessage(), e);
            return null;
        }
    }

    private void animateCardClickImproved(View view, Runnable onComplete) {
        try {
            // Enhanced click animation with multiple properties
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.92f, 1.05f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.92f, 1.05f, 1f);
            ObjectAnimator elevation = ObjectAnimator.ofFloat(view, "elevation", dpToPx(8), dpToPx(16), dpToPx(8));

            scaleX.setDuration(300);
            scaleY.setDuration(300);
            elevation.setDuration(300);

            scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(scaleX, scaleY, elevation);
            animSet.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            animSet.start();
        } catch (Exception e) {
            Log.e(TAG, "Error animating card click: " + e.getMessage(), e);
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private void animateCardClick(View view, Runnable onComplete) {
        // Keep the original method for backward compatibility
        animateCardClickImproved(view, onComplete);
    }

    private void setupFloatingActionButton() {
        try {
            if (fabRefresh != null) {
                fabRefresh.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Enhanced rotation animation for refresh button
                        ObjectAnimator rotation = ObjectAnimator.ofFloat(fabRefresh, "rotation", 0f, 360f);
                        ObjectAnimator scale = ObjectAnimator.ofFloat(fabRefresh, "scaleX", 1f, 1.1f, 1f);
                        ObjectAnimator scaleY = ObjectAnimator.ofFloat(fabRefresh, "scaleY", 1f, 1.1f, 1f);

                        rotation.setDuration(600);
                        scale.setDuration(300);
                        scaleY.setDuration(300);

                        rotation.setInterpolator(new AccelerateDecelerateInterpolator());

                        AnimatorSet animSet = new AnimatorSet();
                        animSet.playTogether(rotation, scale, scaleY);
                        animSet.start();

                        // Regenerate buttons with animation and haptic feedback
                        new Handler().postDelayed(() -> {
                            generateFloorButtons();
                        }, 300);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up FAB: " + e.getMessage(), e);
        }
    }

    private void navigateToFloorMenu(String floorName) {
        try {
            Intent intent;

            // Navigate to the appropriate activity based on floor name
            switch (floorName) {
                case "Floor 1":
                    intent = new Intent(this, Floor1MenuActivity.class);
                    break;
                case "Floor 2":
                    intent = new Intent(this, Floor2MenuActivity.class);
                    break;
                case "Floor 3":
                    intent = new Intent(this, Floor3MenuActivity.class);
                    break;
                case "Floor 4":
                    intent = new Intent(this, Floor4MenuActivity.class);
                    break;
                default:
                    intent = new Intent(this, Floor1MenuActivity.class);
                    break;
            }

            // Pass floor name as extra data
            intent.putExtra("floor_name", floorName);
            startActivity(intent);

            // Add custom transition
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to floor menu: " + e.getMessage(), e);
        }
    }


    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Reload profile data when returning from profile activity
            loadProfileData();

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
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }
}