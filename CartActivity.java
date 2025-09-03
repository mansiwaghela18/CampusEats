package com.example.shashankscreen;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CartActivity extends AppCompatActivity {

    // UPI Payment Constants - FIXED
    private static final String RAZORPAY_PAYMENT_LINK = "https://razorpay.me/@phalgunimaheshwaghela";
    private static final int RAZORPAY_PAYMENT_REQUEST_CODE = 2001; // Your business UPI ID; // Use a standard UPI ID format
    private static final String UPI_PAYEE_NAME = "Canteen"; // Simplified name
    private static final String UPI_PAYMENT_NOTE = "Food Order"; // Simple note without special chars


    // Existing constants
    private static final int REQUEST_WRITE_PERMISSION = 1001;

    // Generic CartItem class that works for all floors
    public static class CartItem implements Serializable {
        public MenuItem menuItem;
        public int quantity;
        public String floorSource;

        public CartItem(MenuItem menuItem, int quantity, String floorSource) {
            this.menuItem = menuItem;
            this.quantity = quantity;
            this.floorSource = floorSource;
        }
    }

    // Generic MenuItem class
    public static class MenuItem implements Serializable {
        public String name;
        public String description;
        public double price;
        public int imageRes;

        public MenuItem(String name, String description, double price, int imageRes) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.imageRes = imageRes;
        }
    }

    // Bill class to store order details
    public static class Bill implements Serializable {
        public String orderId;
        public String orderDate;
        public String orderTime;
        public ArrayList<CartItem> items;
        public double subtotal;
        public double tax;
        public double total;
        public String paymentMethod;
        public String customerName;
        public String paymentStatus;
        public String transactionId;

        public Bill() {
            this.orderId = generateOrderId();
            Date now = new Date();
            this.orderDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now);
            this.orderTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now);
            this.paymentMethod = "UPI Payment";
            this.customerName = "Student";
            this.paymentStatus = "Pending";
            this.transactionId = "";
        }

        private String generateOrderId() {
            // FIXED: Generate shorter, simpler order ID
            return "ORD" + (System.currentTimeMillis() % 100000);
        }
    }

    // UPI Payment Status Enum
    public enum UpiPaymentStatus {
        SUCCESS,
        FAILED,
        CANCELLED,
        ERROR
    }

    private TextView cartTitleText;
    private EditText searchEditText;
    private LinearLayout cartContainer;
    private TextView totalPriceText;
    private TextView totalItemsText;
    private Button payNowButton;
    private TextView emptyCartText;
    private TextView noResultsText;
    private ArrayList<CartItem> cartItems;
    private ArrayList<CartItem> filteredCartItems;
    private double totalPrice = 0.0;
    private int totalItems = 0;
    private Bill currentBill;
    private MenuDataManager menuDataManager;
    private OrderNotificationManager orderNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        menuDataManager = MenuDataManager.getInstance(this);
        orderNotificationManager = OrderNotificationManager.getInstance(this);

        // Get cart items from intent
        cartItems = (ArrayList<CartItem>) getIntent().getSerializableExtra("cart_items");
        if (cartItems == null) {
            cartItems = new ArrayList<>();
        }

        // Initialize filtered list with all items
        filteredCartItems = new ArrayList<>(cartItems);

        // Initialize views
        initializeViews();

        // Setup search functionality
        setupSearchFunctionality();

        // Setup cart display
        setupCartDisplay();

        // Setup pay now functionality with UPI
        setupPayNowFunctionality();

        // Start entrance animations
        startEntranceAnimations();
    }


    private void initializeViews() {
        cartTitleText = findViewById(R.id.cart_title_text);
        searchEditText = findViewById(R.id.search_edit_text);
        cartContainer = findViewById(R.id.cart_container);
        totalPriceText = findViewById(R.id.total_price_text);
        totalItemsText = findViewById(R.id.total_items_text);
        payNowButton = findViewById(R.id.pay_now_button);
        emptyCartText = findViewById(R.id.empty_cart_text);
        noResultsText = findViewById(R.id.no_results_text);

        // Back button functionality
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterCartItems(s.toString().trim());
            }
        });
    }

    private void filterCartItems(String query) {
        filteredCartItems.clear();

        if (query.isEmpty()) {
            filteredCartItems.addAll(cartItems);
        } else {
            String lowerQuery = query.toLowerCase();
            for (CartItem item : cartItems) {
                if (item.menuItem.name.toLowerCase().contains(lowerQuery) ||
                        item.menuItem.description.toLowerCase().contains(lowerQuery)) {
                    filteredCartItems.add(item);
                }
            }
        }

        refreshCartDisplay();
    }

    private void refreshCartDisplay() {
        cartContainer.removeAllViews();

        if (cartItems.isEmpty()) {
            showEmptyCart();
            return;
        }

        if (filteredCartItems.isEmpty()) {
            showNoSearchResults();
            return;
        }

        noResultsText.setVisibility(View.GONE);
        emptyCartText.setVisibility(View.GONE);

        for (int i = 0; i < filteredCartItems.size(); i++) {
            CartItem cartItem = filteredCartItems.get(i);
            int originalIndex = cartItems.indexOf(cartItem);
            addCartItemCard(cartItem, originalIndex);
        }

        updateTotalDisplay();
    }

    private void setupCartDisplay() {
        if (cartItems.isEmpty()) {
            showEmptyCart();
            return;
        }

        totalPrice = 0.0;
        totalItems = 0;

        for (int i = 0; i < filteredCartItems.size(); i++) {
            CartItem cartItem = filteredCartItems.get(i);
            int originalIndex = cartItems.indexOf(cartItem);
            addCartItemCard(cartItem, originalIndex);
            totalPrice += cartItem.menuItem.price * cartItem.quantity;
            totalItems += cartItem.quantity;
        }

        updateTotalDisplay();
    }

    private void showEmptyCart() {
        emptyCartText.setVisibility(View.VISIBLE);
        noResultsText.setVisibility(View.GONE);
        cartContainer.setVisibility(View.VISIBLE);
        payNowButton.setVisibility(View.GONE);
        totalPriceText.setVisibility(View.GONE);
        totalItemsText.setVisibility(View.GONE);
        searchEditText.setVisibility(View.GONE);
    }

    private void showNoSearchResults() {
        emptyCartText.setVisibility(View.GONE);
        noResultsText.setVisibility(View.VISIBLE);
        payNowButton.setVisibility(View.VISIBLE);
        totalPriceText.setVisibility(View.VISIBLE);
        totalItemsText.setVisibility(View.VISIBLE);
        updateTotalDisplay();
    }

    private void addCartItemCard(CartItem cartItem, int position) {
        CardView cardView = new CardView(this);
        cardView.setCardElevation(dpToPx(4));
        cardView.setRadius(dpToPx(12));
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setUseCompatPadding(true);
        cardView.setTag(position);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        cardView.setLayoutParams(cardParams);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        ImageView itemImage = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dpToPx(70), dpToPx(70));
        imageParams.setMargins(0, 0, dpToPx(12), 0);
        itemImage.setLayoutParams(imageParams);
        itemImage.setImageResource(cartItem.menuItem.imageRes);
        itemImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        GradientDrawable imageBackground = new GradientDrawable();
        imageBackground.setColor(Color.parseColor("#FFF8F3"));
        imageBackground.setCornerRadius(dpToPx(8));
        itemImage.setBackground(imageBackground);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView nameText = new TextView(this);
        nameText.setText(cartItem.menuItem.name);
        nameText.setTextSize(16);
        nameText.setTypeface(nameText.getTypeface(), android.graphics.Typeface.BOLD);
        nameText.setTextColor(Color.parseColor("#333333"));

        TextView floorText = new TextView(this);
        floorText.setText("From " + cartItem.floorSource);
        floorText.setTextSize(10);
        floorText.setTextColor(Color.parseColor("#999999"));
        floorText.setPadding(0, dpToPx(2), 0, 0);

        TextView descText = new TextView(this);
        descText.setText(cartItem.menuItem.description);
        descText.setTextSize(12);
        descText.setTextColor(Color.parseColor("#666666"));
        descText.setPadding(0, dpToPx(4), 0, dpToPx(8));

        LinearLayout priceQuantityLayout = new LinearLayout(this);
        priceQuantityLayout.setOrientation(LinearLayout.HORIZONTAL);
        priceQuantityLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView priceText = new TextView(this);
        priceText.setText("₹" + String.format("%.0f", cartItem.menuItem.price));
        priceText.setTextSize(14);
        priceText.setTextColor(Color.parseColor("#fc9432"));
        priceText.setTypeface(priceText.getTypeface(), android.graphics.Typeface.BOLD);

        LinearLayout quantityLayout = new LinearLayout(this);
        quantityLayout.setOrientation(LinearLayout.HORIZONTAL);
        quantityLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams quantityParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        quantityParams.setMargins(dpToPx(12), 0, 0, 0);
        quantityLayout.setLayoutParams(quantityParams);

        Button decreaseButton = createQuantityButton("-");
        decreaseButton.setOnClickListener(v -> {
            if (cartItem.quantity > 1) {
                // Only decrease quantity and restore 1 stock
                cartItem.quantity--;
                menuDataManager.increaseStockByName(cartItem.menuItem.name, cartItem.floorSource, 1);

                // Find and update the TextViews
                TextView qtyText = cartContainer.findViewWithTag("quantity_" + position);
                TextView totalText = cartContainer.findViewWithTag("total_" + position);

                if (qtyText != null && totalText != null) {
                    qtyText.setText(String.valueOf(cartItem.quantity));
                    double itemTotal = cartItem.menuItem.price * cartItem.quantity;
                    totalText.setText("₹" + String.format("%.0f", itemTotal));
                    animateTextUpdate(qtyText);
                    animateTextUpdate(totalText);
                }

                updateTotalDisplay();
                android.util.Log.d("CartActivity", "Decreased " + cartItem.menuItem.name + " to " + cartItem.quantity + ", restored 1 stock");
            } else {
                // Remove item completely
                menuDataManager.increaseStockByName(cartItem.menuItem.name, cartItem.floorSource, 1);
                removeCartItem(position);
                android.util.Log.d("CartActivity", "Removed " + cartItem.menuItem.name + " from cart, restored 1 stock");
            }
        });

        TextView quantityText = new TextView(this);
        quantityText.setText(String.valueOf(cartItem.quantity));
        quantityText.setTextSize(16);
        quantityText.setTextColor(Color.parseColor("#333333"));
        quantityText.setTypeface(quantityText.getTypeface(), android.graphics.Typeface.BOLD);
        quantityText.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        quantityText.setTag("quantity_" + position);

        Button increaseButton = createQuantityButton("+");
        increaseButton.setOnClickListener(v -> {
            // Check current stock before allowing increase
            int currentStock = menuDataManager.getCurrentStock(cartItem.menuItem.name, cartItem.floorSource);

            if (currentStock > 0) {
                // Decrease stock in the menu
                boolean stockDecreased = menuDataManager.decreaseStockByName(cartItem.menuItem.name, cartItem.floorSource, 1);

                if (stockDecreased) {
                    cartItem.quantity++;

                    // Find and update the TextViews
                    TextView qtyText = cartContainer.findViewWithTag("quantity_" + position);
                    TextView totalText = cartContainer.findViewWithTag("total_" + position);

                    if (qtyText != null && totalText != null) {
                        qtyText.setText(String.valueOf(cartItem.quantity));
                        double itemTotal = cartItem.menuItem.price * cartItem.quantity;
                        totalText.setText("₹" + String.format("%.0f", itemTotal));
                        animateTextUpdate(qtyText);
                        animateTextUpdate(totalText);
                    }

                    updateTotalDisplay();
                    Toast.makeText(this, "Item added to cart", Toast.LENGTH_SHORT).show();
                    android.util.Log.d("CartActivity", "Increased " + cartItem.menuItem.name + " to " + cartItem.quantity + ", decreased stock by 1");
                } else {
                    Toast.makeText(this, cartItem.menuItem.name + " - Failed to add item!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, cartItem.menuItem.name + " is out of stock!", Toast.LENGTH_LONG).show();
                android.util.Log.w("CartActivity", cartItem.menuItem.name + " is out of stock, cannot increase quantity");
            }
        });

        TextView itemTotalText = new TextView(this);
        double itemTotal = cartItem.menuItem.price * cartItem.quantity;
        itemTotalText.setText("₹" + String.format("%.0f", itemTotal));
        itemTotalText.setTextSize(16);
        itemTotalText.setTextColor(Color.parseColor("#fc9432"));
        itemTotalText.setTypeface(itemTotalText.getTypeface(), android.graphics.Typeface.BOLD);
        itemTotalText.setTag("total_" + position);

        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        totalParams.setMargins(dpToPx(12), 0, 0, 0);
        itemTotalText.setLayoutParams(totalParams);

        quantityLayout.addView(decreaseButton);
        quantityLayout.addView(quantityText);
        quantityLayout.addView(increaseButton);

        priceQuantityLayout.addView(priceText);
        priceQuantityLayout.addView(quantityLayout);
        priceQuantityLayout.addView(itemTotalText);

        contentLayout.addView(nameText);
        contentLayout.addView(floorText);
        contentLayout.addView(descText);
        contentLayout.addView(priceQuantityLayout);

        mainLayout.addView(itemImage);
        mainLayout.addView(contentLayout);

        cardView.addView(mainLayout);
        cartContainer.addView(cardView);

        cardView.setAlpha(0f);
        cardView.setTranslationX(100f);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(cardView, "alpha", 0f, 1f);
        ObjectAnimator translation = ObjectAnimator.ofFloat(cardView, "translationX", 100f, 0f);
        alpha.setDuration(300);
        translation.setDuration(300);
        alpha.setInterpolator(new DecelerateInterpolator());
        translation.setInterpolator(new DecelerateInterpolator());

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(alpha, translation);
        animSet.setStartDelay(position * 50);
        animSet.start();
    }

    private void updateSingleItemDisplay(int position, CartItem cartItem) {
        TextView quantityText = cartContainer.findViewWithTag("quantity_" + position);
        TextView itemTotalText = cartContainer.findViewWithTag("total_" + position);

        if (quantityText != null && itemTotalText != null) {
            quantityText.setText(String.valueOf(cartItem.quantity));

            double itemTotal = cartItem.menuItem.price * cartItem.quantity;
            itemTotalText.setText("₹" + String.format("%.0f", itemTotal));

            animateTextUpdate(quantityText);
            animateTextUpdate(itemTotalText);
        }

        updateTotalDisplay();

        // IMPORTANT: Don't call any stock methods here - they're already handled in the click listeners
    }

    private void animateTextUpdate(TextView textView) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(textView, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(textView, "scaleY", 1f, 1.1f, 1f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.start();
    }

    private Button createQuantityButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setTextColor(Color.parseColor("#fc9432"));
        button.setTypeface(button.getTypeface(), android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dpToPx(35), dpToPx(35));
        button.setLayoutParams(buttonParams);

        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(Color.parseColor("#FFF8F3"));
        buttonBackground.setCornerRadius(dpToPx(18));
        buttonBackground.setStroke(dpToPx(1), Color.parseColor("#fc9432"));
        button.setBackground(buttonBackground);

        return button;
    }

    private void removeCartItem(int position) {
        if (position >= 0 && position < cartItems.size()) {
            CartItem removedItem = cartItems.get(position);

            // Restore stock for the removed item (only once)
            boolean stockRestored = menuDataManager.increaseStockByName(
                    removedItem.menuItem.name,
                    removedItem.floorSource,
                    removedItem.quantity
            );

            if (stockRestored) {
                android.util.Log.d("CartActivity", "Restored " + removedItem.quantity +
                        " items of " + removedItem.menuItem.name + " back to " + removedItem.floorSource);
            }

            // Remove from cart
            cartItems.remove(position);

            // Update filtered list
            filteredCartItems.clear();
            String currentQuery = searchEditText.getText().toString().trim();
            if (currentQuery.isEmpty()) {
                filteredCartItems.addAll(cartItems);
            } else {
                filterCartItems(currentQuery);
                return; // filterCartItems will call rebuildCartDisplay
            }
            rebuildCartDisplay();
        }
    }

    private void rebuildCartDisplay() {
        cartContainer.removeAllViews();

        if (cartItems.isEmpty()) {
            showEmptyCart();
            return;
        }

        totalPrice = 0.0;
        totalItems = 0;

        for (int i = 0; i < filteredCartItems.size(); i++) {
            CartItem cartItem = filteredCartItems.get(i);
            int originalIndex = cartItems.indexOf(cartItem);
            addCartItemCard(cartItem, originalIndex);
            totalPrice += cartItem.menuItem.price * cartItem.quantity;
            totalItems += cartItem.quantity;
        }

        updateTotalDisplay();
    }

    private void updateTotalDisplay() {
        totalPrice = 0.0;
        totalItems = 0;

        for (CartItem cartItem : cartItems) {
            totalPrice += cartItem.menuItem.price * cartItem.quantity;
            totalItems += cartItem.quantity;
        }

        totalItemsText.setText(totalItems + " items");
        totalPriceText.setText("₹" + String.format("%.0f", totalPrice));

        totalItemsText.setVisibility(View.VISIBLE);
        totalPriceText.setVisibility(View.VISIBLE);
        payNowButton.setVisibility(View.VISIBLE);
    }

    // Updated Pay Now functionality with UPI integration
    private void setupPayNowFunctionality() {
        payNowButton.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            animateButtonClick(payNowButton);
            showOrderReviewDialog();
        });
    }

    private void showOrderReviewDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("📋 Review Your Order");

        StringBuilder orderSummary = new StringBuilder();
        orderSummary.append("🛍️ Order Summary\n");
        orderSummary.append("Total Items: ").append(totalItems).append("\n");
        orderSummary.append("Total Amount: ₹").append(String.format("%.0f", totalPrice)).append("\n\n");

        orderSummary.append("📦 Items Details:\n");
        String currentFloor = "";
        for (CartItem item : cartItems) {
            if (!item.floorSource.equals(currentFloor)) {
                currentFloor = item.floorSource;
                orderSummary.append("\n🏢 ").append(currentFloor).append(":\n");
            }
            orderSummary.append("• ").append(item.menuItem.name)
                    .append(" × ").append(item.quantity)
                    .append(" = ₹").append(String.format("%.0f", item.menuItem.price * item.quantity)).append("\n");
        }

        orderSummary.append("\n✅ Everything looks correct?");
        builder.setMessage(orderSummary.toString());

        builder.setPositiveButton("💳 Pay with Razorpay", (dialog, which) -> {
            dialog.dismiss();
            generateBill();
            initiateRazorpayPayment(); // Changed from initiateUpiPayment()
        });

        builder.setNegativeButton("Edit Order", (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        customizeDialogAppearance(dialog);
    }

    // FIXED UPI Payment Methods
    private void initiateRazorpayPayment() {
        try {
            // Generate bill first
            generateBill();

            // Check if browser is available
            if (!isBrowserAvailable()) {
                showBrowserNotAvailableDialog();
                return;
            }

            // Show payment confirmation dialog
            showRazorpayPaymentDialog();

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("RAZORPAY_PAYMENT", "Error initiating payment: " + e.getMessage());
            Toast.makeText(this, "Error initiating payment: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isBrowserAvailable() {
        return true; // Always assume browser is available
    }


    // FIXED: Corrected UPI intent creation
    private Intent createRazorpayIntent() {
        try {
            // Method 1: Use the payment link without amount parameter (user enters amount manually)
            String paymentUrl = RAZORPAY_PAYMENT_LINK; // Just the base link

            android.util.Log.d("RAZORPAY_PAYMENT", "Payment URL: " + paymentUrl);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl));
            return intent;

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("RAZORPAY_PAYMENT", "Error creating Razorpay intent: " + e.getMessage());
            return null;
        }
    }

    private void showRazorpayPaymentDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("💳 Razorpay Payment");

        StringBuilder paymentInfo = new StringBuilder();
        paymentInfo.append("🏪 Payee: Phalguni Maheshwaghela\n");
        paymentInfo.append("💰 Amount: ₹").append(String.format("%.2f", currentBill.total)).append("\n");
        paymentInfo.append("🎯 Order ID: ").append(currentBill.orderId).append("\n\n");
        paymentInfo.append("⚠️ Important Notes:\n");
        paymentInfo.append("• You'll be redirected to Razorpay payment page\n");
        paymentInfo.append("• Enter the exact amount shown above\n");
        paymentInfo.append("• Complete payment within 10 minutes\n");
        paymentInfo.append("• Return to app after payment completion\n\n");
        paymentInfo.append("Continue with Razorpay payment?");

        builder.setMessage(paymentInfo.toString());

        builder.setPositiveButton("🌐 Open Payment Page", (dialog, which) -> {
            dialog.dismiss();
            try {
                Intent razorpayIntent = createRazorpayIntent();
                if (razorpayIntent != null) {
                    startActivityForResult(razorpayIntent, RAZORPAY_PAYMENT_REQUEST_CODE);
                    // Start a timer to check payment status
                    startPaymentStatusCheck();
                } else {
                    Toast.makeText(this, "Unable to create payment link", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Unable to open payment page: " + e.getMessage(), Toast.LENGTH_LONG).show();
                showPaymentFailedDialog("Payment page not available");
            }
        });

        builder.setNegativeButton("❌ Cancel", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(this, "Payment cancelled", Toast.LENGTH_SHORT).show();
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        customizeDialogAppearance(dialog);
    }
    private boolean isValidUpiId(String upiId) {
        return upiId != null &&
                upiId.matches("^[a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+$") &&
                upiId.contains("@");
    }

    private void showBrowserNotAvailableDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("❌ Browser Not Available");
        builder.setMessage("No browser app found on your device.\n\n" +
                "Please install a browser app like:\n" +
                "• Google Chrome\n" +
                "• Mozilla Firefox\n" +
                "• Samsung Internet\n" +
                "• Any other browser\n\n" +
                "Then try again.");

        builder.setPositiveButton("📱 Go to Play Store", (dialog, which) -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/search?q=browser"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open Play Store", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        builder.setNegativeButton("❌ Cancel", (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        customizeDialogAppearance(dialog);
    }
    private void startPaymentStatusCheck() {
        // Show a dialog asking user to confirm payment completion
        new android.os.Handler().postDelayed(() -> {
            if (!isFinishing()) {
                showPaymentCompletionDialog();
            }
        }, 5000); // Wait 5 seconds before asking
    }
    private void showPaymentCompletionDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("💳 Payment Status");
        builder.setMessage("Have you completed the payment on Razorpay?\n\n" +
                "Order ID: " + currentBill.orderId + "\n" +
                "Amount: ₹" + String.format("%.2f", currentBill.total) + "\n\n" +
                "Please confirm your payment status:");

        builder.setPositiveButton("✅ Payment Completed", (dialog, which) -> {
            dialog.dismiss();
            // Assume payment is successful and show success dialog
            currentBill.paymentStatus = "Success";
            currentBill.transactionId = "RZP_" + System.currentTimeMillis();
            showPaymentSuccessDialog(currentBill.transactionId);
        });

        builder.setNegativeButton("❌ Payment Failed", (dialog, which) -> {
            dialog.dismiss();
            currentBill.paymentStatus = "Failed";
            showPaymentFailedDialog("Payment was not completed successfully");
        });

        builder.setNeutralButton("⏱️ Still Processing", (dialog, which) -> {
            dialog.dismiss();
            // Wait another 10 seconds and ask again
            new android.os.Handler().postDelayed(() -> {
                if (!isFinishing()) {
                    showPaymentCompletionDialog();
                }
            }, 10000);
            Toast.makeText(this, "We'll check again in 10 seconds...", Toast.LENGTH_SHORT).show();
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
        customizeDialogAppearance(dialog);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RAZORPAY_PAYMENT_REQUEST_CODE) {
            // For Razorpay link, we can't get automatic status
            // The payment completion dialog will handle status confirmation
            android.util.Log.d("RAZORPAY_PAYMENT", "Returned from Razorpay page");
        }
    }


    private void handleUpiPaymentResult(int resultCode, Intent data) {
        try {
            android.util.Log.d("UPI_PAYMENT", "Result Code: " + resultCode);

            if (data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    for (String key : extras.keySet()) {
                        android.util.Log.d("UPI_PAYMENT", key + ": " + extras.get(key));
                    }
                }
            }

            String status = null;
            String txnId = null;
            String responseCode = null;

            if (data != null) {
                status = data.getStringExtra("Status");
                txnId = data.getStringExtra("txnId");
                responseCode = data.getStringExtra("responseCode");

                // Try alternative parameter names
                if (status == null) {
                    status = data.getStringExtra("status");
                }
                if (txnId == null) {
                    txnId = data.getStringExtra("TransactionId");
                }
                if (responseCode == null) {
                    responseCode = data.getStringExtra("ResponseCode");
                }
            }

            // Handle payment result
            if (resultCode == RESULT_CANCELED || data == null) {
                currentBill.paymentStatus = "Cancelled";
                showPaymentCancelledDialog();
                return;
            }

            UpiPaymentStatus paymentStatus = determinePaymentStatus(status, responseCode, resultCode);

            // Update transaction ID
            if (txnId != null && !txnId.trim().isEmpty()) {
                currentBill.transactionId = txnId;
            } else {
                currentBill.transactionId = "N/A";
            }

            switch (paymentStatus) {
                case SUCCESS:
                    currentBill.paymentStatus = "Success";
                    showPaymentSuccessDialog(currentBill.transactionId);
                    break;
                case FAILED:
                    currentBill.paymentStatus = "Failed";
                    showPaymentFailedDialog("Payment failed. Please try again.");
                    break;
                case CANCELLED:
                    currentBill.paymentStatus = "Cancelled";
                    showPaymentCancelledDialog();
                    break;
                default:
                    currentBill.paymentStatus = "Error";
                    showPaymentErrorDialog("Payment status unclear. Please contact support if money was debited.");
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            currentBill.paymentStatus = "Error";
            showPaymentErrorDialog("Error processing payment result: " + e.getMessage());
        }
    }


    private UpiPaymentStatus determinePaymentStatus(String status, String responseCode, int resultCode) {
        // Check result code first
        if (resultCode == RESULT_CANCELED) {
            return UpiPaymentStatus.CANCELLED;
        }

        if (resultCode != RESULT_OK) {
            return UpiPaymentStatus.FAILED;
        }

        // Check response code (most reliable)
        if (responseCode != null && !responseCode.trim().isEmpty()) {
            responseCode = responseCode.trim();

            switch (responseCode) {
                case "00":
                case "0":
                    return UpiPaymentStatus.SUCCESS;
                case "U16": // User cancelled
                case "U69": // User cancelled
                case "ZA":  // User cancelled
                    return UpiPaymentStatus.CANCELLED;
                default:
                    return UpiPaymentStatus.FAILED;
            }
        }

        // Check status string
        if (status != null && !status.trim().isEmpty()) {
            status = status.toUpperCase().trim();

            switch (status) {
                case "SUCCESS":
                case "SUBMITTED":
                    return UpiPaymentStatus.SUCCESS;
                case "CANCELLED":
                case "CANCEL":
                    return UpiPaymentStatus.CANCELLED;
                case "FAILED":
                case "FAILURE":
                default:
                    return UpiPaymentStatus.FAILED;
            }
        }

        // If no status information available, assume cancelled
        return UpiPaymentStatus.CANCELLED;
    }


    private void showPaymentSuccessDialog(String transactionId) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🎉 Payment Successful!");

        StringBuilder successMessage = new StringBuilder();
        successMessage.append("✅ Your payment has been completed successfully!\n\n");
        successMessage.append("📋 Payment Details:\n");
        successMessage.append("Order ID: ").append(currentBill.orderId).append("\n");
        successMessage.append("Transaction ID: ").append(transactionId.isEmpty() ? "N/A" : transactionId).append("\n");
        successMessage.append("Amount Paid: ₹").append(String.format("%.2f", currentBill.total)).append("\n");
        successMessage.append("Payment Method: UPI\n");
        successMessage.append("Date: ").append(currentBill.orderDate).append("\n");
        successMessage.append("Time: ").append(currentBill.orderTime).append("\n\n");

        successMessage.append("🍽️ Your order is being prepared!\n");
        successMessage.append("You will receive your items shortly.\n\n");
        successMessage.append("Thank you for your order! 🎉");

        builder.setMessage(successMessage.toString());
        builder.setCancelable(false);

        builder.setPositiveButton("📄 Download Receipt", (dialog, which) -> {
            dialog.dismiss();
            checkPermissionAndDownloadPDF();
        });

        builder.setNeutralButton("📤 Share Receipt", (dialog, which) -> {
            dialog.dismiss();
            shareBill();
        });

        builder.setNegativeButton("✅ Done", (dialog, which) -> {
            dialog.dismiss();

            // ADD THIS: Send notifications to floor owners
            try {
                orderNotificationManager.sendOrderNotification(currentBill, cartItems);
                android.util.Log.d("CartActivity", "Order notifications sent successfully");
            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("CartActivity", "Failed to send order notifications: " + e.getMessage());
            }

            Toast.makeText(this, "Order placed successfully! 🎉", Toast.LENGTH_SHORT).show();
            clearCartAndFinish();
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        customizeDialogAppearance(dialog);
    }

    private void showPaymentFailedDialog(String errorMessage) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("❌ Payment Declined");

        StringBuilder failMessage = new StringBuilder();
        failMessage.append("Your payment was declined for security reasons.\n\n");
        failMessage.append("📋 Order Details:\n");
        failMessage.append("Order ID: ").append(currentBill.orderId).append("\n");
        failMessage.append("Amount: ₹").append(String.format("%.2f", currentBill.total)).append("\n\n");

        failMessage.append("💡 Try these solutions:\n");
        failMessage.append("• Use your registered mobile number\n");
        failMessage.append("• Use your primary UPI ID\n");
        failMessage.append("• Check your account balance\n");
        failMessage.append("• Try a different UPI app\n");
        failMessage.append("• Ensure stable internet connection\n");
        failMessage.append("• Contact your bank if issue persists\n\n");
        failMessage.append("Would you like to try again?");

        builder.setMessage(failMessage.toString());

        builder.setPositiveButton("🔄 Try Again", (dialog, which) -> {
            dialog.dismiss();
            // Small delay before retrying
            new android.os.Handler().postDelayed(() -> initiateRazorpayPayment(), 1000);
        });

        builder.setNegativeButton("❌ Cancel Order", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show();
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        customizeDialogAppearance(dialog);
    }

    private void showPaymentCancelledDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("⚠️ Payment Cancelled");

        StringBuilder cancelMessage = new StringBuilder();
        cancelMessage.append("⚠️ Payment was cancelled.\n\n");
        cancelMessage.append("📋 Order Details:\n");
        cancelMessage.append("Order ID: ").append(currentBill.orderId).append("\n");
        cancelMessage.append("Amount: ₹").append(String.format("%.2f", currentBill.total)).append("\n\n");

        cancelMessage.append("❓ Possible reasons:\n");
        cancelMessage.append("• You pressed back button\n");
        cancelMessage.append("• Payment was cancelled in UPI app\n");
        cancelMessage.append("• Network connection lost\n");
        cancelMessage.append("• UPI app was closed\n\n");

        cancelMessage.append("Your order is still in the cart.\n");
        cancelMessage.append("Would you like to try payment again?");

        builder.setMessage(cancelMessage.toString());

        builder.setPositiveButton("🔄 Try Again", (dialog, which) -> {
            dialog.dismiss();
            // Small delay before retrying
            new android.os.Handler().postDelayed(() -> initiateRazorpayPayment(), 1000);
        });

        builder.setNegativeButton("🛒 Keep in Cart", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(this, "Order kept in cart. You can try payment later.", Toast.LENGTH_LONG).show();
        });

        builder.setNeutralButton("❌ Cancel Order", (dialog, which) -> {
            dialog.dismiss();
            clearCartAndFinish();
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(false); // Prevent accidental dismissal
        dialog.show();
        customizeDialogAppearance(dialog);
    }

    private void showPaymentErrorDialog(String errorMessage) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("⚠️ Payment Error");

        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("⚠️ There was an error processing your payment.\n\n");
        errorMsg.append("📋 Order Details:\n");
        errorMsg.append("Order ID: ").append(currentBill.orderId).append("\n");
        errorMsg.append("Amount: ₹").append(String.format("%.2f", currentBill.total)).append("\n\n");
        errorMsg.append("❓ Error: ").append(errorMessage).append("\n\n");
        errorMsg.append("📞 Please contact support with your order ID if money was debited.\n\n");
        errorMsg.append("Would you like to try again?");

        builder.setMessage(errorMsg.toString());

        builder.setPositiveButton("🔄 Try Again", (dialog, which) -> {
            dialog.dismiss();
            initiateRazorpayPayment();
        });

        builder.setNeutralButton("📞 Contact Support", (dialog, which) -> {
            dialog.dismiss();
            // You can add support contact functionality here
            Toast.makeText(this, "Please contact canteen support", Toast.LENGTH_LONG).show();
        });

        builder.setNegativeButton("❌ Cancel", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(this, "Payment cancelled", Toast.LENGTH_SHORT).show();
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        customizeDialogAppearance(dialog);
    }

    private void clearCartAndFinish() {
        // Clear the cart items to prevent stock restoration in onDestroy
        cartItems.clear();
        filteredCartItems.clear();

        // Set successful result
        setResult(RESULT_OK);

        android.util.Log.d("CartActivity", "Cart cleared successfully, finishing activity");
        finish();
    }

    // Add this method to customize dialog appearance
    private void customizeDialogAppearance(androidx.appcompat.app.AlertDialog dialog) {
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            GradientDrawable dialogBackground = new GradientDrawable();
            dialogBackground.setColor(Color.WHITE);
            dialogBackground.setCornerRadius(dpToPx(16));
            dialogBackground.setStroke(dpToPx(2), Color.parseColor("#fc9432"));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                window.setElevation(dpToPx(8));
            }

            window.setBackgroundDrawable(dialogBackground);
        }

        // Customize buttons
        android.widget.Button positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
        android.widget.Button negativeButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);
        android.widget.Button neutralButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL);

        if (positiveButton != null) {
            GradientDrawable buttonBackground = new GradientDrawable();
            buttonBackground.setColor(Color.WHITE);
            buttonBackground.setStroke(dpToPx(2), Color.parseColor("#fc9432"));
            buttonBackground.setCornerRadius(dpToPx(8));
            positiveButton.setBackground(buttonBackground);
            positiveButton.setTextColor(Color.parseColor("#fc9432"));
            positiveButton.setTypeface(positiveButton.getTypeface(), android.graphics.Typeface.BOLD);
            positiveButton.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        }

        if (negativeButton != null) {
            GradientDrawable buttonBackground = new GradientDrawable();
            buttonBackground.setColor(Color.WHITE);
            buttonBackground.setStroke(dpToPx(2), Color.parseColor("#fc9432"));
            buttonBackground.setCornerRadius(dpToPx(8));
            negativeButton.setBackground(buttonBackground);
            negativeButton.setTextColor(Color.parseColor("#fc9432"));
            negativeButton.setTypeface(negativeButton.getTypeface(), android.graphics.Typeface.BOLD);
            negativeButton.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        }

        if (neutralButton != null) {
            GradientDrawable buttonBackground = new GradientDrawable();
            buttonBackground.setColor(Color.WHITE);
            buttonBackground.setStroke(dpToPx(2), Color.parseColor("#fc9432"));
            buttonBackground.setCornerRadius(dpToPx(8));
            neutralButton.setBackground(buttonBackground);
            neutralButton.setTextColor(Color.parseColor("#fc9432"));
            neutralButton.setTypeface(neutralButton.getTypeface(), android.graphics.Typeface.BOLD);
            neutralButton.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        }

        // Customize text colors
        android.widget.TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextColor(Color.parseColor("#333333"));
            titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        }

        android.widget.TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextColor(Color.parseColor("#666666"));
        }
    }

    private void generateBill() {
        currentBill = new Bill();
        currentBill.items = new ArrayList<>(cartItems);
        currentBill.subtotal = totalPrice;
        currentBill.tax = totalPrice * 0.18; // 18% GST
        currentBill.total = currentBill.subtotal + currentBill.tax;
    }

    // PDF and sharing methods remain the same as in your original code
    private void checkPermissionAndDownloadPDF() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            generateAndDownloadPDF();

            // ADD THIS: Send notifications after PDF generation
            try {
                orderNotificationManager.sendOrderNotification(currentBill, cartItems);
                android.util.Log.d("CartActivity", "Order notifications sent after PDF download");
            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("CartActivity", "Failed to send order notifications: " + e.getMessage());
            }

            clearCartAndFinish();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_PERMISSION);
            } else {
                generateAndDownloadPDF();

                // ADD THIS: Send notifications after PDF generation
                try {
                    orderNotificationManager.sendOrderNotification(currentBill, cartItems);
                    android.util.Log.d("CartActivity", "Order notifications sent after PDF download");
                } catch (Exception e) {
                    e.printStackTrace();
                    android.util.Log.e("CartActivity", "Failed to send order notifications: " + e.getMessage());
                }

                clearCartAndFinish();
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateAndDownloadPDF();
            } else {
                Toast.makeText(this, "Storage permission needed to save PDF. You can still share the receipt.", Toast.LENGTH_LONG).show();
                shareBill();
            }
        }
    }

    private void generateAndDownloadPDF() {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            android.graphics.Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setAntiAlias(true);

            // Header
            paint.setTextSize(24);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setColor(Color.parseColor("#fc9432"));
            canvas.drawText("UPI PAYMENT RECEIPT", 50, 80, paint);

            // Restaurant details
            paint.setTextSize(14);
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            paint.setColor(Color.BLACK);
            canvas.drawText("Multi-Floor Restaurant", 50, 120, paint);
            canvas.drawText("Address: Your Restaurant Address", 50, 140, paint);
            canvas.drawText("Phone: +91 XXXXXXXXXX", 50, 160, paint);

            // Order and payment details
            paint.setTextSize(12);
            canvas.drawText("Order ID: " + currentBill.orderId, 50, 200, paint);
            canvas.drawText("Date: " + currentBill.orderDate, 300, 200, paint);
            canvas.drawText("Time: " + currentBill.orderTime, 450, 200, paint);
            canvas.drawText("Customer: " + currentBill.customerName, 50, 220, paint);
            canvas.drawText("Payment Status: " + currentBill.paymentStatus, 300, 220, paint);

            if (!currentBill.transactionId.isEmpty()) {
                canvas.drawText("Transaction ID: " + currentBill.transactionId, 50, 240, paint);
            }

            // Draw line
            paint.setStrokeWidth(2);
            canvas.drawLine(50, 260, 545, 260, paint);

            // Items header
            int yPosition = 290;
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            canvas.drawText("Item", 50, yPosition, paint);
            canvas.drawText("Qty", 250, yPosition, paint);
            canvas.drawText("Price", 350, yPosition, paint);
            canvas.drawText("Total", 450, yPosition, paint);

            canvas.drawLine(50, yPosition + 15, 545, yPosition + 15, paint);

            // Items
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            yPosition += 35;

            String currentFloor = "";
            for (CartItem item : currentBill.items) {
                if (!item.floorSource.equals(currentFloor)) {
                    currentFloor = item.floorSource;
                    paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    paint.setColor(Color.parseColor("#666666"));
                    canvas.drawText("--- " + currentFloor + " ---", 50, yPosition, paint);
                    yPosition += 25;
                    paint.setTypeface(android.graphics.Typeface.DEFAULT);
                    paint.setColor(Color.BLACK);
                }

                String itemName = item.menuItem.name;
                if (itemName.length() > 25) {
                    itemName = itemName.substring(0, 22) + "...";
                }

                canvas.drawText(itemName, 50, yPosition, paint);
                canvas.drawText(String.valueOf(item.quantity), 250, yPosition, paint);
                canvas.drawText("₹" + String.format("%.0f", item.menuItem.price), 350, yPosition, paint);
                canvas.drawText("₹" + String.format("%.0f", item.menuItem.price * item.quantity), 450, yPosition, paint);
                yPosition += 25;

                if (yPosition > 750) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 2).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    paint.setAntiAlias(true);
                    yPosition = 50;
                }
            }

            // Totals section
            canvas.drawLine(300, yPosition + 15, 545, yPosition + 15, paint);
            yPosition += 40;

            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            canvas.drawText("Subtotal:", 350, yPosition, paint);
            canvas.drawText("₹" + String.format("%.2f", currentBill.subtotal), 450, yPosition, paint);
            yPosition += 25;

            canvas.drawText("GST (18%):", 350, yPosition, paint);
            canvas.drawText("₹" + String.format("%.2f", currentBill.tax), 450, yPosition, paint);
            yPosition += 25;

            paint.setStrokeWidth(2);
            canvas.drawLine(300, yPosition + 5, 545, yPosition + 5, paint);
            yPosition += 30;

            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(16);
            canvas.drawText("TOTAL PAID:", 350, yPosition, paint);
            canvas.drawText("₹" + String.format("%.2f", currentBill.total), 450, yPosition, paint);

            // Footer with payment details
            paint.setTextSize(12);
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            yPosition += 40;
            canvas.drawText("Payment Method: UPI", 200, yPosition, paint);
            yPosition += 20;
            canvas.drawText("Payment Status: " + currentBill.paymentStatus, 200, yPosition, paint);
            yPosition += 20;
            if (!currentBill.transactionId.isEmpty()) {
                canvas.drawText("Transaction ID: " + currentBill.transactionId, 200, yPosition, paint);
                yPosition += 20;
            }
            canvas.drawText("Order Status: Confirmed & Being Prepared", 200, yPosition, paint);
            yPosition += 20;
            canvas.drawText("Thank you for your order!", 200, yPosition, paint);

            pdfDocument.finishPage(page);

            // Save PDF
            String fileName = "UPI_Receipt_" + currentBill.orderId + ".pdf";
            savePdfToStorage(pdfDocument, fileName);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            shareBill();
        }
    }

    private void savePdfToStorage(PdfDocument pdfDocument, String fileName) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                savePDFUsingMediaStore(pdfDocument, fileName);
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }

                File file = new File(downloadsDir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                pdfDocument.writeTo(fos);
                fos.flush();
                fos.close();
                pdfDocument.close();

                Toast.makeText(this, "Receipt saved successfully!\nLocation: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                openPDF(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            pdfDocument.close();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void savePDFUsingMediaStore(PdfDocument pdfDocument, String fileName) {
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Files.getContentUri("external"), values);

            if (uri != null) {
                java.io.OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream);
                    outputStream.flush();
                    outputStream.close();
                    pdfDocument.close();

                    Toast.makeText(this, "Receipt saved successfully to Downloads!\nFile: " + fileName, Toast.LENGTH_LONG).show();
                    openPDFFromUri(uri);
                } else {
                    throw new IOException("Could not open output stream");
                }
            } else {
                throw new IOException("Could not create file in Downloads");
            }
        } catch (IOException e) {
            e.printStackTrace();
            pdfDocument.close();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            shareBill();
        }
    }

    private void openPDF(File pdfFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            android.net.Uri uri;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                uri = androidx.core.content.FileProvider.getUriForFile(this,
                        getPackageName() + ".provider", pdfFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = android.net.Uri.fromFile(pdfFile);
            }

            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No PDF viewer app found.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Cannot open PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openPDFFromUri(android.net.Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No PDF viewer app found.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Cannot open PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareBill() {
        StringBuilder receiptText = new StringBuilder();
        receiptText.append("=== UPI PAYMENT RECEIPT ===\n");
        receiptText.append("Multi-Floor Restaurant\n\n");
        receiptText.append("Order ID: ").append(currentBill.orderId).append("\n");
        receiptText.append("Date: ").append(currentBill.orderDate).append("\n");
        receiptText.append("Time: ").append(currentBill.orderTime).append("\n");
        receiptText.append("Customer: ").append(currentBill.customerName).append("\n");
        receiptText.append("Payment Status: ").append(currentBill.paymentStatus).append("\n");

        if (!currentBill.transactionId.isEmpty()) {
            receiptText.append("Transaction ID: ").append(currentBill.transactionId).append("\n");
        }
        receiptText.append("\n");

        receiptText.append("--- ORDER DETAILS ---\n");
        String currentFloor = "";
        for (CartItem item : currentBill.items) {
            if (!item.floorSource.equals(currentFloor)) {
                currentFloor = item.floorSource;
                receiptText.append("\n").append(currentFloor).append(":\n");
            }
            receiptText.append(item.menuItem.name).append("\n");
            receiptText.append(item.quantity).append(" x ₹")
                    .append(String.format("%.0f", item.menuItem.price))
                    .append(" = ₹").append(String.format("%.0f", item.menuItem.price * item.quantity)).append("\n");
        }

        receiptText.append("\n--- PAYMENT SUMMARY ---\n");
        receiptText.append("Subtotal: ₹").append(String.format("%.2f", currentBill.subtotal)).append("\n");
        receiptText.append("GST (18%): ₹").append(String.format("%.2f", currentBill.tax)).append("\n");
        receiptText.append("Total Paid: ₹").append(String.format("%.2f", currentBill.total)).append("\n");
        receiptText.append("Payment Method: UPI\n");
        receiptText.append("Payment Status: ").append(currentBill.paymentStatus).append("\n\n");
        receiptText.append("Order Status: Confirmed & Being Prepared\n");
        receiptText.append("Thank you for your order! 🎉");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "UPI Payment Receipt - " + currentBill.orderId);
        shareIntent.putExtra(Intent.EXTRA_TEXT, receiptText.toString());

        startActivity(Intent.createChooser(shareIntent, "Share Receipt"));
        try {
            orderNotificationManager.sendOrderNotification(currentBill, cartItems);
            android.util.Log.d("CartActivity", "Order notifications sent after bill sharing");
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("CartActivity", "Failed to send order notifications: " + e.getMessage());
        }

        clearCartAndFinish();
    }

    // Utility methods
    private void animateButtonClick(Button button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f, 1f);
        scaleX.setDuration(150);
        scaleY.setDuration(150);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.start();
    }

    private void startEntranceAnimations() {
        ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(cartTitleText, "alpha", 0f, 1f);
        ObjectAnimator titleTranslation = ObjectAnimator.ofFloat(cartTitleText, "translationY", -50f, 0f);
        titleAlpha.setDuration(600);
        titleTranslation.setDuration(600);

        AnimatorSet titleAnimSet = new AnimatorSet();
        titleAnimSet.playTogether(titleAlpha, titleTranslation);
        titleAnimSet.start();

        ObjectAnimator searchAlpha = ObjectAnimator.ofFloat(searchEditText, "alpha", 0f, 1f);
        ObjectAnimator searchTranslation = ObjectAnimator.ofFloat(searchEditText, "translationY", -30f, 0f);
        searchAlpha.setDuration(600);
        searchTranslation.setDuration(600);

        AnimatorSet searchAnimSet = new AnimatorSet();
        searchAnimSet.playTogether(searchAlpha, searchTranslation);
        searchAnimSet.setStartDelay(200);
        searchAnimSet.start();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Only restore stock if payment was not successful
        // and the activity is being destroyed unexpectedly (back button, app killed, etc.)
        if (currentBill == null ||
                (currentBill.paymentStatus != null && !currentBill.paymentStatus.equals("Success"))) {

            // Check if this is an unexpected destruction (not from successful payment)
            if (isFinishing() && (currentBill == null ||
                    !currentBill.paymentStatus.equals("Success"))) {
                restoreCartStock();
                android.util.Log.d("CartActivity", "Activity destroyed, restored cart stock");
            }
        }
    }

    private void restoreCartStock() {
        if (cartItems != null && !cartItems.isEmpty()) {
            android.util.Log.d("CartActivity", "Restoring stock for abandoned cart...");

            for (CartItem cartItem : cartItems) {
                try {
                    boolean restored = menuDataManager.increaseStockByName(
                            cartItem.menuItem.name,
                            cartItem.floorSource,
                            cartItem.quantity
                    );

                    if (restored) {
                        android.util.Log.d("CartActivity", "Restored " + cartItem.quantity +
                                " items for " + cartItem.menuItem.name + " from " + cartItem.floorSource);
                    } else {
                        android.util.Log.w("CartActivity", "Failed to restore stock for: " + cartItem.menuItem.name);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    android.util.Log.e("CartActivity", "Error restoring stock for item: " + cartItem.menuItem.name, e);
                }
            }

            android.util.Log.d("CartActivity", "Stock restoration completed");

            // Clear the cart to prevent double restoration
            cartItems.clear();
        }
    }

    private void restoreStockForItem(CartItem cartItem) {
        try {
            // Use the new method to restore stock by name
            boolean restored = menuDataManager.increaseStockByName(
                    cartItem.menuItem.name,
                    cartItem.floorSource,
                    cartItem.quantity
            );

            if (restored) {
                android.util.Log.d("CartActivity", "Restored " + cartItem.quantity +
                        " items for " + cartItem.menuItem.name + " from " + cartItem.floorSource);
            } else {
                android.util.Log.w("CartActivity", "Failed to restore stock for: " + cartItem.menuItem.name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("CartActivity", "Error restoring stock for item: " + cartItem.menuItem.name, e);
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    // Static helper method for cart conversion
    public static ArrayList<CartItem> convertToGenericCart(List<?> existingCartItems, String floorSource) {
        ArrayList<CartItem> genericCart = new ArrayList<>();

        for (Object item : existingCartItems) {
            try {
                java.lang.reflect.Field menuItemField = item.getClass().getField("menuItem");
                java.lang.reflect.Field quantityField = item.getClass().getField("quantity");

                Object menuItemObj = menuItemField.get(item);
                int quantity = quantityField.getInt(item);

                java.lang.reflect.Field nameField = menuItemObj.getClass().getField("name");
                java.lang.reflect.Field descField = menuItemObj.getClass().getField("description");
                java.lang.reflect.Field priceField = menuItemObj.getClass().getField("price");
                java.lang.reflect.Field imageField = menuItemObj.getClass().getField("imageRes");

                String name = (String) nameField.get(menuItemObj);
                String description = (String) descField.get(menuItemObj);
                double price = priceField.getDouble(menuItemObj);
                int imageRes = imageField.getInt(menuItemObj);

                MenuItem genericMenuItem = new MenuItem(name, description, price, imageRes);
                CartItem genericCartItem = new CartItem(genericMenuItem, quantity, floorSource);

                genericCart.add(genericCartItem);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return genericCart;
    }
}