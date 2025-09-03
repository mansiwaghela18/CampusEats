package com.example.shashankscreen;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.io.IOException;
import java.util.List;

public class OwnerFloor1MenuActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private TextView floorTitleText;
    private LinearLayout menuContainer;
    private FloatingActionButton fabAddMenu;
    private String floorName;
    private List<MenuDataManager.MenuItem> menuItems;
    private MenuDataManager.MenuItem currentEditingItem = null;
    private ImageView dialogImageView;
    private Uri selectedImageUri;
    private boolean isEditMode = false;
    private MenuDataManager menuDataManager;
    private OrderNotificationManager orderNotificationManager;
    private TextView orderNotificationBadge;
    private Handler notificationCheckHandler;
    private Runnable notificationCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_floor1_menu);

        // Get floor name from intent
        floorName = getIntent().getStringExtra("floor_name");
        if (floorName == null) floorName = "Floor 1";

        // Initialize menu data manager
        menuDataManager = MenuDataManager.getInstance(this);
        // Set the current floor context
        menuDataManager.setCurrentFloor(floorName);
        orderNotificationManager = OrderNotificationManager.getInstance(this);


        // Initialize views
        initializeViews();

        // Load menu items from data manager
        loadMenuItems();

        // Setup menu display
        setupMenu();

        // Setup add menu functionality
        setupAddMenuFunctionality();

        // Start entrance animations
        startEntranceAnimations();
        startNotificationCheck();
    }
    private void startNotificationCheck() {
        notificationCheckHandler = new Handler();
        notificationCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    updateOrderNotificationBadge();
                    notificationCheckHandler.postDelayed(this, 5000); // Check every 5 seconds
                }
            }
        };
        notificationCheckHandler.postDelayed(notificationCheckRunnable, 1000);
    }
    private void updateOrderNotificationBadge() {
        int pendingOrderCount = orderNotificationManager.getPendingOrderCount(floorName);

        if (pendingOrderCount > 0) {
            if (orderNotificationBadge == null) {
                createOrderNotificationBadge();
            }
            orderNotificationBadge.setText(String.valueOf(pendingOrderCount));
            orderNotificationBadge.setVisibility(View.VISIBLE);

            // Animate badge if new orders
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(orderNotificationBadge, "scaleX", 1.2f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(orderNotificationBadge, "scaleY", 1.2f, 1f);
            scaleX.setDuration(300);
            scaleY.setDuration(300);

            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(scaleX, scaleY);
            animSet.start();
        } else if (orderNotificationBadge != null) {
            orderNotificationBadge.setVisibility(View.GONE);
        }
    }
    private void addViewOrdersButton() {
        // Create "View Orders" button (add this to your layout or create programmatically)
        Button viewOrdersButton = new Button(this);
        viewOrdersButton.setText("📋 View Orders");
        viewOrdersButton.setTextColor(Color.WHITE);
        viewOrdersButton.setTextSize(14);
        viewOrdersButton.setTypeface(viewOrdersButton.getTypeface(), android.graphics.Typeface.BOLD);

        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(Color.parseColor("#fc9432"));
        buttonBackground.setCornerRadius(dpToPx(8));
        viewOrdersButton.setBackground(buttonBackground);

        viewOrdersButton.setOnClickListener(v -> {
            Intent orderManagementIntent = new Intent(this, OwnerOrderManagementActivity.class);
            orderManagementIntent.putExtra("floor_name", floorName);
            startActivity(orderManagementIntent);
            animateButtonClick(viewOrdersButton);
        });

        // Add button to your layout (you'll need to modify your XML layout file)
        // For now, you can add it programmatically to your existing layout
    }
    private void createOrderNotificationBadge() {
        // Create floating notification badge
        orderNotificationBadge = new TextView(this);
        orderNotificationBadge.setTextSize(12);
        orderNotificationBadge.setTextColor(Color.WHITE);
        orderNotificationBadge.setTypeface(orderNotificationBadge.getTypeface(), android.graphics.Typeface.BOLD);
        orderNotificationBadge.setGravity(android.view.Gravity.CENTER);
        orderNotificationBadge.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));

        // Create background
        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setColor(Color.parseColor("#F44336"));
        badgeBackground.setCornerRadius(dpToPx(12));
        orderNotificationBadge.setBackground(badgeBackground);

        // Position badge (you'll need to adjust based on your layout)
        android.widget.FrameLayout.LayoutParams badgeParams = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        badgeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        badgeParams.setMargins(0, dpToPx(16), dpToPx(16), 0);

        // Add badge to your root layout (you may need to wrap your existing layout in FrameLayout)
        if (findViewById(android.R.id.content) instanceof android.widget.FrameLayout) {
            ((android.widget.FrameLayout) findViewById(android.R.id.content)).addView(orderNotificationBadge, badgeParams);
        }

        // Make badge clickable to open order management
        orderNotificationBadge.setOnClickListener(v -> {
            Intent orderManagementIntent = new Intent(this, OwnerOrderManagementActivity.class);
            orderManagementIntent.putExtra("floor_name", floorName);
            startActivity(orderManagementIntent);
        });
    }



    @Override
    protected void onResume() {
        super.onResume();
        // Refresh menu data when returning to this activity
        refreshMenuData();
        if (orderNotificationManager != null) {
            updateOrderNotificationBadge();
            startNotificationCheck();
        }
    }


    private void refreshMenuData() {
        menuDataManager.refreshData(this); // Add 'this' as context parameter
        loadMenuItems();
        setupMenu();
    }

    private void initializeViews() {
        floorTitleText = findViewById(R.id.floor_title_text);
        menuContainer = findViewById(R.id.menu_container);
        fabAddMenu = findViewById(R.id.fab_add_menu);

        if (floorTitleText == null) {
            Toast.makeText(this, "Error: floor_title_text not found in layout", Toast.LENGTH_LONG).show();
            return;
        }
        if (menuContainer == null) {
            Toast.makeText(this, "Error: menu_container not found in layout", Toast.LENGTH_LONG).show();
            return;
        }
        if (fabAddMenu == null) {
            Toast.makeText(this, "Error: fab_add_menu not found in layout", Toast.LENGTH_LONG).show();
            return;
        }

        floorTitleText.setText(floorName + " Menu Management");

        // Ensure FAB is visible and clickable
        fabAddMenu.setVisibility(View.VISIBLE);
        fabAddMenu.setClickable(true);
        fabAddMenu.setFocusable(true);
        fabAddMenu.bringToFront();
    }


    private void loadMenuItems() {
        menuItems = menuDataManager.getAllMenuItems();
    }

    private void setupMenu() {
        if (menuContainer == null) return;

        menuContainer.removeAllViews();
        String currentCategory = "";

        for (MenuDataManager.MenuItem item : menuItems) {
            if (!item.category.equals(currentCategory)) {
                currentCategory = item.category;
                addCategoryHeader(currentCategory);
            }
            addMenuItemCard(item);
        }
    }

    private void addCategoryHeader(String category) {
        TextView categoryHeader = new TextView(this);
        categoryHeader.setText(category);
        categoryHeader.setTextSize(20);
        categoryHeader.setTextColor(Color.parseColor("#fc9432"));
        categoryHeader.setTypeface(categoryHeader.getTypeface(), android.graphics.Typeface.BOLD);
        categoryHeader.setPadding(dpToPx(16), dpToPx(20), dpToPx(16), dpToPx(8));
        menuContainer.addView(categoryHeader);
    }

    private void addMenuItemCard(MenuDataManager.MenuItem item) {
        CardView cardView = new CardView(this);
        cardView.setCardElevation(dpToPx(6));
        cardView.setRadius(dpToPx(16));
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setUseCompatPadding(true);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        cardView.setLayoutParams(cardParams);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Top section with image and content
        LinearLayout topLayout = new LinearLayout(this);
        topLayout.setOrientation(LinearLayout.HORIZONTAL);
        topLayout.setPadding(0, 0, 0, dpToPx(12));

        // Create image view
        ImageView itemImage = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dpToPx(80), dpToPx(80));
        imageParams.setMargins(0, 0, dpToPx(16), 0);
        itemImage.setLayoutParams(imageParams);
        itemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Load image (custom or default)
        if (item.hasCustomImage && item.imageUri != null && !item.imageUri.isEmpty()) {
            try {
                Uri imageUri = Uri.parse(item.imageUri);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                itemImage.setImageBitmap(bitmap);
            } catch (IOException | SecurityException e) {
                itemImage.setImageResource(item.imageRes);
            }
        } else {
            itemImage.setImageResource(item.imageRes);
        }

        // Create rounded background for image
        GradientDrawable imageBackground = new GradientDrawable();
        imageBackground.setColor(Color.parseColor("#FFF8F3"));
        imageBackground.setCornerRadius(dpToPx(12));
        itemImage.setBackground(imageBackground);

        // Create content layout
        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        // Item name
        TextView nameText = new TextView(this);
        nameText.setText(item.name);
        nameText.setTextSize(18);
        nameText.setTypeface(nameText.getTypeface(), android.graphics.Typeface.BOLD);
        nameText.setTextColor(Color.parseColor("#333333"));

        // Item description
        TextView descText = new TextView(this);
        descText.setText(item.description);
        descText.setTextSize(14);
        descText.setTextColor(Color.parseColor("#666666"));
        descText.setPadding(0, dpToPx(4), 0, dpToPx(8));

        // Price text
        TextView priceText = new TextView(this);
        priceText.setText("₹" + String.format("%.2f", item.price));
        priceText.setTextSize(16);
        priceText.setTypeface(priceText.getTypeface(), android.graphics.Typeface.BOLD);
        priceText.setTextColor(Color.parseColor("#fc9432"));

        // Stock layout with icon
        LinearLayout stockLayout = new LinearLayout(this);
        stockLayout.setOrientation(LinearLayout.HORIZONTAL);
        stockLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        stockLayout.setPadding(0, dpToPx(4), 0, dpToPx(4));

        // Stock icon
        ImageView stockIcon = new ImageView(this);
        stockIcon.setImageResource(android.R.drawable.ic_menu_agenda); // Using system drawable as fallback
        stockIcon.setColorFilter(item.stock > 5 ? Color.parseColor("#4CAF50") :
                item.stock > 0 ? Color.parseColor("#FF9800") : Color.parseColor("#F44336"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(16), dpToPx(16));
        iconParams.setMargins(0, 0, dpToPx(4), 0);
        stockIcon.setLayoutParams(iconParams);

        // Stock text
        TextView stockText = new TextView(this);
        stockText.setText("Stock: " + item.stock + " items");
        stockText.setTextSize(14);
        stockText.setTypeface(stockText.getTypeface(), android.graphics.Typeface.BOLD);
        stockText.setTextColor(item.stock > 5 ? Color.parseColor("#4CAF50") :
                item.stock > 0 ? Color.parseColor("#FF9800") : Color.parseColor("#F44336"));

        stockLayout.addView(stockIcon);
        stockLayout.addView(stockText);

        contentLayout.addView(nameText);
        contentLayout.addView(descText);
        contentLayout.addView(priceText);
        contentLayout.addView(stockLayout);

        topLayout.addView(itemImage);
        topLayout.addView(contentLayout);

        // Bottom section with buttons
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(android.view.Gravity.END);
        buttonLayout.setPadding(0, dpToPx(8), 0, 0);

        // Edit button
        Button editButton = createStyledButton("EDIT", "#fc9432", "#e8851e");
        LinearLayout.LayoutParams editButtonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        editButtonParams.setMargins(0, 0, dpToPx(12), 0);
        editButton.setLayoutParams(editButtonParams);

        // Delete button
        Button deleteButton = createStyledButton("DELETE", "#fc9432", "#e8851e");
        LinearLayout.LayoutParams deleteButtonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        deleteButton.setLayoutParams(deleteButtonParams);

        // Add button click listeners
        editButton.setOnClickListener(v -> {
            showEditMenuDialog(item);
            animateButtonClick(editButton);
        });

        deleteButton.setOnClickListener(v -> {
            showDeleteConfirmationDialog(item);
            animateButtonClick(deleteButton);
        });

        buttonLayout.addView(editButton);
        buttonLayout.addView(deleteButton);

        mainLayout.addView(topLayout);
        mainLayout.addView(buttonLayout);

        cardView.addView(mainLayout);

        // Add card entrance animation
        cardView.setAlpha(0f);
        cardView.setTranslationY(50f);

        new Handler().postDelayed(() -> {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(cardView, "alpha", 0f, 1f);
            ObjectAnimator translation = ObjectAnimator.ofFloat(cardView, "translationY", 50f, 0f);
            alpha.setDuration(400);
            translation.setDuration(400);
            alpha.setInterpolator(new DecelerateInterpolator());
            translation.setInterpolator(new DecelerateInterpolator());

            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(alpha, translation);
            animSet.start();
        }, menuContainer.getChildCount() * 50);

        menuContainer.addView(cardView);
    }

    private Button createStyledButton(String text, String normalColor, String pressedColor) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(12);
        button.setTypeface(button.getTypeface(), android.graphics.Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        button.setMinWidth(dpToPx(80));
        button.setMinHeight(dpToPx(36));

        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(Color.parseColor(normalColor));
        buttonBackground.setCornerRadius(dpToPx(18));
        button.setBackground(buttonBackground);

        return button;
    }

    private void showAddMenuDialog() {
        isEditMode = false;
        currentEditingItem = null;
        selectedImageUri = null;
        showMenuDialog("Add New Menu Item", "Add");
    }

    private void showEditMenuDialog(MenuDataManager.MenuItem item) {
        isEditMode = true;
        currentEditingItem = item;
        selectedImageUri = item.hasCustomImage && item.imageUri != null ? Uri.parse(item.imageUri) : null;
        showMenuDialog("Edit Menu Item", "Update");
    }


    private void showMenuDialog(String title, String buttonText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        View dialogView;
        try {
            dialogView = inflater.inflate(R.layout.dialog_add_menu_item, null);
        } catch (Exception e) {
            Toast.makeText(this, "Error: dialog_add_menu_item layout not found", Toast.LENGTH_LONG).show();
            return;
        }

        TextInputEditText etName = dialogView.findViewById(R.id.et_menu_name);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_menu_description);
        TextInputEditText etPrice = dialogView.findViewById(R.id.et_menu_price);
        TextInputEditText etCategory = dialogView.findViewById(R.id.et_menu_category);
        TextInputEditText etStock = dialogView.findViewById(R.id.et_menu_stock);
        dialogImageView = dialogView.findViewById(R.id.iv_menu_image);
        Button btnSelectImage = dialogView.findViewById(R.id.btn_select_image);

        if (etName == null || etDescription == null || etPrice == null || etCategory == null || etStock == null) {
            Toast.makeText(this, "Error: Dialog input fields not found", Toast.LENGTH_LONG).show();
            return;
        }

        // Pre-fill data if editing
        if (isEditMode && currentEditingItem != null) {
            etName.setText(currentEditingItem.name);
            etDescription.setText(currentEditingItem.description);
            etPrice.setText(String.valueOf(currentEditingItem.price));
            etCategory.setText(currentEditingItem.category);
            etStock.setText(String.valueOf(currentEditingItem.stock));

            // Load existing image
            if (currentEditingItem.hasCustomImage && currentEditingItem.imageUri != null) {
                try {
                    Uri imageUri = Uri.parse(currentEditingItem.imageUri);
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    dialogImageView.setImageBitmap(bitmap);
                } catch (IOException | SecurityException e) {
                    dialogImageView.setImageResource(currentEditingItem.imageRes);
                }
            } else {
                dialogImageView.setImageResource(currentEditingItem.imageRes);
            }
        } else {
            // Set default stock for new items
            etStock.setText("10");
        }

        // Image selection button
        if (btnSelectImage != null) {
            btnSelectImage.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            });
        }

        AlertDialog dialog = builder.setView(dialogView)
                .setTitle(title)
                .setPositiveButton(buttonText, null)
                .setNegativeButton("Cancel", (d, which) -> {
                    currentEditingItem = null;
                    isEditMode = false;
                    selectedImageUri = null;
                })
                .create();

        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String stockStr = etStock.getText().toString().trim();

            if (validateMenuInput(name, description, priceStr, category, stockStr)) {
                double price = Double.parseDouble(priceStr);
                int stock = Integer.parseInt(stockStr);

                if (isEditMode && currentEditingItem != null) {
                    // Update existing item
                    MenuDataManager.MenuItem updatedItem = new MenuDataManager.MenuItem(
                            name, description, price, currentEditingItem.imageRes, category,
                            selectedImageUri != null ? selectedImageUri.toString() : currentEditingItem.imageUri,
                            selectedImageUri != null || currentEditingItem.hasCustomImage,
                            floorName, stock
                    );
                    updatedItem.id = currentEditingItem.id; // Set the ID after creation

                    menuDataManager.updateMenuItem(currentEditingItem.id, updatedItem);
                    Toast.makeText(this, "Menu item updated successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    // Add new item
                    MenuDataManager.MenuItem newItem;
                    if (selectedImageUri != null) {
                        newItem = new MenuDataManager.MenuItem(name, description, price, selectedImageUri.toString(), category, floorName, stock);
                    } else {
                        newItem = new MenuDataManager.MenuItem(name, description, price, R.drawable.khandvi, category, floorName, stock);
                    }

                    menuDataManager.addMenuItem(newItem);
                    Toast.makeText(this, "Menu item added successfully!", Toast.LENGTH_SHORT).show();
                }

                loadMenuItems();
                setupMenu();
                currentEditingItem = null;
                isEditMode = false;
                selectedImageUri = null;
                dialog.dismiss();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (dialogImageView != null && selectedImageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    dialogImageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showDeleteConfirmationDialog(MenuDataManager.MenuItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Menu Item")
                .setMessage("Are you sure you want to delete '" + item.name + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    menuDataManager.deleteMenuItem(item);
                    loadMenuItems();
                    setupMenu();
                    Toast.makeText(this, "Menu item deleted successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean validateMenuInput(String name, String description, String priceStr, String category, String stockStr) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter menu item name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "Please enter price", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please enter category", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(stockStr)) {
            Toast.makeText(this, "Please enter stock quantity", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double price = Double.parseDouble(priceStr);
            if (price <= 0) {
                Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            int stock = Integer.parseInt(stockStr);
            if (stock < 0) {
                Toast.makeText(this, "Stock cannot be negative", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid stock number", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check for duplicate menu items (excluding current item if editing)
        long excludeId = isEditMode && currentEditingItem != null ? currentEditingItem.id : -1;
        if (menuDataManager.isMenuItemNameExists(name, excludeId)) {
            Toast.makeText(this, "Menu item with this name already exists", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void animateButtonClick(Button button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f, 1f);
        scaleX.setDuration(150);
        scaleY.setDuration(150);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.start();
    }

    private void setupAddMenuFunctionality() {
        if (fabAddMenu != null) {
            fabAddMenu.setOnClickListener(v -> {
                showAddMenuDialog();

                // Animate FAB click
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(fabAddMenu, "scaleX", 1f, 0.9f, 1f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(fabAddMenu, "scaleY", 1f, 0.9f, 1f);
                scaleX.setDuration(150);
                scaleY.setDuration(150);

                AnimatorSet animSet = new AnimatorSet();
                animSet.playTogether(scaleX, scaleY);
                animSet.start();
            });
        }
    }

    private void startEntranceAnimations() {
        if (floorTitleText != null) {
            ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(floorTitleText, "alpha", 0f, 1f);
            ObjectAnimator titleTranslation = ObjectAnimator.ofFloat(floorTitleText, "translationY", -50f, 0f);
            titleAlpha.setDuration(600);
            titleTranslation.setDuration(600);

            AnimatorSet titleAnimSet = new AnimatorSet();
            titleAnimSet.playTogether(titleAlpha, titleTranslation);
            titleAnimSet.start();
        }

        if (fabAddMenu != null) {
            new Handler().postDelayed(() -> {
                ObjectAnimator fabScale = ObjectAnimator.ofFloat(fabAddMenu, "scaleX", 0f, 1f);
                ObjectAnimator fabScaleY = ObjectAnimator.ofFloat(fabAddMenu, "scaleY", 0f, 1f);
                fabScale.setDuration(400);
                fabScaleY.setDuration(400);

                AnimatorSet fabAnimSet = new AnimatorSet();
                fabAnimSet.playTogether(fabScale, fabScaleY);
                fabAnimSet.start();
            }, 500);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}