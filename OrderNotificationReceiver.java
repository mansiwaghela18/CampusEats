package com.example.shashankscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class OrderNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if ("MARK_READY".equals(action)) {
            String orderId = intent.getStringExtra("order_id");
            String floorName = intent.getStringExtra("floor_name");

            if (orderId != null && floorName != null) {
                OrderNotificationManager notificationManager = OrderNotificationManager.getInstance(context);
                notificationManager.markOrderAsReady(orderId, floorName);

                // Show toast notification
                Toast.makeText(context, "Order #" + orderId + " marked as ready!", Toast.LENGTH_LONG).show();

                // Log the action
                android.util.Log.d("OrderNotification", "Order " + orderId + " marked as ready from notification");
            }
        }
    }
}