package com.example.shashankscreen;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderNotificationManager {

    private static final String CHANNEL_ID = "order_notifications";
    private static final String CHANNEL_NAME = "Order Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for new food orders";
    private static final String PREFS_NAME = "OrderNotifications";
    private static final String PENDING_ORDERS_KEY = "pending_orders";

    private static OrderNotificationManager instance;
    private Context context;
    private NotificationManager notificationManager;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    // Order data structure for notifications
    public static class OrderNotification {
        public String orderId;
        public String customerName;
        public String floorName;
        public String orderDate;
        public String orderTime;
        public double totalAmount;
        public int totalItems;
        public ArrayList<OrderItem> items;
        public String paymentStatus;
        public String orderStatus;
        public long timestamp;

        public OrderNotification() {
            this.timestamp = System.currentTimeMillis();
            Date now = new Date();
            this.orderDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now);
            this.orderTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now);
            this.orderStatus = "New Order";
            this.customerName = "Student";
        }
    }

    public static class OrderItem {
        public String itemName;
        public int quantity;
        public double price;
        public double total;

        public OrderItem(String itemName, int quantity, double price) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.price = price;
            this.total = price * quantity;
        }
    }

    private OrderNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.sharedPreferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        createNotificationChannel();
    }

    public static synchronized OrderNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new OrderNotificationManager(context);
        }
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLightColor(android.graphics.Color.parseColor("#fc9432"));

            notificationManager.createNotificationChannel(channel);
        }
    }

    // Main method to send order notification to floor owner
    public void sendOrderNotification(CartActivity.Bill bill, ArrayList<CartActivity.CartItem> cartItems) {
        try {
            // Group items by floor
            for (String floorName : getFloorsFromCart(cartItems)) {
                ArrayList<CartActivity.CartItem> floorItems = getItemsForFloor(cartItems, floorName);
                if (!floorItems.isEmpty()) {
                    createAndSendNotification(bill, floorItems, floorName);
                    saveOrderForFloor(bill, floorItems, floorName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("OrderNotification", "Error sending notification: " + e.getMessage());
        }
    }

    private List<String> getFloorsFromCart(ArrayList<CartActivity.CartItem> cartItems) {
        List<String> floors = new ArrayList<>();
        for (CartActivity.CartItem item : cartItems) {
            if (!floors.contains(item.floorSource)) {
                floors.add(item.floorSource);
            }
        }
        return floors;
    }

    private ArrayList<CartActivity.CartItem> getItemsForFloor(ArrayList<CartActivity.CartItem> cartItems, String floorName) {
        ArrayList<CartActivity.CartItem> floorItems = new ArrayList<>();
        for (CartActivity.CartItem item : cartItems) {
            if (item.floorSource.equals(floorName)) {
                floorItems.add(item);
            }
        }
        return floorItems;
    }

    private void createAndSendNotification(CartActivity.Bill bill, ArrayList<CartActivity.CartItem> floorItems, String floorName) {
        // Calculate floor-specific totals
        double floorTotal = 0;
        int floorItemCount = 0;
        StringBuilder itemsText = new StringBuilder();

        for (CartActivity.CartItem item : floorItems) {
            floorTotal += item.menuItem.price * item.quantity;
            floorItemCount += item.quantity;
            itemsText.append("• ").append(item.menuItem.name)
                    .append(" x").append(item.quantity).append("\n");
        }

        // Create notification intent to open owner's order management activity
        Intent notificationIntent = new Intent(context, OwnerOrderManagementActivity.class);
        notificationIntent.putExtra("floor_name", floorName);
        notificationIntent.putExtra("order_id", bill.orderId);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_agenda) // You can replace with custom icon
                .setContentTitle("New Order - " + floorName)
                .setContentText(floorItemCount + " items • ₹" + String.format("%.0f", floorTotal))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Order ID: " + bill.orderId + "\n" +
                                "Items: " + floorItemCount + "\n" +
                                "Amount: ₹" + String.format("%.2f", floorTotal) + "\n" +
                                "Time: " + bill.orderTime + "\n\n" +
                                "Items:\n" + itemsText.toString().trim()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(android.graphics.Color.parseColor("#fc9432"))
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Add action buttons
        Intent markReadyIntent = new Intent(context, OrderNotificationReceiver.class);
        markReadyIntent.setAction("MARK_READY");
        markReadyIntent.putExtra("order_id", bill.orderId);
        markReadyIntent.putExtra("floor_name", floorName);

        PendingIntent markReadyPendingIntent = PendingIntent.getBroadcast(
                context,
                (int) (System.currentTimeMillis() + 1),
                markReadyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(android.R.drawable.ic_menu_agenda, "Mark Ready", markReadyPendingIntent);

        // Send notification with unique ID for each floor
        int notificationId = (bill.orderId + "_" + floorName).hashCode();

        try {
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            notificationManagerCompat.notify(notificationId, builder.build());

            android.util.Log.d("OrderNotification", "Notification sent for " + floorName +
                    " - Order ID: " + bill.orderId + " - Notification ID: " + notificationId);
        } catch (SecurityException e) {
            android.util.Log.e("OrderNotification", "Notification permission not granted: " + e.getMessage());
        }
    }

    private void saveOrderForFloor(CartActivity.Bill bill, ArrayList<CartActivity.CartItem> floorItems, String floorName) {
        try {
            // Create order notification object
            OrderNotification orderNotification = new OrderNotification();
            orderNotification.orderId = bill.orderId;
            orderNotification.floorName = floorName;
            orderNotification.totalAmount = 0;
            orderNotification.totalItems = 0;
            orderNotification.items = new ArrayList<>();
            orderNotification.paymentStatus = bill.paymentStatus;
            orderNotification.customerName = bill.customerName;

            // Calculate floor-specific details
            for (CartActivity.CartItem item : floorItems) {
                orderNotification.totalAmount += item.menuItem.price * item.quantity;
                orderNotification.totalItems += item.quantity;
                orderNotification.items.add(new OrderItem(
                        item.menuItem.name,
                        item.quantity,
                        item.menuItem.price
                ));
            }

            // Save to SharedPreferences
            List<OrderNotification> existingOrders = getPendingOrdersForFloor(floorName);
            existingOrders.add(orderNotification);

            String ordersJson = gson.toJson(existingOrders);
            sharedPreferences.edit()
                    .putString(PENDING_ORDERS_KEY + "_" + floorName, ordersJson)
                    .apply();

            android.util.Log.d("OrderNotification", "Order saved for " + floorName +
                    " - Total: ₹" + orderNotification.totalAmount);

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("OrderNotification", "Error saving order: " + e.getMessage());
        }
    }

    public List<OrderNotification> getPendingOrdersForFloor(String floorName) {
        try {
            String ordersJson = sharedPreferences.getString(PENDING_ORDERS_KEY + "_" + floorName, "[]");
            Type listType = new TypeToken<List<OrderNotification>>(){}.getType();
            return gson.fromJson(ordersJson, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void markOrderAsReady(String orderId, String floorName) {
        try {
            List<OrderNotification> orders = getPendingOrdersForFloor(floorName);
            for (OrderNotification order : orders) {
                if (order.orderId.equals(orderId)) {
                    order.orderStatus = "Ready";
                    break;
                }
            }

            // Save updated list
            String ordersJson = gson.toJson(orders);
            sharedPreferences.edit()
                    .putString(PENDING_ORDERS_KEY + "_" + floorName, ordersJson)
                    .apply();

            // Cancel the notification
            int notificationId = (orderId + "_" + floorName).hashCode();
            notificationManager.cancel(notificationId);

            android.util.Log.d("OrderNotification", "Order " + orderId + " marked as ready for " + floorName);

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("OrderNotification", "Error marking order ready: " + e.getMessage());
        }
    }

    public void removeOrder(String orderId, String floorName) {
        try {
            List<OrderNotification> orders = getPendingOrdersForFloor(floorName);
            orders.removeIf(order -> order.orderId.equals(orderId));

            // Save updated list
            String ordersJson = gson.toJson(orders);
            sharedPreferences.edit()
                    .putString(PENDING_ORDERS_KEY + "_" + floorName, ordersJson)
                    .apply();

            // Cancel the notification
            int notificationId = (orderId + "_" + floorName).hashCode();
            notificationManager.cancel(notificationId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPendingOrderCount(String floorName) {
        return getPendingOrdersForFloor(floorName).size();
    }

    // Method to clear all orders for a floor (useful for testing)
    public void clearAllOrdersForFloor(String floorName) {
        sharedPreferences.edit()
                .remove(PENDING_ORDERS_KEY + "_" + floorName)
                .apply();
    }

    // Method to send test notification (for debugging)
    public void sendTestNotification(String floorName) {
        CartActivity.Bill testBill = new CartActivity.Bill();
        testBill.orderId = "TEST001";
        testBill.paymentStatus = "Success";

        ArrayList<CartActivity.CartItem> testItems = new ArrayList<>();
        CartActivity.MenuItem testMenuItem = new CartActivity.MenuItem(
                "Test Item", "Test Description", 50.0, android.R.drawable.ic_menu_gallery
        );
        testItems.add(new CartActivity.CartItem(testMenuItem, 2, floorName));

        sendOrderNotification(testBill, testItems);
    }
}