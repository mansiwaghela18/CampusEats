package com.example.shashankscreen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

// Add these Firebase imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.graphics.drawable.GradientDrawable;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private Spinner spinnerUserType;
    private EditText etEmail, etPassword;
    private AppCompatButton btnLogin;
    private TextView tvDontHaveAccount;
    private ProgressBar progressBar;

    // Firebase Auth instance
    private FirebaseAuth mAuth;

    // SharedPreferences for storing login state
    private SharedPreferences loginPrefs;
    private static final String LOGIN_PREFS = "LoginPrefs";
    private static final String IS_LOGGED_IN = "isLoggedIn";
    private static final String USER_TYPE = "userType";
    private static final String USER_EMAIL = "userEmail";
    private static final String USER_FLOOR = "userFloor";

    // Fixed admin credentials
    private static final String ADMIN_EMAIL_1 = "Admin1@gmail.com";
    private static final String ADMIN_EMAIL_2 = "Admin2@gmail.com";
    private static final String ADMIN_PASSWORD = "campuseats";

    // Floor owner emails and fixed password
    private static final String FLOOR1_EMAIL = "floor1@gmail.com";
    private static final String FLOOR2_EMAIL = "floor2@gmail.com";
    private static final String FLOOR3_EMAIL = "floor3@gmail.com";
    private static final String FLOOR4_EMAIL = "floor4@gmail.com";
    private static final String FLOOR_PASSWORD = "campuseats"; // Fixed password for all floor owners

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() started");

        try {
            // Initialize Firebase Auth first
            mAuth = FirebaseAuth.getInstance();
            Log.d(TAG, "Firebase Auth initialized successfully");

            // Initialize SharedPreferences
            loginPrefs = getSharedPreferences(LOGIN_PREFS, MODE_PRIVATE);

            // Check if user is already logged in - CRITICAL FOR PERSISTENCE
            if (checkAndHandleExistingLogin()) {
                return; // User is already logged in, navigated to appropriate dashboard
            }

            setContentView(R.layout.activity_login);
            Log.d(TAG, "Layout set successfully");

            // Initialize views
            Log.d(TAG, "Initializing views");
            initializeViews();
            Log.d(TAG, "Views initialized successfully");

            // Set up click listeners
            Log.d(TAG, "Setting up click listeners");
            setupClickListeners();
            Log.d(TAG, "Click listeners set up successfully");

            // Pre-fill email if coming from registration
            prefillEmailFromIntent();

            // Debug: Check if there's already a logged-in user
            checkCurrentUser();

            Log.d(TAG, "onCreate() completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate(): " + e.getMessage(), e);
            showToast("Error initializing app: " + e.getMessage());
        }
    }

    /**
     * Enhanced method to check and handle existing login state
     * This is the key method for login persistence
     */
    private boolean checkAndHandleExistingLogin() {
        try {
            boolean isLoggedInPrefs = loginPrefs.getBoolean(IS_LOGGED_IN, false);
            FirebaseUser currentUser = mAuth.getCurrentUser();
            String savedUserType = loginPrefs.getString(USER_TYPE, "");
            String savedUserEmail = loginPrefs.getString(USER_EMAIL, "");
            String savedUserFloor = loginPrefs.getString(USER_FLOOR, "");

            Log.d(TAG, "Checking existing login state:");
            Log.d(TAG, "SharedPreferences isLoggedIn: " + isLoggedInPrefs);
            Log.d(TAG, "Firebase currentUser: " + (currentUser != null ? currentUser.getEmail() : "null"));
            Log.d(TAG, "Saved user type: " + savedUserType);
            Log.d(TAG, "Saved user email: " + savedUserEmail);
            Log.d(TAG, "Saved user floor: " + savedUserFloor);

            // Case 1: Both SharedPreferences and Firebase indicate user is logged in
            if (isLoggedInPrefs && currentUser != null && !savedUserType.isEmpty() && !savedUserEmail.isEmpty()) {
                // Verify the saved email matches the Firebase user
                if (savedUserEmail.equals(currentUser.getEmail())) {
                    Log.d(TAG, "Valid existing login found - redirecting to appropriate dashboard");

                    // Navigate to appropriate dashboard based on user type
                    if ("Owner".equals(savedUserType)) {
                        Log.d(TAG, "Redirecting to Owner Dashboard for: " + savedUserFloor);
                        navigateToOwnerDashboard(savedUserEmail, savedUserFloor);
                        return true;
                    } else if ("Student/Faculty".equals(savedUserType)) {
                        Log.d(TAG, "Redirecting to Student Dashboard");
                        navigateToStudentDashboard();
                        return true;
                    } else {
                        Log.w(TAG, "Unknown user type in saved state: " + savedUserType);
                        clearLoginState(this);
                        return false;
                    }
                } else {
                    Log.w(TAG, "Saved email (" + savedUserEmail + ") doesn't match Firebase user (" +
                            (currentUser != null ? currentUser.getEmail() : "null") + "), clearing login state");
                    clearLoginState(this);
                    return false;
                }
            }

            // Case 2: SharedPreferences says logged in but Firebase user is null
            else if (isLoggedInPrefs && currentUser == null) {
                Log.w(TAG, "SharedPreferences indicates login but Firebase user is null, clearing login state");
                clearLoginState(this);
                return false;
            }

            // Case 3: Firebase user exists but SharedPreferences says not logged in
            else if (!isLoggedInPrefs && currentUser != null) {
                Log.w(TAG, "Firebase user exists but SharedPreferences indicates not logged in, signing out from Firebase");
                mAuth.signOut();
                return false;
            }

            // Case 4: Neither indicates logged in - normal flow
            else {
                Log.d(TAG, "No existing login found - proceeding with normal login flow");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking login status: " + e.getMessage(), e);
            clearLoginState(this);
            return false;
        }
    }

    /**
     * Enhanced method to save login state with better error handling
     */
    private void saveLoginState(String userType, String email, String floor) {
        try {
            SharedPreferences.Editor editor = loginPrefs.edit();
            editor.putBoolean(IS_LOGGED_IN, true);
            editor.putString(USER_TYPE, userType);
            editor.putString(USER_EMAIL, email);
            editor.putString(USER_FLOOR, floor);

            // Use commit() instead of apply() to ensure immediate write
            boolean success = editor.commit();

            if (success) {
                Log.d(TAG, "Login state saved successfully:");
                Log.d(TAG, "- User Type: " + userType);
                Log.d(TAG, "- Email: " + email);
                Log.d(TAG, "- Floor: " + floor);
            } else {
                Log.e(TAG, "Failed to save login state to SharedPreferences");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving login state: " + e.getMessage(), e);
        }
    }

    // Helper method to determine floor number from email
    private String getFloorFromEmail(String email) {
        if (FLOOR1_EMAIL.equalsIgnoreCase(email)) {
            return "Floor 1";
        } else if (FLOOR2_EMAIL.equalsIgnoreCase(email)) {
            return "Floor 2";
        } else if (FLOOR3_EMAIL.equalsIgnoreCase(email)) {
            return "Floor 3";
        } else if (FLOOR4_EMAIL.equalsIgnoreCase(email)) {
            return "Floor 4";
        } else if (ADMIN_EMAIL_1.equals(email) || ADMIN_EMAIL_2.equals(email)) {
            return "Admin"; // Admin can see all floors
        }
        return "";
    }

    /**
     * Enhanced method to clear login state and sign out from Firebase
     */
    public static void clearLoginState(android.content.Context context) {
        try {
            // Clear SharedPreferences
            SharedPreferences loginPrefs = context.getSharedPreferences(LOGIN_PREFS, MODE_PRIVATE);
            SharedPreferences.Editor editor = loginPrefs.edit();
            editor.clear();
            boolean success = editor.commit(); // Use commit for immediate write

            if (success) {
                Log.d("LoginActivity", "Login state cleared from SharedPreferences successfully");
            } else {
                Log.e("LoginActivity", "Failed to clear login state from SharedPreferences");
            }

            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();
            Log.d("LoginActivity", "Signed out from Firebase successfully");

        } catch (Exception e) {
            Log.e("LoginActivity", "Error clearing login state: " + e.getMessage(), e);
        }
    }

    /**
     * Method to perform complete logout and redirect to login
     * Call this method from any activity when user wants to logout
     */
    public static void performLogout(android.content.Context context) {
        try {
            // Clear login state
            clearLoginState(context);

            // Navigate to LoginActivity
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);

            Log.d("LoginActivity", "Logout completed and redirected to LoginActivity");
        } catch (Exception e) {
            Log.e("LoginActivity", "Error performing logout: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            Log.d(TAG, "onStart() called");

            // Re-check authentication state when activity starts
            // This handles cases where user was logged out externally
            FirebaseUser currentUser = mAuth.getCurrentUser();
            boolean isLoggedInPrefs = loginPrefs.getBoolean(IS_LOGGED_IN, false);

            if (currentUser == null && isLoggedInPrefs) {
                Log.w(TAG, "Firebase user is null but SharedPreferences indicates logged in, clearing state");
                clearLoginState(this);
            } else if (currentUser != null && isLoggedInPrefs) {
                // User is still logged in, redirect to dashboard
                String userType = loginPrefs.getString(USER_TYPE, "");
                String userEmail = loginPrefs.getString(USER_EMAIL, "");
                String userFloor = loginPrefs.getString(USER_FLOOR, "");

                if (!userType.isEmpty() && !userEmail.isEmpty() && userEmail.equals(currentUser.getEmail())) {
                    Log.d(TAG, "User still logged in, redirecting to dashboard");
                    if ("Owner".equals(userType)) {
                        navigateToOwnerDashboard(userEmail, userFloor);
                        return;
                    } else if ("Student/Faculty".equals(userType)) {
                        navigateToStudentDashboard();
                        return;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onStart(): " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            Log.d(TAG, "onResume() called");

            // Additional check for login state consistency
            FirebaseUser currentUser = mAuth.getCurrentUser();
            boolean isLoggedInPrefs = loginPrefs.getBoolean(IS_LOGGED_IN, false);

            if (currentUser == null && isLoggedInPrefs) {
                Log.w(TAG, "User was logged out externally, clearing local state");
                clearLoginState(this);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onResume(): " + e.getMessage(), e);
        }
    }

    private void initializeViews() {
        Log.d(TAG, "Finding views by ID");

        try {
            spinnerUserType = findViewById(R.id.spinnerUserType);
            if (spinnerUserType == null) {
                Log.e(TAG, "spinnerUserType is null");
                throw new RuntimeException("spinnerUserType not found");
            }
            Log.d(TAG, "spinnerUserType found");

            etEmail = findViewById(R.id.etEmail);
            if (etEmail == null) {
                Log.e(TAG, "etEmail is null");
                throw new RuntimeException("etEmail not found");
            }
            Log.d(TAG, "etEmail found");

            etPassword = findViewById(R.id.etPassword);
            if (etPassword == null) {
                Log.e(TAG, "etPassword is null");
                throw new RuntimeException("etPassword not found");
            }
            Log.d(TAG, "etPassword found");

            btnLogin = findViewById(R.id.btnLogin);
            if (btnLogin == null) {
                Log.e(TAG, "btnLogin is null");
                throw new RuntimeException("btnLogin not found");
            }
            Log.d(TAG, "btnLogin found");

            tvDontHaveAccount = findViewById(R.id.tvDontHaveAccount);
            if (tvDontHaveAccount == null) {
                Log.e(TAG, "tvDontHaveAccount is null");
                throw new RuntimeException("tvDontHaveAccount not found");
            }
            Log.d(TAG, "tvDontHaveAccount found");

            progressBar = findViewById(R.id.progressBar);
            if (progressBar == null) {
                Log.e(TAG, "progressBar is null");
                throw new RuntimeException("progressBar not found");
            }
            Log.d(TAG, "progressBar found");

            // Set up spinner
            Log.d(TAG, "Setting up spinner");
            setupSpinner();
            Log.d(TAG, "Spinner setup completed");

        } catch (Exception e) {
            Log.e(TAG, "Error in initializeViews(): " + e.getMessage(), e);
            throw e;
        }
    }

    private void setupSpinner() {
        try {
            Log.d(TAG, "Creating spinner adapter");

            // Create a custom adapter that ensures black text in dropdown
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                    android.R.layout.simple_spinner_item) {

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);
                    TextView textView = (TextView) view;
                    textView.setTextColor(Color.BLACK);
                    textView.setAlpha(1.0f); // Ensure full opacity
                    textView.setTextSize(16f); // Set consistent text size
                    return view;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView textView = (TextView) view;
                    textView.setTextColor(Color.parseColor("#424242")); // Dark gray color
                    textView.setAlpha(1.0f);
                    return view;
                }
            };

            // Populate adapter with user types
            String[] userTypes = getResources().getStringArray(R.array.user_types);
            for (String userType : userTypes) {
                adapter.add(userType);
            }

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerUserType.setAdapter(adapter);
            Log.d(TAG, "Spinner adapter set successfully");

            // Set spinner listener to handle password hint changes
            spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        String selectedUserType = parent.getItemAtPosition(position).toString();
                        Log.d(TAG, "Spinner selection changed to: " + selectedUserType);

                        if ("Owner".equals(selectedUserType)) {
                            etEmail.setHint("Use: Given mail");
                            etPassword.setHint("Fixed");
                        } else if ("Student/Faculty".equals(selectedUserType)) {
                            etEmail.setHint("Enter your email address");
                            etPassword.setHint("Enter your password");
                            etEmail.setText("");
                        } else {
                            etEmail.setHint("Email Address");
                            etPassword.setHint("Password");
                        }

                        // Ensure selected item text is also visible
                        if (view != null) {
                            try {
                                ((TextView) view).setTextColor(Color.BLACK);
                                ((TextView) view).setAlpha(1.0f);
                            } catch (Exception e) {
                                Log.w(TAG, "Could not set spinner text color: " + e.getMessage());
                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error in spinner onItemSelected: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    etEmail.setHint("Email Address");
                    etPassword.setHint("Password");
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in setupSpinner(): " + e.getMessage(), e);
            throw e;
        }
    }

    private void setupClickListeners() {
        try {
            // Login button click
            btnLogin.setOnClickListener(v -> {
                Log.d(TAG, "Login button clicked");
                loginUser();
            });

            // Don't have account click
            tvDontHaveAccount.setOnClickListener(v -> {
                Log.d(TAG, "Don't have account clicked");
                try {
                    Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to registration: " + e.getMessage(), e);
                    showToast("Error: RegistrationActivity not found");
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in setupClickListeners(): " + e.getMessage(), e);
            throw e;
        }
    }

    private void prefillEmailFromIntent() {
        try {
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra("email")) {
                String email = intent.getStringExtra("email");
                Log.d(TAG, "Pre-filling email: " + email);
                etEmail.setText(email);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in prefillEmailFromIntent(): " + e.getMessage(), e);
        }
    }

    private void loginUser() {
        try {
            String userType = spinnerUserType.getSelectedItem().toString();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            Log.d(TAG, "Login attempt - UserType: " + userType + ", Email: " + email);

            // Validate input
            if (!validateInput(userType, email, password)) {
                return;
            }

            // Show loading state
            setLoadingState(true);

            // Handle login based on user type
            if ("Owner".equals(userType)) {
                Log.d(TAG, "Calling handleOwnerLogin");
                handleOwnerLogin(email, password);
            } else if ("Student/Faculty".equals(userType)) { // Changed from "Student"
                Log.d(TAG, "Calling handleStudentFacultyLogin");
                handleStudentFacultyLogin(email, password); // Renamed method
            } else {
                Log.e(TAG, "Unknown user type: " + userType);
                setLoadingState(false);
                showToast("Please select a valid user type");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in loginUser(): " + e.getMessage(), e);
            setLoadingState(false);
            showToast("Login error: " + e.getMessage());
        }
    }

    private void handleOwnerLogin(String email, String password) {
        Log.d(TAG, "Handling owner login for email: " + email);

        // Check if it's admin login
        if (ADMIN_EMAIL_1.equals(email) || ADMIN_EMAIL_2.equals(email)) {
            if (!ADMIN_PASSWORD.equals(password)) {
                setLoadingState(false);
                showToast("Invalid admin password! Use: " + ADMIN_PASSWORD);
                return;
            }

            // Handle admin login
            authenticateWithFirebase(email, ADMIN_PASSWORD, "Owner");
            return;
        }

        // Check if it's a floor owner email
        String floor = getFloorFromEmail(email);
        if (floor.isEmpty()) {
            setLoadingState(false);
            showToast("Invalid floor owner email! Use: floor1@gmail.com, floor2@gmail.com, floor3@gmail.com, or floor4@gmail.com");
            return;
        }

        // Check if password is correct for floor owners
        if (!FLOOR_PASSWORD.equals(password)) {
            setLoadingState(false);
            showToast("Invalid password! Use: " + FLOOR_PASSWORD);
            return;
        }

        Log.d(TAG, "Floor owner login attempt for " + floor);

        // Authenticate floor owner with Firebase
        authenticateWithFirebase(email, FLOOR_PASSWORD, "Owner");
    }

    private void authenticateWithFirebase(String email, String password, String userType) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login success
                        Log.d("FirebaseAuth", userType + " login successful");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            Log.d("FirebaseDebug", userType + " Login: " + user.getUid() + ", " + user.getEmail());
                        }

                        String floor = getFloorFromEmail(email);

                        // CRITICAL: Save login state immediately after successful authentication
                        saveLoginState(userType, email, floor);

                        if ("Owner".equals(userType)) {
                            if ("Admin".equals(floor)) {
                                showToast("Admin login successful!");
                            } else {
                                showToast(floor + " owner login successful!");
                            }
                            navigateToOwnerDashboard(email, floor);
                        } else {
                            showToast("Student/Faculty login successful!");
                            navigateToStudentDashboard();
                        }

                    } else {
                        // Login failed - try to create account
                        Log.w("FirebaseAuth", "Login failed, trying to create account", task.getException());
                        createFirebaseAccount(email, password, userType);
                    }
                });
    }

    private void createFirebaseAccount(String email, String password, String userType) {
        Log.d(TAG, "Creating Firebase account for: " + email);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseAuth", userType + " account created successfully");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            Log.d("FirebaseDebug", "New " + userType + " Created: " + user.getUid() + ", " + user.getEmail());
                        }

                        String floor = getFloorFromEmail(email);

                        // CRITICAL: Save login state immediately after successful account creation
                        saveLoginState(userType, email, floor);

                        if ("Owner".equals(userType)) {
                            if ("Admin".equals(floor)) {
                                showToast("Admin account created and login successful!");
                            } else {
                                showToast(floor + " owner account created and login successful!");
                            }
                            navigateToOwnerDashboard(email, floor);
                        } else {
                            showToast("Student/Faculty account created and login successful!");
                            navigateToStudentDashboard();
                        }

                    } else {
                        Log.e("FirebaseAuth", "Failed to create account", task.getException());
                        setLoadingState(false);
                        handleLoginError(task.getException());
                    }
                });
    }

    private void handleStudentFacultyLogin(String email, String password) {
        Log.d(TAG, "Handling student/faculty login");
        authenticateWithFirebase(email, password, "Student/Faculty");
    }

    private void navigateToOwnerDashboard(String email, String floor) {
        try {
            Log.d(TAG, "Attempting to navigate to Owner Dashboard for " + floor);
            setLoadingState(false);

            Intent intent;

            // Navigate to appropriate dashboard based on email/floor
            if ("Admin".equals(floor)) {
                // Admin sees the main dashboard with all floors
                Log.d(TAG, "Creating intent for OwnerDashboardActivity (Admin)");
                intent = new Intent(LoginActivity.this, OwnerDashboardActivity.class);
            } else {
                // Floor owners see their specific floor dashboard
                switch (floor) {
                    case "Floor 1":
                        Log.d(TAG, "Creating intent for Floor1DashboardActivity");
                        intent = new Intent(LoginActivity.this, Floor1DashboardActivity.class);
                        break;
                    case "Floor 2":
                        Log.d(TAG, "Creating intent for Floor2DashboardActivity");
                        intent = new Intent(LoginActivity.this, Floor2DashboardActivity.class);
                        break;
                    case "Floor 3":
                        Log.d(TAG, "Creating intent for Floor3DashboardActivity");
                        intent = new Intent(LoginActivity.this, Floor3DashboardActivity.class);
                        break;
                    case "Floor 4":
                        Log.d(TAG, "Creating intent for Floor4DashboardActivity");
                        intent = new Intent(LoginActivity.this, Floor4DashboardActivity.class);
                        break;
                    default:
                        Log.w(TAG, "Unknown floor: " + floor + ", defaulting to main dashboard");
                        intent = new Intent(LoginActivity.this, OwnerDashboardActivity.class);
                        break;
                }
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("floor_name", floor);
            intent.putExtra("user_email", email);

            Log.d(TAG, "Starting dashboard activity");
            startActivity(intent);
            finish();
            Log.d(TAG, "Navigation to " + floor + " Dashboard completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to Owner Dashboard: " + e.getMessage(), e);
            setLoadingState(false);
            showToast("Error: Unable to open Dashboard - " + e.getMessage());
        }
    }

    private void navigateToStudentDashboard() {
        try {
            Log.d(TAG, "Attempting to navigate to StudentDashboard");
            setLoadingState(false);

            // Check if the activity exists
            try {
                Class<?> studentDashboardClass = Class.forName("com.example.shashankscreen.StudentDashboardActivity");
                Log.d(TAG, "StudentDashboardActivity class found");

                Intent intent = new Intent(LoginActivity.this, studentDashboardClass);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                Log.d(TAG, "Navigation to StudentDashboard initiated");

            } catch (ClassNotFoundException e) {
                Log.e(TAG, "StudentDashboardActivity class not found: " + e.getMessage(), e);
                showToast("Error: StudentDashboardActivity not found. Please create this activity first.");
                return;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to StudentDashboard: " + e.getMessage(), e);
            setLoadingState(false);
            showToast("Error: Unable to open Student Dashboard - " + e.getMessage());
        }
    }

    private void handleLoginError(Exception exception) {
        String errorMessage = "Login failed.";

        if (exception != null) {
            String exceptionMessage = exception.getMessage();
            if (exceptionMessage != null) {
                if (exceptionMessage.contains("no user record")) {
                    errorMessage = "No account found with this email. Please register first.";
                } else if (exceptionMessage.contains("wrong password")) {
                    errorMessage = "Incorrect password. Please try again.";
                } else if (exceptionMessage.contains("invalid email")) {
                    errorMessage = "Please enter a valid email address.";
                } else if (exceptionMessage.contains("too many requests")) {
                    errorMessage = "Too many failed attempts. Please try again later.";
                } else if (exceptionMessage.contains("email address is already in use")) {
                    errorMessage = "Account already exists. Please try logging in.";
                } else {
                    errorMessage = "Login failed: " + exceptionMessage;
                }
            }
        }

        showToast(errorMessage);
        Log.e("FirebaseDebug", "Login error: " + errorMessage);
    }

    private boolean validateInput(String userType, String email, String password) {
        try {
            // Reset errors
            etEmail.setError(null);
            etPassword.setError(null);

            // Validate user type selection
            if ("Select User Type".equals(userType)) {
                showToast("Please select a user type");
                return false;
            }

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

            // Additional validation for owner
            if ("Owner".equals(userType)) {
                // Check if it's a valid owner email
                if (!(ADMIN_EMAIL_1.equals(email) || ADMIN_EMAIL_2.equals(email) ||
                        FLOOR1_EMAIL.equalsIgnoreCase(email) || FLOOR2_EMAIL.equalsIgnoreCase(email) ||
                        FLOOR3_EMAIL.equalsIgnoreCase(email) || FLOOR4_EMAIL.equalsIgnoreCase(email))) {
                    etEmail.setError("Invalid owner email! Use admin emails or floor1@gmail.com to floor4@gmail.com");
                    etEmail.requestFocus();
                    return false;
                }

                // Check for correct password
                if ((ADMIN_EMAIL_1.equals(email) || ADMIN_EMAIL_2.equals(email)) && !ADMIN_PASSWORD.equals(password)) {
                    etPassword.setError("Use the fixed admin password: " + ADMIN_PASSWORD);
                    etPassword.requestFocus();
                    return false;
                }

                if ((FLOOR1_EMAIL.equalsIgnoreCase(email) || FLOOR2_EMAIL.equalsIgnoreCase(email) ||
                        FLOOR3_EMAIL.equalsIgnoreCase(email) || FLOOR4_EMAIL.equalsIgnoreCase(email)) && !FLOOR_PASSWORD.equals(password)) {
                    etPassword.setError("Use the fixed floor password: " + FLOOR_PASSWORD);
                    etPassword.requestFocus();
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error in validateInput(): " + e.getMessage(), e);
            return false;
        }
    }

    private void setLoadingState(boolean isLoading) {
        try {
            btnLogin.setEnabled(!isLoading);
            etEmail.setEnabled(!isLoading);
            etPassword.setEnabled(!isLoading);
            spinnerUserType.setEnabled(!isLoading);

            if (isLoading) {
                btnLogin.setText("SIGNING IN...");
                progressBar.setVisibility(View.VISIBLE);
            } else {
                btnLogin.setText("LOGIN");
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
                Log.d("FirebaseDebug", "Current User on Login page: " + currentUser.getUid() + ", " + currentUser.getEmail());
            } else {
                Log.d("FirebaseDebug", "No user currently logged in on Login page");
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