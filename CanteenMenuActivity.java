package com.example.shashankscreen;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class CanteenMenuActivity extends AppCompatActivity {

    private TextView floorTitleText;
    private LinearLayout menuContainer;
    private FloatingActionButton fabCart;
    private TextView cartBadge;
    private String floorName;
    private List<CartItem> cartItems;
    private int totalCartItems = 0;

    // Menu items data structure
    private static class MenuItem {
        String name;
        String description;
        double price;
        int imageRes;
        String category;

        MenuItem(String name, String description, double price, int imageRes, String category) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.imageRes = imageRes;
            this.category = category;
        }
    }

    // Cart item data structure
    private static class CartItem {
        MenuItem menuItem;
        int quantity;

        CartItem(MenuItem menuItem, int quantity) {
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
        if (floorName == null) floorName = "Menu";

        // Initialize cart
        cartItems = new ArrayList<>();

        // Initialize views
        initializeViews();

        // Setup menu based on floor
        setupMenu();

        // Setup cart functionality
        setupCartFunctionality();

        // Start entrance animations
        startEntranceAnimations();
    }

    private void initializeViews() {
        floorTitleText = findViewById(R.id.floor_title_text);
        menuContainer = findViewById(R.id.menu_container);
        fabCart = findViewById(R.id.fab_cart);
        cartBadge = findViewById(R.id.cart_badge);

        floorTitleText.setText(floorName + " Menu");
    }

    private void setupMenu() {
        List<MenuItem> menuItems = getMenuItems();
        String currentCategory = "";

        for (MenuItem item : menuItems) {
            // Add category header if it's a new category
            if (!item.category.equals(currentCategory)) {
                currentCategory = item.category;
                addCategoryHeader(currentCategory);
            }

            // Add menu item card
            addMenuItemCard(item);
        }
    }

    private List<MenuItem> getMenuItems() {
        List<MenuItem> items = new ArrayList<>();

        // Main Course items
        items.add(new MenuItem("Chicken Biryani", "Aromatic basmati rice with tender chicken", 180.0,
                R.drawable.khandvi, "Main Course"));
        items.add(new MenuItem("Mutton Curry", "Spicy mutton curry with rice", 220.0,
                R.drawable.p, "Main Course"));
        items.add(new MenuItem("Veg Thali", "Complete vegetarian meal with variety", 120.0,
                R.drawable.veg_thali, "Main Course"));
        items.add(new MenuItem("Fish Fry", "Crispy fried fish with special marinade", 160.0,
                R.drawable.vadapav, "Main Course"));

        return items;
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

    private void addMenuItemCard(MenuItem item) {
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
        itemImage.setImageResource(item.imageRes);
        itemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);


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
        priceText.setText("₹" + String.format("%.0f", item.price));
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

        // Add order button click listener
        orderButton.setOnClickListener(v -> {
            addToCart(item);
            animateButtonClick(orderButton);
        });

        // Add views to layouts
        priceOrderLayout.addView(priceText);
        priceOrderLayout.addView(orderButton);

        contentLayout.addView(nameText);
        contentLayout.addView(descText);
        contentLayout.addView(priceOrderLayout);

        mainLayout.addView(itemImage);
        mainLayout.addView(contentLayout);

        cardView.addView(mainLayout);

        // Add card entrance animation
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

        menuContainer.addView(cardView);
    }

    private void addToCart(MenuItem item) {
        // Check if item already exists in cart
        boolean found = false;
        for (CartItem cartItem : cartItems) {
            if (cartItem.menuItem.name.equals(item.name)) {
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
                // Navigate to cart activity (you'll need to create this)
                Intent cartIntent = new Intent(this, CartActivity.class);
                cartIntent.putExtra("cart_items", (ArrayList<CartItem>) cartItems);
                startActivity(cartIntent);
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
