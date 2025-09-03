package com.example.shashankscreen;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;
import com.google.gson.Gson;

public class Floor2MenuActivity extends AppCompatActivity {

    private TextView floorTitleText;
    private EditText searchEditText;
    private LinearLayout menuContainer;
    private FloatingActionButton fabCart;
    private TextView cartBadge;
    private String floorName;
    private ArrayList<CartItem> cartItems;
    private int totalCartItems = 0;

    // For search functionality
    private List<MenuDataManager.MenuItem> allMenuItems;
    private List<MenuDataManager.MenuItem> filteredMenuItems;
    private MenuDataManager menuDataManager;

    // Cart item data structure - MADE SERIALIZABLE
    public static class CartItem implements Serializable {
        MenuDataManager.MenuItem menuItem;
        int quantity;

        CartItem(MenuDataManager.MenuItem menuItem, int quantity) {
            this.menuItem = menuItem;
            this.quantity = quantity;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canteen_menu);

        // Get floor name from intent
        floorName = getIntent().getStringExtra("floor_name");
        if (floorName == null) floorName = "Floor 2";

        // Initialize cart
        cartItems = new ArrayList<>();

        // Initialize menu data manager
        menuDataManager = MenuDataManager.getInstance(this);
        // Set the current floor context
        menuDataManager.setCurrentFloor(floorName);

        // Initialize views
        initializeViews();

        // Setup menu based on floor
        setupMenu();

        // Setup search functionality
        setupSearchFunctionality();

        // Setup cart functionality
        setupCartFunctionality();

        // Start entrance animations
        startEntranceAnimations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh menu data when returning to this activity
        menuDataManager.refreshData(this); // Pass 'this' as context
        setupMenu();
    }

    private void refreshMenuData() {
        menuDataManager.refreshData(this); // Pass 'this' as context
        setupMenu();
    }

    private void initializeViews() {
        floorTitleText = findViewById(R.id.floor_title_text);
        searchEditText = findViewById(R.id.search_edit_text);
        menuContainer = findViewById(R.id.menu_container);
        fabCart = findViewById(R.id.fab_cart);
        cartBadge = findViewById(R.id.cart_badge);

        floorTitleText.setText(floorName + " Menu");
    }

    private void setupMenu() {
        allMenuItems = menuDataManager.getAllMenuItems();
        filteredMenuItems = new ArrayList<>(allMenuItems);
        displayMenuItems(filteredMenuItems);
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMenuItems(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterMenuItems(String query) {
        filteredMenuItems.clear();

        if (query.isEmpty()) {
            filteredMenuItems.addAll(allMenuItems);
        } else {
            String lowerQuery = query.toLowerCase();
            for (MenuDataManager.MenuItem item : allMenuItems) {
                if (item.name.toLowerCase().contains(lowerQuery) ||
                        item.description.toLowerCase().contains(lowerQuery) ||
                        item.category.toLowerCase().contains(lowerQuery)) {
                    filteredMenuItems.add(item);
                }
            }
        }

        // Always display filtered results
        displayMenuItems(filteredMenuItems);
    }

    private void displayMenuItems(List<MenuDataManager.MenuItem> menuItems) {
        // ALWAYS clear the container first to prevent duplicates
        menuContainer.removeAllViews();

        if (menuItems.isEmpty()) {
            if (allMenuItems.isEmpty()) {
                // No items exist at all
                showEmptyMenuState();
            } else {
                // Items exist but search found nothing
                showNoSearchResultsState();
            }
            return;
        }

        // Show search bar when items are available
        searchEditText.setVisibility(View.VISIBLE);

        // Group items by category to prevent duplicate headers
        String currentCategory = "";

        for (MenuDataManager.MenuItem item : menuItems) {
            // Only add category header if it's different from the previous one
            if (!item.category.equals(currentCategory)) {
                currentCategory = item.category;
                addCategoryHeader(currentCategory);
            }
            addMenuItemCard(item);
        }
    }

    private void showEmptyMenuState() {
        // Hide search bar when no items exist
        searchEditText.setVisibility(View.GONE);

        // Create a layout for empty state
        LinearLayout emptyStateLayout = new LinearLayout(this);
        emptyStateLayout.setOrientation(LinearLayout.VERTICAL);
        emptyStateLayout.setGravity(android.view.Gravity.CENTER);
        emptyStateLayout.setPadding(dpToPx(32), dpToPx(64), dpToPx(32), dpToPx(64));

        // Empty state icon
        ImageView emptyIcon = new ImageView(this);
        emptyIcon.setImageResource(android.R.drawable.ic_menu_info_details);
        emptyIcon.setColorFilter(Color.parseColor("#cccccc"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(64), dpToPx(64));
        iconParams.setMargins(0, 0, 0, dpToPx(16));
        emptyIcon.setLayoutParams(iconParams);

        // Empty state title
        TextView emptyTitle = new TextView(this);
        emptyTitle.setText("No Menu Items Available");
        emptyTitle.setTextSize(20);
        emptyTitle.setTextColor(Color.parseColor("#666666"));
        emptyTitle.setTypeface(emptyTitle.getTypeface(), android.graphics.Typeface.BOLD);
        emptyTitle.setGravity(android.view.Gravity.CENTER);
        emptyTitle.setPadding(0, 0, 0, dpToPx(8));

        // Empty state message
        TextView emptyMessage = new TextView(this);
        emptyMessage.setText("The menu for this floor is not ready yet.\nPlease check back later!");
        emptyMessage.setTextSize(16);
        emptyMessage.setTextColor(Color.parseColor("#999999"));
        emptyMessage.setGravity(android.view.Gravity.CENTER);
        emptyMessage.setLineSpacing(dpToPx(4), 1.0f);

        // Add views to layout
        emptyStateLayout.addView(emptyIcon);
        emptyStateLayout.addView(emptyTitle);
        emptyStateLayout.addView(emptyMessage);

        // Add to main container
        menuContainer.addView(emptyStateLayout);
    }

    private void showNoSearchResultsState() {
        // Keep search bar visible
        searchEditText.setVisibility(View.VISIBLE);

        // Create layout for no search results
        LinearLayout noResultsLayout = new LinearLayout(this);
        noResultsLayout.setOrientation(LinearLayout.VERTICAL);
        noResultsLayout.setGravity(android.view.Gravity.CENTER);
        noResultsLayout.setPadding(dpToPx(32), dpToPx(64), dpToPx(32), dpToPx(64));

        // No results icon
        ImageView noResultsIcon = new ImageView(this);
        noResultsIcon.setImageResource(android.R.drawable.ic_menu_search);
        noResultsIcon.setColorFilter(Color.parseColor("#cccccc"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(64), dpToPx(64));
        iconParams.setMargins(0, 0, 0, dpToPx(16));
        noResultsIcon.setLayoutParams(iconParams);

        // No results title
        TextView noResultsTitle = new TextView(this);
        noResultsTitle.setText("No Items Found");
        noResultsTitle.setTextSize(20);
        noResultsTitle.setTextColor(Color.parseColor("#666666"));
        noResultsTitle.setTypeface(noResultsTitle.getTypeface(), android.graphics.Typeface.BOLD);
        noResultsTitle.setGravity(android.view.Gravity.CENTER);
        noResultsTitle.setPadding(0, 0, 0, dpToPx(8));

        // No results message
        TextView noResultsMessage = new TextView(this);
        noResultsMessage.setText("Try searching with different keywords");
        noResultsMessage.setTextSize(16);
        noResultsMessage.setTextColor(Color.parseColor("#999999"));
        noResultsMessage.setGravity(android.view.Gravity.CENTER);

        // Add views to layout
        noResultsLayout.addView(noResultsIcon);
        noResultsLayout.addView(noResultsTitle);
        noResultsLayout.addView(noResultsMessage);

        // Add to main container
        menuContainer.addView(noResultsLayout);
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
        cardView.setCardElevation(dpToPx(4));
        cardView.setRadius(dpToPx(12));
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setUseCompatPadding(true);

        // Set card layout params
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        cardView.setLayoutParams(cardParams);

        // Create main horizontal layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        // Create image view
        ImageView itemImage = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dpToPx(80), dpToPx(80));
        imageParams.setMargins(0, 0, dpToPx(12), 0);
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
        imageBackground.setCornerRadius(dpToPx(8));
        itemImage.setBackground(imageBackground);

        // Create content layout (vertical)
        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        // Item name
        TextView nameText = new TextView(this);
        nameText.setText(item.name);
        nameText.setTextSize(16);
        nameText.setTypeface(nameText.getTypeface(), android.graphics.Typeface.BOLD);
        nameText.setTextColor(Color.parseColor("#333333"));

        // Item description
        TextView descText = new TextView(this);
        descText.setText(item.description);
        descText.setTextSize(12);
        descText.setTextColor(Color.parseColor("#666666"));
        descText.setPadding(0, dpToPx(4), 0, dpToPx(8));

        // Price and order layout (horizontal)
        LinearLayout priceOrderLayout = new LinearLayout(this);
        priceOrderLayout.setOrientation(LinearLayout.HORIZONTAL);
        priceOrderLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Price text
        TextView priceText = new TextView(this);
        priceText.setText("₹" + (int) item.price);
        priceText.setTextSize(16);
        priceText.setTypeface(priceText.getTypeface(), android.graphics.Typeface.BOLD);
        priceText.setTextColor(Color.parseColor("#fc9432"));

        // Order button
        Button orderButton = new Button(this);
        orderButton.setText("ADD");
        orderButton.setTextSize(12);
        orderButton.setTypeface(orderButton.getTypeface(), android.graphics.Typeface.BOLD);
        orderButton.setTextColor(Color.WHITE);
        orderButton.setPadding(dpToPx(16), dpToPx(6), dpToPx(16), dpToPx(6));

        // Create button background
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(Color.parseColor("#fc9432"));
        buttonBackground.setCornerRadius(dpToPx(20));
        orderButton.setBackground(buttonBackground);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(dpToPx(12), 0, 0, 0);
        orderButton.setLayoutParams(buttonParams);

        // Stock text - NEW ADDITION
        TextView stockText = new TextView(this);
        stockText.setText("Available: " + item.stock + " items");
        stockText.setTextSize(12);
        stockText.setTextColor(item.stock > 5 ? Color.parseColor("#4CAF50") :
                item.stock > 0 ? Color.parseColor("#FF9800") : Color.parseColor("#F44336"));
        stockText.setPadding(0, dpToPx(2), 0, dpToPx(4));

        // Add order button click listener - UPDATED WITH STOCK MANAGEMENT
        orderButton.setOnClickListener(v -> {
            if (item.stock > 0) {
                // Check current stock from data manager (to get most recent value)
                int currentStock = menuDataManager.getItemStock(item.id);

                if (currentStock > 0) {
                    addToCart(item);
                    animateButtonClick(orderButton);

                    // Update the item's stock in memory
                    item.stock = currentStock - 1;

                    // Update the stock display immediately
                    stockText.setText("Available: " + item.stock + " items");
                    stockText.setTextColor(item.stock > 5 ? Color.parseColor("#4CAF50") :
                            item.stock > 0 ? Color.parseColor("#FF9800") : Color.parseColor("#F44336"));

                    // Disable button if out of stock
                    if (item.stock <= 0) {
                        orderButton.setText("OUT OF STOCK");
                        orderButton.setEnabled(false);
                        orderButton.setTextColor(Color.parseColor("#999999"));
                        GradientDrawable disabledBackground = new GradientDrawable();
                        disabledBackground.setColor(Color.parseColor("#CCCCCC"));
                        disabledBackground.setCornerRadius(dpToPx(20));
                        orderButton.setBackground(disabledBackground);
                    }
                } else {
                    Toast.makeText(this, "Item is out of stock!", Toast.LENGTH_SHORT).show();
                    // Refresh the display to show correct stock
                    refreshMenuData();
                }
            } else {
                Toast.makeText(this, "Item is out of stock!", Toast.LENGTH_SHORT).show();
            }
        });

        // Set initial button state based on current stock from data manager - NEW ADDITION
        int currentStock = menuDataManager.getItemStock(item.id);
        if (currentStock != item.stock) {
            // Update item stock if it differs from data manager
            item.stock = currentStock;
        }

        if (item.stock <= 0) {
            orderButton.setText("OUT OF STOCK");
            orderButton.setEnabled(false);
            orderButton.setTextColor(Color.parseColor("#999999"));
            GradientDrawable disabledBackground = new GradientDrawable();
            disabledBackground.setColor(Color.parseColor("#CCCCCC"));
            disabledBackground.setCornerRadius(dpToPx(20));
            orderButton.setBackground(disabledBackground);
        }

        // Add views to layouts
        priceOrderLayout.addView(priceText);
        priceOrderLayout.addView(orderButton);

        contentLayout.addView(nameText);
        contentLayout.addView(descText);
        contentLayout.addView(stockText); // NEW LINE ADDED
        contentLayout.addView(priceOrderLayout);

        mainLayout.addView(itemImage);
        mainLayout.addView(contentLayout);

        cardView.addView(mainLayout);

        // Add card entrance animation (only for initial load, not for search results)
        if (searchEditText.getText().toString().trim().isEmpty()) {
            cardView.setAlpha(0f);
            cardView.setTranslationX(100f);

            new Handler().postDelayed(() -> {
                ObjectAnimator alpha = ObjectAnimator.ofFloat(cardView, "alpha", 0f, 1f);
                ObjectAnimator translation = ObjectAnimator.ofFloat(cardView, "translationX", 100f, 0f);
                alpha.setDuration(400);
                translation.setDuration(400);
                alpha.setInterpolator(new DecelerateInterpolator());
                translation.setInterpolator(new DecelerateInterpolator());

                AnimatorSet animSet = new AnimatorSet();
                animSet.playTogether(alpha, translation);
                animSet.start();
            }, menuContainer.getChildCount() * 50);
        }

        menuContainer.addView(cardView);
    }

    private void addToCart(MenuDataManager.MenuItem item) {
        menuDataManager.decreaseStock(item.id, 1); // NEW LINE ADDED
        // Check if item already exists in cart
        boolean found = false;
        for (CartItem cartItem : cartItems) {
            if (cartItem.menuItem.id == item.id) {
                cartItem.quantity++;
                found = true;
                break;
            }
        }

        if (!found) {
            cartItems.add(new CartItem(item, 1));
        }

        totalCartItems++;
        updateCartBadge();

        Toast.makeText(this, item.name + " added to cart!", Toast.LENGTH_SHORT).show();
    }

    private void updateCartBadge() {
        if (totalCartItems > 0) {
            cartBadge.setVisibility(View.VISIBLE);
            cartBadge.setText(String.valueOf(totalCartItems));

            // Animate badge
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(cartBadge, "scaleX", 1.2f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(cartBadge, "scaleY", 1.2f, 1f);
            scaleX.setDuration(200);
            scaleY.setDuration(200);

            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(scaleX, scaleY);
            animSet.start();
        } else {
            cartBadge.setVisibility(View.GONE);
        }
    }

    private void animateButtonClick(Button button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.9f, 1f);
        scaleX.setDuration(150);
        scaleY.setDuration(150);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.start();
    }

    private void setupCartFunctionality() {
        fabCart.setOnClickListener(v -> {
            if (totalCartItems > 0) {
                try {
                    // Convert local cart items to generic format for CartActivity
                    ArrayList<CartActivity.CartItem> genericCart = new ArrayList<>();
                    for (CartItem item : cartItems) {
                        CartActivity.MenuItem genericMenuItem = new CartActivity.MenuItem(
                                item.menuItem.name,
                                item.menuItem.description,
                                item.menuItem.price,
                                item.menuItem.imageRes
                        );
                        CartActivity.CartItem genericCartItem = new CartActivity.CartItem(
                                genericMenuItem,
                                item.quantity,
                                floorName
                        );
                        genericCart.add(genericCartItem);
                    }

                    Intent cartIntent = new Intent(this, CartActivity.class);
                    cartIntent.putExtra("cart_items", genericCart);
                    startActivity(cartIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Error opening cart: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startEntranceAnimations() {
        // Title animation
        ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(floorTitleText, "alpha", 0f, 1f);
        ObjectAnimator titleTranslation = ObjectAnimator.ofFloat(floorTitleText, "translationY", -50f, 0f);
        titleAlpha.setDuration(600);
        titleTranslation.setDuration(600);

        AnimatorSet titleAnimSet = new AnimatorSet();
        titleAnimSet.playTogether(titleAlpha, titleTranslation);
        titleAnimSet.start();

        // Search bar animation
        new Handler().postDelayed(() -> {
            ObjectAnimator searchAlpha = ObjectAnimator.ofFloat(searchEditText, "alpha", 0f, 1f);
            ObjectAnimator searchTranslation = ObjectAnimator.ofFloat(searchEditText, "translationY", -30f, 0f);
            searchAlpha.setDuration(400);
            searchTranslation.setDuration(400);

            AnimatorSet searchAnimSet = new AnimatorSet();
            searchAnimSet.playTogether(searchAlpha, searchTranslation);
            searchAnimSet.start();
        }, 200);

        // FAB animation
        new Handler().postDelayed(() -> {
            ObjectAnimator fabScale = ObjectAnimator.ofFloat(fabCart, "scaleX", 0f, 1f);
            ObjectAnimator fabScaleY = ObjectAnimator.ofFloat(fabCart, "scaleY", 0f, 1f);
            fabScale.setDuration(400);
            fabScaleY.setDuration(400);

            AnimatorSet fabAnimSet = new AnimatorSet();
            fabAnimSet.playTogether(fabScale, fabScaleY);
            fabAnimSet.start();
        }, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save current cart state for potential stock restoration
        saveCartStateForStockRestoration();
    }

    private void saveCartStateForStockRestoration() {
        SharedPreferences prefs = getSharedPreferences("TempCart", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Save cart items and timestamp
        Gson gson = new Gson();
        String cartJson = gson.toJson(cartItems);
        editor.putString("pending_cart_" + floorName, cartJson);
        editor.putLong("cart_timestamp_" + floorName, System.currentTimeMillis());
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // If user closes the app without completing order, we need to handle this
        // This is handled in the next solution for CartActivity
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