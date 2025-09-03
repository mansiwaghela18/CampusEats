package com.example.shashankscreen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

// Add these Firebase imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";

    private EditText etEmail, etPassword, etConfirmPassword;
    private AppCompatButton btnRegister;
    private TextView tvAlreadyHaveAccount;
    private ProgressBar progressBar;

    // Add Firebase Auth instance
    private FirebaseAuth mAuth;

    // SharedPreferences for login state - same as LoginActivity
    private SharedPreferences loginPrefs;
    private static final String LOGIN_PREFS = "LoginPrefs";
    private static final String IS_LOGGED_IN = "isLoggedIn";
    private static final String USER_TYPE = "userType";
    private static final String USER_EMAIL = "userEmail";
    private static final String USER_FLOOR = "userFloor";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        try {
            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance();
            Log.d(TAG, "Firebase Auth initialized successfully");

            // Initialize SharedPreferences
            loginPrefs = getSharedPreferences(LOGIN_PREFS, MODE_PRIVATE);

            // Check if user is already logged in
            if (checkIfAlreadyLoggedIn()) {
                return; // User is already logged in, navigated to dashboard
            }

            // Initialize views
            initializeViews();

            // Set up click listeners
            setupClickListeners();

            // Debug: Check if there's already a logged-in user
            checkCurrentUser();

            Log.d(TAG, "RegistrationActivity initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate(): " + e.getMessage(), e);
            showToast("Error initializing registration: " + e.getMessage());
        }
    }

    /**
     * Check if user is already logged in - redirect if so
     */
    private boolean checkIfAlreadyLoggedIn() {
        try {
            boolean isLoggedInPrefs = loginPrefs.getBoolean(IS_LOGGED_IN, false);
            FirebaseUser currentUser = mAuth.getCurrentUser();
            String savedUserType = loginPrefs.getString(USER_TYPE, "");
            String savedUserEmail = loginPrefs.getString(USER_EMAIL, "");
            String savedUserFloor = loginPrefs.getString(USER_FLOOR, "");

            // Check if both sources indicate user is logged in
            if (isLoggedInPrefs && currentUser != null && !savedUserType.isEmpty() && !savedUserEmail.isEmpty()) {
                // Verify the saved email matches the Firebase user
                if (savedUserEmail.equals(currentUser.getEmail())) {
                    Log.d(TAG, "User already logged in, redirecting from registration page");

                    // Navigate to appropriate dashboard based on user type
                    if ("Owner".equals(savedUserType)) {
                        navigateToOwnerDashboard(savedUserEmail, savedUserFloor);
                        return true;
                    } else if ("Student/Faculty".equals(savedUserType)) {
                        navigateToStudentDashboard();
                        return true;
                    }
                } else {
                    // Mismatch, clear the state
                    Log.w(TAG, "Email mismatch in saved state, clearing login state");
                    clearLoginState();
                }
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error checking login status in registration: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Save login state after successful registration
     */
    private void saveLoginState(String userType, String email) {
        try {
            SharedPreferences.Editor editor = loginPrefs.edit();
            editor.putBoolean(IS_LOGGED_IN, true);
            editor.putString(USER_TYPE, userType);
            editor.putString(USER_EMAIL, email);
            editor.putString(USER_FLOOR, ""); // Student/Faculty don't have floors

            // Use commit() for immediate write
            boolean success = editor.commit();

            if (success) {
                Log.d(TAG, "Login state saved after registration:");
                Log.d(TAG, "- User Type: " + userType);
                Log.d(TAG, "- Email: " + email);
            } else {
                Log.e(TAG, "Failed to save login state after registration");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving login state: " + e.getMessage(), e);
        }
    }

    /**
     * Clear login state
     */
    private void clearLoginState() {
        try {
            SharedPreferences.Editor editor = loginPrefs.edit();
            editor.clear();
            editor.commit();
            mAuth.signOut();
            Log.d(TAG, "Login state cleared from registration activity");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing login state: " + e.getMessage(), e);
        }
    }

    private void initializeViews() {
        try {
            etEmail = findViewById(R.id.etUsername); // Using existing ID from layout
            etPassword = findViewById(R.id.etPassword);
            etConfirmPassword = findViewById(R.id.etConfirmPassword);
            btnRegister = findViewById(R.id.btnRegister);
            tvAlreadyHaveAccount = findViewById(R.id.tvAlreadyHaveAccount);
            progressBar = findViewById(R.id.progressBar);

            Log.d(TAG, "Views initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            throw e;
        }
    }

    private void setupClickListeners() {
        try {
            // Register button click
            btnRegister.setOnClickListener(v -> registerUser());

            // Already have account click
            tvAlreadyHaveAccount.setOnClickListener(v -> {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });

            Log.d(TAG, "Click listeners set up successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void registerUser() {
        try {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            Log.d(TAG, "Registration attempt for email: " + email);

            // Validate input
            if (!validateInput(email, password, confirmPassword)) {
                return;
            }

            // Show loading state
            setLoadingState(true);

            // Create user with Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        // Reset loading state
                        setLoadingState(false);

                        if (task.isSuccessful()) {
                            // Registration success
                            Log.d("FirebaseAuth", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Debug logging
                            if (user != null) {
                                Log.d("FirebaseDebug", "New User Created: " + user.getUid() + ", " + user.getEmail());
                                Log.d("FirebaseDebug", "User created: " + user.getMetadata().getCreationTimestamp());
                                Log.d("FirebaseDebug", "Last sign in: " + user.getMetadata().getLastSignInTimestamp());
                            }

                            showToast("Registration successful!");

                            // OPTION 1: Auto-login user after registration (Recommended)
                            // Save login state for student (default for registration)
                            saveLoginState("Student/Faculty", email);

                            // Navigate directly to student dashboard
                            navigateToStudentDashboard();

                            // OPTION 2: Go to login page (uncomment if you prefer this approach)
                            /*
                            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();
                            */

                        } else {
                            // Registration failed
                            Log.w("FirebaseAuth", "createUserWithEmail:failure", task.getException());
                            handleRegistrationError(task.getException());
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in registerUser(): " + e.getMessage(), e);
            setLoadingState(false);
            showToast("Registration error: " + e.getMessage());
        }
    }

    private void handleRegistrationError(Exception exception) {
        String errorMessage = "Registration failed.";

        if (exception != null) {
            String exceptionMessage = exception.getMessage();
            if (exceptionMessage != null) {
                if (exceptionMessage.contains("email address is already in use")) {
                    errorMessage = "This email is already registered. Please use a different email or try logging in.";
                } else if (exceptionMessage.contains("weak password")) {
                    errorMessage = "Password is too weak. Please choose a stronger password.";
                } else if (exceptionMessage.contains("malformed email")) {
                    errorMessage = "Please enter a valid email address.";
                } else {
                    errorMessage = "Registration failed: " + exceptionMessage;
                }
            }
        }

        showToast(errorMessage);
        Log.e("FirebaseDebug", "Registration error: " + errorMessage);
    }

    /**
     * Navigate to Student Dashboard after successful registration
     */
    private void navigateToStudentDashboard() {
        try {
            Log.d(TAG, "Attempting to navigate to StudentDashboard after registration");

            // Check if the activity exists
            try {
                Class<?> studentDashboardClass = Class.forName("com.example.shashankscreen.StudentDashboardActivity");
                Log.d(TAG, "StudentDashboardActivity class found");

                Intent intent = new Intent(RegistrationActivity.this, studentDashboardClass);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                Log.d(TAG, "Navigation to StudentDashboard completed");

            } catch (ClassNotFoundException e) {
                Log.e(TAG, "StudentDashboardActivity class not found: " + e.getMessage(), e);
                // Fallback: go to login page with email pre-filled
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                intent.putExtra("email", etEmail.getText().toString().trim());
                startActivity(intent);
                finish();
                showToast("Registration successful! Please login to continue.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to StudentDashboard: " + e.getMessage(), e);
            // Fallback: go to login page
            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
            intent.putExtra("email", etEmail.getText().toString().trim());
            startActivity(intent);
            finish();
            showToast("Registration successful! Please login to continue.");
        }
    }

    /**
     * Navigate to Owner Dashboard (in case an owner registers - though unlikely)
     */
    private void navigateToOwnerDashboard(String email, String floor) {
        try {
            Log.d(TAG, "Attempting to navigate to Owner Dashboard for " + floor);

            Intent intent;

            // Navigate to appropriate dashboard based on floor
            if ("Admin".equals(floor)) {
                intent = new Intent(RegistrationActivity.this, OwnerDashboardActivity.class);
            } else {
                switch (floor) {
                    case "Floor 1":
                        intent = new Intent(RegistrationActivity.this, Floor1DashboardActivity.class);
                        break;
                    case "Floor 2":
                        intent = new Intent(RegistrationActivity.this, Floor2DashboardActivity.class);
                        break;
                    case "Floor 3":
                        intent = new Intent(RegistrationActivity.this, Floor3DashboardActivity.class);
                        break;
                    case "Floor 4":
                        intent = new Intent(RegistrationActivity.this, Floor4DashboardActivity.class);
                        break;
                    default:
                        intent = new Intent(RegistrationActivity.this, OwnerDashboardActivity.class);
                        break;
                }
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("floor_name", floor);
            intent.putExtra("user_email", email);

            startActivity(intent);
            finish();
            Log.d(TAG, "Navigation to " + floor + " Dashboard completed");

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to Owner Dashboard: " + e.getMessage(), e);
            // Fallback to login
            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
            intent.putExtra("email", email);
            startActivity(intent);
            finish();
        }
    }

    private boolean validateInput(String email, String password, String confirmPassword) {
        try {
            // Reset errors
            etEmail.setError(null);
            etPassword.setError(null);
            etConfirmPassword.setError(null);

            // Validate email
            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return false;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Please enter a valid email address");
                etEmail.requestFocus();
                return false;
            }

            // Validate password
            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                etPassword.requestFocus();
                return false;
            }

            if (password.length() < 6) {
                etPassword.setError("Password must be at least 6 characters");
                etPassword.requestFocus();
                return false;
            }

            // Validate confirm password
            if (confirmPassword.isEmpty()) {
                etConfirmPassword.setError("Please confirm your password");
                etConfirmPassword.requestFocus();
                return false;
            }

            if (!password.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                etConfirmPassword.requestFocus();
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error in validateInput(): " + e.getMessage(), e);
            return false;
        }
    }

    private void setLoadingState(boolean isLoading) {
        try {
            btnRegister.setEnabled(!isLoading);
            etEmail.setEnabled(!isLoading);
            etPassword.setEnabled(!isLoading);
            etConfirmPassword.setEnabled(!isLoading);

            if (isLoading) {
                btnRegister.setText("CREATING ACCOUNT...");
                progressBar.setVisibility(View.VISIBLE);
            } else {
                btnRegister.setText("REGISTER");
                progressBar.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setLoadingState(): " + e.getMessage(), e);
        }
    }

    private void showToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast: " + e.getMessage(), e);
        }
    }

    // Debug method to check current user
    private void checkCurrentUser() {
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Log.d("FirebaseDebug", "Current User on Registration page: " + currentUser.getUid() + ", " + currentUser.getEmail());
                Log.d("FirebaseDebug", "User created: " + currentUser.getMetadata().getCreationTimestamp());
            } else {
                Log.d("FirebaseDebug", "No user currently logged in on Registration page");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in checkCurrentUser(): " + e.getMessage(), e);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}