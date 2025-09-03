package com.example.shashankscreen;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.List;
import android.os.Build;

public class OwnerOrderManagementActivity extends AppCompatActivity {

    private TextView floorTitleText;
    private TextView orderCountText;
    private LinearLayout ordersContainer;
    private TextView emptyOrdersText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String floorName;
    private OrderNotificationManager notificationManager;
    private List<OrderNotificationManager.OrderNotification> pendingOrders;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 10000; // 10 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_order_management);

        // Get floor name from intent
        floorName = getIntent().getStringExtra("floor_name");
        if (floorName == null) floorName = "Floor 1";

        // Initialize notification manager
        notificationManager = OrderNotificationManager.getInstance(this);

        // Initialize views
        initializeViews();

        // Load pending orders
        loadPendingOrders();

        // Setup refresh functionality
        setupRefreshFunctionality();

        // Start auto-refresh
        startAutoRefresh();

        // Start entrance animations
        startEntranceAnimations();

        // Play notification sound if coming from notification
        String orderId = getIntent().getStringExtra("order_id");
        if (orderId != null) {
            playNotificationSound();
        }
    }

    private void initializeViews() {
        floorTitleText = findViewById(R.id.floor_title_text);
        orderCountText = findViewById(R.id.order_count_text);
        ordersContainer = findViewById(R.id.orders_container);
        emptyOrdersText = findViewById(R.id.empty_orders_text);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        // Back button
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> onBackPressed());

        // Set title
        floorTitleText.setText(floorName + " - Order Management");

        // Configure swipe refresh
        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#fc9432"));
    }

    private void setupRefreshFunctionality() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadPendingOrders();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void startAutoRefresh() {
        refreshHandler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    loadPendingOrders();
                    refreshHandler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        };
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    private void stopAutoRefresh() {
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void loadPendingOrders() {
        pendingOrders = notificationManager.getPendingOrdersForFloor(floorName);
        displayOrders();
        updateOrderCount();
    }

    private void updateOrderCount() {
        int pendingCount = 0;
        for (OrderNotificationManager.OrderNotification order : pendingOrders) {
            if (order.orderStatus.equals("New Order")) {
                pendingCount++;
            }
        }

        orderCountText.setText("Pending Orders: " + pendingCount);
        orderCountText.setVisibility(View.VISIBLE);
    }

    private void displayOrders() {
        ordersContainer.removeAllViews();

        if (pendingOrders.isEmpty()) {
            showEmptyOrdersState();
            return;
        }

        emptyOrdersText.setVisibility(View.GONE);

        // Sort orders by timestamp (newest first)
        pendingOrders.sort((o1, o2) -> Long.compare(o2.timestamp, o1.timestamp));

        for (int i = 0; i < pendingOrders.size(); i++) {
            OrderNotificationManager.OrderNotification order = pendingOrders.get(i);
            addOrderCard(order, i);
        }
    }

    private void showEmptyOrdersState() {
        emptyOrdersText.setVisibility(View.VISIBLE);
        emptyOrdersText.setText("No pending orders for " + floorName + "\n\nNew orders will appear here automatically");
    }

    private void addOrderCard(OrderNotificationManager.OrderNotification order, int position) {
        CardView cardView = new CardView(this);
        cardView.setCardElevation(dpToPx(6));
        cardView.setRadius(dpToPx(16));
        cardView.setUseCompatPadding(true);

        // Set card color based on order status
        if (order.orderStatus.equals("New Order")) {
            cardView.setCardBackgroundColor(Color.parseColor("#FFF8E1")); // Light yellow for new orders
        } else if (order.orderStatus.equals("Ready")) {
            cardView.setCardBackgroundColor(Color.parseColor("#E8F5E8")); // Light green for ready orders
        } else {
            cardView.setCardBackgroundColor(Color.WHITE);
        }

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        cardView.setLayoutParams(cardParams);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Header section
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(0, 0, 0, dpToPx(12));

        // Order ID and status
        TextView orderIdText = new TextView(this);
        orderIdText.setText("Order #" + order.orderId);
        orderIdText.setTextSize(18);
        orderIdText.setTypeface(orderIdText.getTypeface(), android.graphics.Typeface.BOLD);
        orderIdText.setTextColor(Color.parseColor("#333333"));
        orderIdText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        // Status badge
        TextView statusBadge = new TextView(this);
        statusBadge.setText(order.orderStatus);
        statusBadge.setTextSize(12);
        statusBadge.setTypeface(statusBadge.getTypeface(), android.graphics.Typeface.BOLD);
        statusBadge.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));

        GradientDrawable statusBackground = new GradientDrawable();
        if (order.orderStatus.equals("New Order")) {
            statusBadge.setTextColor(Color.parseColor("#E65100"));
            statusBackground.setColor(Color.parseColor("#FFE0B2"));
        } else if (order.orderStatus.equals("Ready")) {
            statusBadge.setTextColor(Color.parseColor("#2E7D32"));
            statusBackground.setColor(Color.parseColor("#C8E6C9"));
        }
        statusBackground.setCornerRadius(dpToPx(12));
        statusBadge.setBackground(statusBackground);

        headerLayout.addView(orderIdText);
        headerLayout.addView(statusBadge);

        // Order details section
        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.HORIZONTAL);
        detailsLayout.setPadding(0, 0, 0, dpToPx(12));

        TextView timeText = new TextView(this);
        timeText.setText("📅 " + order.orderDate + " at " + order.orderTime);
        timeText.setTextSize(12);
        timeText.setTextColor(Color.parseColor("#666666"));
        timeText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView totalText = new TextView(this);
        totalText.setText("💰 ₹" + String.format("%.2f", order.totalAmount));
        totalText.setTextSize(14);
        totalText.setTypeface(totalText.getTypeface(), android.graphics.Typeface.BOLD);
        totalText.setTextColor(Color.parseColor("#fc9432"));

        detailsLayout.addView(timeText);
        detailsLayout.addView(totalText);

        // Items section
        TextView itemsHeaderText = new TextView(this);
        itemsHeaderText.setText("🍽️ Order Items (" + order.totalItems + " items):");
        itemsHeaderText.setTextSize(14);
        itemsHeaderText.setTypeface(itemsHeaderText.getTypeface(), android.graphics.Typeface.BOLD);
        itemsHeaderText.setTextColor(Color.parseColor("#333333"));
        itemsHeaderText.setPadding(0, 0, 0, dpToPx(8));

        // Items list
        LinearLayout itemsLayout = new LinearLayout(this);
        itemsLayout.setOrientation(LinearLayout.VERTICAL);
        itemsLayout.setPadding(dpToPx(12), 0, 0, 0);

        for (OrderNotificationManager.OrderItem item : order.items) {
            TextView itemText = new TextView(this);
            itemText.setText("• " + item.itemName + " × " + item.quantity + " = ₹" + String.format("%.0f", item.total));
            itemText.setTextSize(13);
            itemText.setTextColor(Color.parseColor("#555555"));
            itemText.setPadding(0, dpToPx(2), 0, dpToPx(2));
            itemsLayout.addView(itemText);
        }

        // Action buttons section
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(android.view.Gravity.END);
        buttonLayout.setPadding(0, dpToPx(16), 0, 0);

        if (order.orderStatus.equals("New Order")) {
            // Mark as Ready button
            Button markReadyButton = createStyledButton("✅ Mark Ready", "#4CAF50", "#388E3C");
            markReadyButton.setOnClickListener(v -> {
                markOrderAsReady(order);
                animateButtonClick(markReadyButton);
            });

            // View Details button
            Button viewDetailsButton = createStyledButton("👁️ View Details", "#2196F3", "#1976D2");
            LinearLayout.LayoutParams detailsButtonParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            detailsButtonParams.setMargins(0, 0, dpToPx(12), 0);
            viewDetailsButton.setLayoutParams(detailsButtonParams);

            viewDetailsButton.setOnClickListener(v -> {
                showOrderDetailsDialog(order);
                animateButtonClick(viewDetailsButton);
            });

            buttonLayout.addView(viewDetailsButton);
            buttonLayout.addView(markReadyButton);

        } else if (order.orderStatus.equals("Ready")) {
            // Order Completed button
            Button completedButton = createStyledButton("✅ Order Delivered", "#FF5722", "#D84315");
            completedButton.setOnClickListener(v -> {
                completeOrder(order);
                animateButtonClick(completedButton);
            });
            buttonLayout.addView(completedButton);
        }

        // Assemble the card
        mainLayout.addView(headerLayout);
        mainLayout.addView(detailsLayout);
        mainLayout.addView(itemsHeaderText);
        mainLayout.addView(itemsLayout);
        mainLayout.addView(buttonLayout);

        cardView.addView(mainLayout);

        // Add entrance animation
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
        }, position * 100);

        ordersContainer.addView(cardView);
    }

    private Button createStyledButton(String text, String normalColor, String pressedColor) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(12);
        button.setTypeface(button.getTypeface(), android.graphics.Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        button.setMinWidth(dpToPx(100));
        button.setMinHeight(dpToPx(40));

        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(Color.parseColor(normalColor));
        buttonBackground.setCornerRadius(dpToPx(20));
        button.setBackground(buttonBackground);

        return button;
    }

    private void markOrderAsReady(OrderNotificationManager.OrderNotification order) {
        new AlertDialog.Builder(this)
                .setTitle("Mark Order Ready")
                .setMessage("Mark order #" + order.orderId + " as ready for pickup?\n\n" +
                        "Customer will be notified that the order is ready.")
                .setPositiveButton("✅ Mark Ready", (dialog, which) -> {
                    notificationManager.markOrderAsReady(order.orderId, floorName);

                    // Send notification to student (you can implement this later)
                    sendReadyNotificationToStudent(order);

                    loadPendingOrders();
                    Toast.makeText(this, "Order #" + order.orderId + " marked as ready!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completeOrder(OrderNotificationManager.OrderNotification order) {
        new AlertDialog.Builder(this)
                .setTitle("Complete Order")
                .setMessage("Mark order #" + order.orderId + " as delivered and remove from pending list?")
                .setPositiveButton("✅ Delivered", (dialog, which) -> {
                    notificationManager.removeOrder(order.orderId, floorName);
                    loadPendingOrders();
                    Toast.makeText(this, "Order #" + order.orderId + " completed!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showOrderDetailsDialog(OrderNotificationManager.OrderNotification order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Order Details - #" + order.orderId);

        StringBuilder details = new StringBuilder();
        details.append("📋 Order Information:\n");
        details.append("Order ID: ").append(order.orderId).append("\n");
        details.append("Customer: ").append(order.customerName).append("\n");
        details.append("Date: ").append(order.orderDate).append("\n");
        details.append("Time: ").append(order.orderTime).append("\n");
        details.append("Floor: ").append(order.floorName).append("\n");
        details.append("Status: ").append(order.orderStatus).append("\n");
        details.append("Payment: ").append(order.paymentStatus).append("\n\n");

        details.append("🍽️ Order Items:\n");
        for (OrderNotificationManager.OrderItem item : order.items) {
            details.append("• ").append(item.itemName)
                    .append(" × ").append(item.quantity)
                    .append(" = ₹").append(String.format("%.0f", item.total)).append("\n");
        }

        details.append("\n💰 Payment Summary:\n");
        details.append("Total Items: ").append(order.totalItems).append("\n");
        details.append("Total Amount: ₹").append(String.format("%.2f", order.totalAmount)).append("\n");

        builder.setMessage(details.toString());
        builder.setPositiveButton("OK", null);

        AlertDialog dialog = builder.create();
        dialog.show();
        customizeDialogAppearance(dialog);
    }

    private void sendReadyNotificationToStudent(OrderNotificationManager.OrderNotification order) {
        // This would integrate with a student notification system
        // For now, we'll just log it
        android.util.Log.d("StudentNotification", "Order #" + order.orderId + " is ready for pickup from " + floorName);

        // You can implement push notifications to student app here
        // Or integrate with Firebase Cloud Messaging for real-time notifications
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer mp = MediaPlayer.create(getApplicationContext(), notification);
            mp.start();
            mp.setOnCompletionListener(MediaPlayer::release);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void startEntranceAnimations() {
        ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(floorTitleText, "alpha", 0f, 1f);
        ObjectAnimator titleTranslation = ObjectAnimator.ofFloat(floorTitleText, "translationY", -50f, 0f);
        titleAlpha.setDuration(600);
        titleTranslation.setDuration(600);

        AnimatorSet titleAnimSet = new AnimatorSet();
        titleAnimSet.playTogether(titleAlpha, titleTranslation);
        titleAnimSet.start();

        new Handler().postDelayed(() -> {
            ObjectAnimator countAlpha = ObjectAnimator.ofFloat(orderCountText, "alpha", 0f, 1f);
            ObjectAnimator countTranslation = ObjectAnimator.ofFloat(orderCountText, "translationY", -30f, 0f);
            countAlpha.setDuration(400);
            countTranslation.setDuration(400);

            AnimatorSet countAnimSet = new AnimatorSet();
            countAnimSet.playTogether(countAlpha, countTranslation);
            countAnimSet.start();
        }, 200);
    }

    private void customizeDialogAppearance(AlertDialog dialog) {
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            GradientDrawable dialogBackground = new GradientDrawable();
            dialogBackground.setColor(Color.WHITE);
            dialogBackground.setCornerRadius(dpToPx(16));
            dialogBackground.setStroke(dpToPx(2), Color.parseColor("#fc9432"));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setElevation(dpToPx(8));
            }

            window.setBackgroundDrawable(dialogBackground);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingOrders();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoRefresh();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}