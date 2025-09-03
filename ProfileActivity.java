package com.example.shashankscreen;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputEditText;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private static final int GALLERY_REQUEST_CODE = 100;

    private ImageView backButton, profileImage;
    private TextInputEditText etName, etSection, etBranch, etGrNumber;
    private AppCompatButton btnChangePhoto, btnSave;
    private CardView mainCard;
    private SharedPreferences sharedPreferences;
    private String profileImagePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("StudentProfile", MODE_PRIVATE);

        // Initialize views
        initializeViews();

        // Load existing profile data
        loadProfileData();

        // Setup click listeners
        setupClickListeners();

        // Start entrance animations
        startEntranceAnimations();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        profileImage = findViewById(R.id.profile_image);
        etName = findViewById(R.id.et_name);
        etSection = findViewById(R.id.et_section);
        etBranch = findViewById(R.id.et_branch);
        etGrNumber = findViewById(R.id.et_gr_number);
        btnChangePhoto = findViewById(R.id.btn_change_photo);
        btnSave = findViewById(R.id.btn_save);
        mainCard = findViewById(R.id.main_card);
    }

    private void loadProfileData() {
        // Load saved data from SharedPreferences
        String name = sharedPreferences.getString("student_name", "");
        String section = sharedPreferences.getString("section", "");
        String branch = sharedPreferences.getString("branch", "");
        String grNumber = sharedPreferences.getString("gr_number", "");
        profileImagePath = sharedPreferences.getString("profile_image", "");

        // Set data to EditTexts
        etName.setText(name);
        etSection.setText(section);
        etBranch.setText(branch);
        etGrNumber.setText(grNumber);

        // Load profile image if exists
        if (!profileImagePath.isEmpty()) {
            loadImageFromPath(profileImagePath);
        }
    }

    private void setupClickListeners() {
        // Back button click
        backButton.setOnClickListener(v -> {
            animateButtonClick(backButton, this::onBackPressed);
        });

        // Change photo button click
        btnChangePhoto.setOnClickListener(v -> {
            animateButtonClick(btnChangePhoto, this::openGallery);
        });

        // Save button click
        btnSave.setOnClickListener(v -> {
            animateButtonClick(btnSave, this::saveProfile);
        });
    }

    private void animateButtonClick(View view, Runnable action) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f);
        scaleX.setDuration(150);
        scaleY.setDuration(150);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (action != null) {
                    action.run();
                }
            }
        });
        animSet.start();
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    // Load and display the selected image
                    InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    // Scale bitmap to reduce size
                    Bitmap scaledBitmap = scaleBitmap(bitmap, 400, 400);

                    // Save image to internal storage
                    profileImagePath = saveImageToInternalStorage(scaledBitmap);

                    // Display the image
                    profileImage.setImageBitmap(scaledBitmap);

                    // Add a subtle animation to the profile image
                    animateProfileImage();

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private String saveImageToInternalStorage(Bitmap bitmap) {
        try {
            String filename = "profile_image_" + System.currentTimeMillis() + ".png";
            FileOutputStream fos = openFileOutput(filename, MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
            return getFilesDir().getAbsolutePath() + "/" + filename;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void loadImageFromPath(String imagePath) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                profileImage.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // If loading fails, keep default image
        }
    }

    private void animateProfileImage() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(profileImage, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(profileImage, "scaleY", 1f, 1.1f, 1f);
        scaleX.setDuration(300);
        scaleY.setDuration(300);
        scaleX.setInterpolator(new BounceInterpolator());
        scaleY.setInterpolator(new BounceInterpolator());

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.start();
    }

    private void saveProfile() {
        // Get input values
        String name = etName.getText().toString().trim();
        String section = etSection.getText().toString().trim();
        String branch = etBranch.getText().toString().trim();
        String grNumber = etGrNumber.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(section)) {
            etSection.setError("Section is required");
            etSection.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(branch)) {
            etBranch.setError("Branch is required");
            etBranch.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(grNumber)) {
            etGrNumber.setError("GR Number is required");
            etGrNumber.requestFocus();
            return;
        }

        // Save to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("student_name", name);
        editor.putString("section", section);
        editor.putString("branch", branch);
        editor.putString("gr_number", grNumber);
        editor.putString("profile_image", profileImagePath);
        editor.apply();

        // Show success message with animation
        showSuccessAnimation();

        // Navigate back to dashboard after a short delay
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, StudentDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        }, 1500);
    }

    private void showSuccessAnimation() {
        // Disable save button to prevent multiple clicks
        btnSave.setEnabled(false);
        btnSave.setText("Saved!");

        // Success animation for save button
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(btnSave, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(btnSave, "scaleY", 1f, 1.2f, 1f);
        scaleX.setDuration(600);
        scaleY.setDuration(600);
        scaleX.setInterpolator(new BounceInterpolator());
        scaleY.setInterpolator(new BounceInterpolator());

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.start();

        // Show toast
        Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
    }

    private void startEntranceAnimations() {
        // Main card slide up animation
        mainCard.setAlpha(0f);
        mainCard.setTranslationY(200f);

        ObjectAnimator cardAlpha = ObjectAnimator.ofFloat(mainCard, "alpha", 0f, 1f);
        ObjectAnimator cardTranslation = ObjectAnimator.ofFloat(mainCard, "translationY", 200f, 0f);
        cardAlpha.setDuration(600);
        cardTranslation.setDuration(600);
        cardAlpha.setInterpolator(new DecelerateInterpolator());
        cardTranslation.setInterpolator(new DecelerateInterpolator());

        AnimatorSet cardAnimSet = new AnimatorSet();
        cardAnimSet.playTogether(cardAlpha, cardTranslation);
        cardAnimSet.start();

        // Profile image animation
        new Handler().postDelayed(() -> {
            ObjectAnimator profileScale = ObjectAnimator.ofFloat(profileImage, "scaleX", 0f, 1.2f, 1f);
            ObjectAnimator profileScaleY = ObjectAnimator.ofFloat(profileImage, "scaleY", 0f, 1.2f, 1f);
            profileScale.setDuration(700);
            profileScaleY.setDuration(700);
            profileScale.setInterpolator(new BounceInterpolator());
            profileScaleY.setInterpolator(new BounceInterpolator());

            AnimatorSet profileAnimSet = new AnimatorSet();
            profileAnimSet.playTogether(profileScale, profileScaleY);
            profileAnimSet.start();
        }, 200);

        // Animate buttons
        new Handler().postDelayed(() -> {
            animateButtonEntrance(btnChangePhoto, 300);
            animateButtonEntrance(btnSave, 500);
        }, 400);
    }

    private void animateButtonEntrance(View button, int delay) {
        button.setAlpha(0f);
        button.setTranslationY(50f);

        new Handler().postDelayed(() -> {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(button, "alpha", 0f, 1f);
            ObjectAnimator translation = ObjectAnimator.ofFloat(button, "translationY", 50f, 0f);
            alpha.setDuration(400);
            translation.setDuration(400);
            alpha.setInterpolator(new DecelerateInterpolator());
            translation.setInterpolator(new DecelerateInterpolator());

            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(alpha, translation);
            animSet.start();
        }, delay);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}