package com.example.shashankscreen;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuDataManager {
    private static MenuDataManager instance;
    private static final String PREFS_NAME = "MenuPreferences";
    private static final String MENU_ITEMS_KEY_PREFIX = "menu_items_floor_";
    private static final String DATA_VERSION_KEY = "data_version";
    private static final int CURRENT_DATA_VERSION = 10; // Increment for image fix

    private Map<String, List<MenuItem>> floorMenuItems;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private String currentFloor;

    // Singleton pattern
    public static synchronized MenuDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new MenuDataManager(context.getApplicationContext());
        }
        return instance;
    }

    private MenuDataManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        floorMenuItems = new HashMap<>();
        currentFloor = "Floor 1"; // Default floor
        loadAllFloorsMenuItems();
    }

    // Enhanced Menu items data structure with stock - MADE SERIALIZABLE
    public static class MenuItem implements java.io.Serializable {
        public String name;
        public String description;
        public double price;
        public int imageRes;
        public String category;
        public String imageUri;
        public boolean hasCustomImage;
        public long id; // Add unique identifier
        public String floor; // Add floor identifier
        public int stock; // Add stock field

        // Main constructor with floor and stock parameters
        public MenuItem(String name, String description, double price, int imageRes, String category, String floor, int stock) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.imageRes = imageRes;
            this.category = category;
            this.floor = floor;
            this.stock = stock;
            this.imageUri = null;
            this.hasCustomImage = false;
            this.id = System.currentTimeMillis() + (long)(Math.random() * 1000); // Better unique ID
        }

        // Constructor for custom image with floor and stock parameters
        public MenuItem(String name, String description, double price, String imageUri, String category, String floor, int stock) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.imageRes = getDefaultImageForCategory(category); // Use category-based default
            this.category = category;
            this.floor = floor;
            this.stock = stock;
            this.imageUri = imageUri;
            this.hasCustomImage = true;
            this.id = System.currentTimeMillis() + (long)(Math.random() * 1000); // Better unique ID
        }

        // Constructor for existing items with ID and stock
        public MenuItem(long id, String name, String description, double price, int imageRes,
                        String category, String imageUri, boolean hasCustomImage, String floor, int stock) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.imageRes = imageRes;
            this.category = category;
            this.imageUri = imageUri;
            this.hasCustomImage = hasCustomImage;
            this.floor = floor;
            this.stock = stock;
        }

        // Additional constructor without ID (for updates) with stock
        public MenuItem(String name, String description, double price, int imageRes,
                        String category, String imageUri, boolean hasCustomImage, String floor, int stock) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.imageRes = imageRes;
            this.category = category;
            this.imageUri = imageUri;
            this.hasCustomImage = hasCustomImage;
            this.floor = floor;
            this.stock = stock;
            this.id = System.currentTimeMillis() + (long)(Math.random() * 1000);
        }

        // Helper method to get default image based on category
        private static int getDefaultImageForCategory(String category) {
            if (category == null) return R.drawable.khandvi;

            switch (category.toLowerCase()) {
                case "beverages":
                    return R.drawable.cappuccino;
                case "gujarati special":
                    return R.drawable.dal;
                case "telugu special":
                    return R.drawable.arise;
                case "main course":
                default:
                    return R.drawable.khandvi;
            }
        }

        // Backward compatibility constructors (without stock parameter) - defaults stock to 10
        public MenuItem(String name, String description, double price, int imageRes, String category, String floor) {
            this(name, description, price, imageRes, category, floor, 10); // Default stock of 10
        }

        public MenuItem(String name, String description, double price, String imageUri, String category, String floor) {
            this(name, description, price, imageUri, category, floor, 10); // Default stock of 10
        }

        // Backward compatibility constructors (without floor parameter) - defaults to Floor 1 and stock 10
        public MenuItem(String name, String description, double price, int imageRes, String category) {
            this(name, description, price, imageRes, category, "Floor 1", 10);
        }

        public MenuItem(String name, String description, double price, String imageUri, String category) {
            this(name, description, price, imageUri, category, "Floor 1", 10);
        }
    }

    // Set current floor context
    public void setCurrentFloor(String floorName) {
        this.currentFloor = floorName;
        if (!floorMenuItems.containsKey(floorName)) {
            floorMenuItems.put(floorName, new ArrayList<>());
            loadMenuItemsForFloor(floorName);
        }
    }

    private void loadAllFloorsMenuItems() {
        // Load menu items for all floors
        String[] floors = {"Floor 1", "Floor 2", "Floor 3", "Floor 4"};

        for (String floor : floors) {
            loadMenuItemsForFloor(floor);
        }
    }

    private void loadMenuItemsForFloor(String floorName) {
        // Check data version to determine if we need to reload defaults
        int savedVersion = sharedPreferences.getInt(DATA_VERSION_KEY, 1);
        String menuKey = MENU_ITEMS_KEY_PREFIX + floorName.toLowerCase().replace(" ", "_");

        List<MenuItem> menuItems = new ArrayList<>();

        // Always load fresh default items if version doesn't match or no data exists
        if (savedVersion != CURRENT_DATA_VERSION) {
            // Version mismatch - clear all floor data and load fresh defaults
            clearAllStoredData();
            loadDefaultMenuItemsForFloor(floorName, menuItems);
            // Update version after clearing all data
            sharedPreferences.edit().putInt(DATA_VERSION_KEY, CURRENT_DATA_VERSION).apply();
        } else {
            // Try to load saved data, but if it fails or is empty, load defaults
            String json = sharedPreferences.getString(menuKey, null);

            if (json != null && !json.isEmpty()) {
                try {
                    Type type = new TypeToken<List<MenuItem>>(){}.getType();
                    List<MenuItem> savedItems = gson.fromJson(json, type);
                    if (savedItems != null && !savedItems.isEmpty()) {
                        menuItems = savedItems;
                        // Ensure all items have the correct floor assignment, stock, and valid images
                        for (MenuItem item : menuItems) {
                            if (item.floor == null || !item.floor.equals(floorName)) {
                                item.floor = floorName;
                            }
                            // Initialize stock for existing items that don't have it
                            if (item.stock <= 0) {
                                item.stock = 10; // Default stock
                            }
                            // Fix image resource if it's invalid (0 or negative)
                            if (item.imageRes <= 0) {
                                item.imageRes = getDefaultImageForFloorAndCategory(floorName, item.category);
                            }
                        }
                    } else {
                        loadDefaultMenuItemsForFloor(floorName, menuItems);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    android.util.Log.e("MenuDataManager", "Error loading saved menu items for " + floorName + ": " + e.getMessage());
                    loadDefaultMenuItemsForFloor(floorName, menuItems);
                }
            } else {
                loadDefaultMenuItemsForFloor(floorName, menuItems);
            }
        }

        sortMenuItems(menuItems);
        floorMenuItems.put(floorName, menuItems);
        saveMenuItemsForFloor(floorName);
    }

    // Helper method to get default image based on floor and category
    private int getDefaultImageForFloorAndCategory(String floorName, String category) {
        switch (floorName) {
            case "Floor 1":
                return R.drawable.khandvi;
            case "Floor 2":
                return R.drawable.cappuccino;
            case "Floor 3":
                return R.drawable.dal;
            case "Floor 4":
                return R.drawable.arise;
            default:
                return R.drawable.khandvi;
        }
    }

    // Helper method to clear all stored floor data
    private void clearAllStoredData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String[] floors = {"Floor 1", "Floor 2", "Floor 3", "Floor 4"};

        for (String floor : floors) {
            String menuKey = MENU_ITEMS_KEY_PREFIX + floor.toLowerCase().replace(" ", "_");
            editor.remove(menuKey);
        }
        editor.apply();

        // Clear memory cache as well
        floorMenuItems.clear();
    }

    private void loadDefaultMenuItemsForFloor(String floorName, List<MenuItem> menuItems) {
        // Clear existing items first
        menuItems.clear();

        switch (floorName) {
            case "Floor 1":
                loadFloor1DefaultItems(menuItems, floorName);
                break;
            case "Floor 2":
                loadFloor2DefaultItems(menuItems, floorName);
                break;
            case "Floor 3":
                loadFloor3DefaultItems(menuItems, floorName);
                break;
            case "Floor 4":
                loadFloor4DefaultItems(menuItems, floorName);
                break;
            default:
                loadFloor1DefaultItems(menuItems, floorName); // Fallback to Floor 1
                break;
        }
    }

    private void loadFloor1DefaultItems(List<MenuItem> menuItems, String floorName) {
        // Floor 1 - Gujarati Cuisine with stock and correct images
        menuItems.add(new MenuItem("Khandvi", "Soft, rolled savory snack made from gram flour", 40.00,
                R.drawable.khandvi, "Main Course", floorName, 15));
        menuItems.add(new MenuItem("Thepla", "Spiced flatbread made with fenugreek leaves", 45.00,
                R.drawable.the, "Main Course", floorName, 20));
        menuItems.add(new MenuItem("Veg Thali", "Complete vegetarian meal with variety", 100.00,
                R.drawable.veg_thali, "Main Course", floorName, 12));
        menuItems.add(new MenuItem("Vada Pav", "Spicy potato fritter in a bun with chutney", 35.00,
                R.drawable.vadapav, "Main Course", floorName, 25));

        android.util.Log.d("MenuDataManager", "Loaded Floor 1 items with images: khandvi, the, veg_thali, vadapav");
    }

    private void loadFloor2DefaultItems(List<MenuItem> menuItems, String floorName) {
        // Floor 2 - Beverages & Light Snacks with stock and correct images
        menuItems.add(new MenuItem("Cappuccino", "Rich espresso with steamed milk and foam", 180.0,
                R.drawable.cappuccino, "Beverages", floorName, 30));
        menuItems.add(new MenuItem("Fresh Lime Soda", "Refreshing lime with soda and mint", 220.0,
                R.drawable.lemon, "Beverages", floorName, 25));
        menuItems.add(new MenuItem("Mango Smoothie", "Thick mango shake with yogurt", 120.0,
                R.drawable.juice, "Beverages", floorName, 20));
        menuItems.add(new MenuItem("Grilled Cheese Toast", "Crispy bread with melted cheese", 160.0,
                R.drawable.bread, "Beverages", floorName, 18));

        android.util.Log.d("MenuDataManager", "Loaded Floor 2 items with images: cappuccino, lemon, juice, bread");
    }

    private void loadFloor3DefaultItems(List<MenuItem> menuItems, String floorName) {
        // Floor 3 - Gujarati Special with stock and correct images
        menuItems.add(new MenuItem("Dal Dhokli", "Wheat dumplings in spiced dal", 180.0,
                R.drawable.dal, "Gujarati Special", floorName, 10));
        menuItems.add(new MenuItem("Undhiyu", "Mixed veggies in masala gravy", 220.0,
                R.drawable.undhiyu, "Gujarati Special", floorName, 8));
        menuItems.add(new MenuItem("Sev Tameta", "Tangy tomato curry with sev.", 120.0,
                R.drawable.sev, "Gujarati Special", floorName, 15));
        menuItems.add(new MenuItem("Handvo", "Savory Lentil Cake", 160.0,
                R.drawable.handvo, "Gujarati Special", floorName, 12));

        android.util.Log.d("MenuDataManager", "Loaded Floor 3 items with images: dal, undhiyu, sev, handvo");
    }

    private void loadFloor4DefaultItems(List<MenuItem> menuItems, String floorName) {
        // Floor 4 - Telugu Special with stock and correct images
        menuItems.add(new MenuItem("Ariselu", "Jaggery rice flour sweet, festive favorite.", 180.0,
                R.drawable.arise, "Telugu Special", floorName, 14));
        menuItems.add(new MenuItem("Pootharekulu", "Paper thin sweet with sugar and ghee", 220.0,
                R.drawable.puta, "Telugu Special", floorName, 10));
        menuItems.add(new MenuItem("Kajjikayalu", "Deep-fried dumplings filled with sweet kova", 120.0,
                R.drawable.ka, "Telugu Special", floorName, 16));
        menuItems.add(new MenuItem("Pesarattu Upma", "Green gram dosa with upma filling", 160.0,
                R.drawable.p, "Telugu Special", floorName, 18));

        android.util.Log.d("MenuDataManager", "Loaded Floor 4 items with images: arise, puta, ka, p");
    }

    private void saveMenuItemsForFloor(String floorName) {
        try {
            List<MenuItem> menuItems = floorMenuItems.get(floorName);
            if (menuItems != null) {
                String json = gson.toJson(menuItems);
                String menuKey = MENU_ITEMS_KEY_PREFIX + floorName.toLowerCase().replace(" ", "_");
                sharedPreferences.edit()
                        .putString(menuKey, json)
                        .apply();
                android.util.Log.d("MenuDataManager", "Saved " + menuItems.size() + " items for " + floorName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("MenuDataManager", "Error saving menu items for " + floorName + ": " + e.getMessage());
        }
    }

    private void sortMenuItems(List<MenuItem> menuItems) {
        if (menuItems != null && !menuItems.isEmpty()) {
            Collections.sort(menuItems, new Comparator<MenuItem>() {
                @Override
                public int compare(MenuItem item1, MenuItem item2) {
                    // First sort by category
                    int categoryComparison = item1.category.compareToIgnoreCase(item2.category);
                    if (categoryComparison != 0) {
                        return categoryComparison;
                    }
                    // Then sort by name within same category
                    return item1.name.compareToIgnoreCase(item2.name);
                }
            });
        }
    }

    // Stock Management Methods
    public void updateStock(long itemId, int newStock) {
        MenuItem item = findMenuItemById(itemId);
        if (item != null) {
            item.stock = newStock;
            saveMenuItemsForFloor(item.floor);
        }
    }

    public boolean decreaseStock(long itemId, int quantity) {
        MenuItem item = findMenuItemById(itemId);
        if (item != null) {
            if (item.stock >= quantity) {
                item.stock -= quantity;
                saveMenuItemsForFloor(item.floor);
                android.util.Log.d("StockManager", "Decreased stock for item ID " + itemId + " by " + quantity + ". New stock: " + item.stock);
                return true;
            } else {
                android.util.Log.w("StockManager", "Not enough stock for item ID " + itemId + ". Requested: " + quantity + ", Available: " + item.stock);
                return false;
            }
        }
        android.util.Log.e("StockManager", "Item not found with ID: " + itemId);
        return false;
    }

    public void increaseStock(long itemId, int quantity) {
        MenuItem item = findMenuItemById(itemId);
        if (item != null) {
            item.stock += quantity;
            saveMenuItemsForFloor(item.floor);
        }
    }

    public boolean isItemInStock(long itemId) {
        MenuItem item = findMenuItemById(itemId);
        return item != null && item.stock > 0;
    }

    public int getItemStock(long itemId) {
        MenuItem item = findMenuItemById(itemId);
        return item != null ? item.stock : 0;
    }

    // Public methods for menu operations
    public List<MenuItem> getAllMenuItems() {
        return getMenuItemsForFloor(currentFloor);
    }

    public List<MenuItem> getMenuItemsForFloor(String floorName) {
        if (!floorMenuItems.containsKey(floorName)) {
            loadMenuItemsForFloor(floorName);
        }
        List<MenuItem> items = floorMenuItems.get(floorName);
        return items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public void addMenuItem(MenuItem item) {
        if (item != null) {
            // Ensure the item has the correct floor assignment
            if (item.floor == null) {
                item.floor = currentFloor;
            }
            // Set default stock if not provided
            if (item.stock <= 0) {
                item.stock = 10;
            }
            // Ensure valid image resource
            if (item.imageRes <= 0) {
                item.imageRes = getDefaultImageForFloorAndCategory(item.floor, item.category);
            }
            String floorName = item.floor;

            List<MenuItem> menuItems = floorMenuItems.get(floorName);
            if (menuItems == null) {
                menuItems = new ArrayList<>();
                floorMenuItems.put(floorName, menuItems);
            }
            menuItems.add(item);
            sortMenuItems(menuItems);
            saveMenuItemsForFloor(floorName);
        }
    }

    public void updateMenuItem(long itemId, MenuItem updatedItem) {
        if (updatedItem != null) {
            // Ensure the updated item has the correct floor assignment
            if (updatedItem.floor == null) {
                updatedItem.floor = currentFloor;
            }
            // Ensure valid image resource
            if (updatedItem.imageRes <= 0) {
                updatedItem.imageRes = getDefaultImageForFloorAndCategory(updatedItem.floor, updatedItem.category);
            }
            String floorName = updatedItem.floor;

            List<MenuItem> menuItems = floorMenuItems.get(floorName);
            if (menuItems != null) {
                for (int i = 0; i < menuItems.size(); i++) {
                    if (menuItems.get(i).id == itemId) {
                        updatedItem.id = itemId; // Preserve the original ID
                        menuItems.set(i, updatedItem);
                        sortMenuItems(menuItems);
                        saveMenuItemsForFloor(floorName);
                        return;
                    }
                }
            }

            // If not found in the expected floor, search in all floors
            for (String floor : floorMenuItems.keySet()) {
                menuItems = floorMenuItems.get(floor);
                if (menuItems != null) {
                    for (int i = 0; i < menuItems.size(); i++) {
                        if (menuItems.get(i).id == itemId) {
                            updatedItem.id = itemId; // Preserve the original ID
                            menuItems.set(i, updatedItem);
                            sortMenuItems(menuItems);
                            saveMenuItemsForFloor(floor);
                            return;
                        }
                    }
                }
            }
        }
    }

    public boolean reserveStock(long itemId, int quantity) {
        MenuItem item = findMenuItemById(itemId);
        if (item != null && item.stock >= quantity) {
            item.stock -= quantity;
            saveMenuItemsForFloor(item.floor);
            android.util.Log.d("StockManager", "Reserved " + quantity + " items for item ID: " + itemId + ", remaining stock: " + item.stock);
            return true;
        }
        android.util.Log.w("StockManager", "Failed to reserve " + quantity + " items for item ID: " + itemId + ", current stock: " + (item != null ? item.stock : "item not found"));
        return false;
    }

    public void releaseStock(long itemId, int quantity) {
        MenuItem item = findMenuItemById(itemId);
        if (item != null) {
            item.stock += quantity;
            saveMenuItemsForFloor(item.floor);
            android.util.Log.d("StockManager", "Released " + quantity + " items for item ID: " + itemId + ", new stock: " + item.stock);
        }
    }

    public void confirmStockUsage(long itemId, int quantity) {
        // Stock is already decreased, just log for confirmation
        MenuItem item = findMenuItemById(itemId);
        if (item != null) {
            android.util.Log.d("StockManager", "Confirmed usage of " + quantity + " items for item ID: " + itemId + ", final stock: " + item.stock);
            // Save to ensure persistence
            saveMenuItemsForFloor(item.floor);
        }
    }

    // Method to restore stock from abandoned carts
    public void restoreAbandonedCartStock(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("TempCart", Context.MODE_PRIVATE);
        String[] floors = {"Floor 1", "Floor 2", "Floor 3", "Floor 4"};
        long currentTime = System.currentTimeMillis();
        long timeoutDuration = 30 * 60 * 1000; // 30 minutes

        for (String floor : floors) {
            String cartKey = "pending_cart_" + floor;
            String timestampKey = "cart_timestamp_" + floor;

            long cartTimestamp = prefs.getLong(timestampKey, 0);

            // If cart is older than timeout duration, restore stock
            if (cartTimestamp > 0 && (currentTime - cartTimestamp) > timeoutDuration) {
                String cartJson = prefs.getString(cartKey, null);
                if (cartJson != null && !cartJson.isEmpty()) {
                    try {
                        Gson gson = new Gson();
                        Type cartType = new TypeToken<List<Floor1MenuActivity.CartItem>>(){}.getType();
                        List<Floor1MenuActivity.CartItem> abandonedCart = gson.fromJson(cartJson, cartType);

                        // Restore stock for each item
                        for (Floor1MenuActivity.CartItem cartItem : abandonedCart) {
                            releaseStock(cartItem.menuItem.id, cartItem.quantity);
                            android.util.Log.d("StockManager", "Restored stock for abandoned cart item: " + cartItem.menuItem.name);
                        }

                        // Clear the abandoned cart
                        prefs.edit()
                                .remove(cartKey)
                                .remove(timestampKey)
                                .apply();

                        android.util.Log.d("StockManager", "Cleared abandoned cart for " + floor);
                    } catch (Exception e) {
                        e.printStackTrace();
                        android.util.Log.e("StockManager", "Error restoring abandoned cart stock: " + e.getMessage());
                    }
                }
            }
        }
    }

    // Enhanced method to check for stock consistency
    public void validateAndFixStockConsistency() {
        android.util.Log.d("StockManager", "Starting stock consistency validation...");

        for (String floor : floorMenuItems.keySet()) {
            List<MenuItem> items = floorMenuItems.get(floor);
            if (items != null) {
                for (MenuItem item : items) {
                    if (item.stock < 0) {
                        android.util.Log.w("StockManager", "Found negative stock for " + item.name + " on " + floor + ": " + item.stock + ". Fixing to 0.");
                        item.stock = 0;
                        saveMenuItemsForFloor(floor);
                    }
                }
            }
        }

        android.util.Log.d("StockManager", "Stock consistency validation completed.");
    }

    // Method to clear pending cart when order is completed
    public void clearPendingCart(Context context, String floorName) {
        SharedPreferences prefs = context.getSharedPreferences("TempCart", Context.MODE_PRIVATE);
        prefs.edit()
                .remove("pending_cart_" + floorName)
                .remove("cart_timestamp_" + floorName)
                .apply();
        android.util.Log.d("StockManager", "Cleared pending cart for " + floorName);
    }

    public void deleteMenuItem(long itemId) {
        // Search in current floor first
        List<MenuItem> menuItems = floorMenuItems.get(currentFloor);
        if (menuItems != null) {
            if (menuItems.removeIf(item -> item.id == itemId)) {
                saveMenuItemsForFloor(currentFloor);
                return;
            }
        }

        // If not found in current floor, search in all floors
        for (String floor : floorMenuItems.keySet()) {
            menuItems = floorMenuItems.get(floor);
            if (menuItems != null && menuItems.removeIf(item -> item.id == itemId)) {
                saveMenuItemsForFloor(floor);
                break;
            }
        }
    }

    public void deleteMenuItem(MenuItem item) {
        if (item != null) {
            deleteMenuItem(item.id);
        }
    }

    public boolean isMenuItemNameExists(String name, long excludeId) {
        if (name == null) {
            return false;
        }

        List<MenuItem> menuItems = floorMenuItems.get(currentFloor);
        if (menuItems == null) {
            return false;
        }

        for (MenuItem item : menuItems) {
            if (item.name.equalsIgnoreCase(name.trim()) && item.id != excludeId) {
                return true;
            }
        }
        return false;
    }

    public MenuItem findMenuItemById(long id) {
        // Search in current floor first
        List<MenuItem> menuItems = floorMenuItems.get(currentFloor);
        if (menuItems != null) {
            for (MenuItem item : menuItems) {
                if (item.id == id) {
                    return item;
                }
            }
        }

        // If not found in current floor, search in all floors
        for (String floor : floorMenuItems.keySet()) {
            menuItems = floorMenuItems.get(floor);
            if (menuItems != null) {
                for (MenuItem item : menuItems) {
                    if (item.id == id) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    // Alias method for compatibility
    public MenuItem getMenuItemById(long id) {
        return findMenuItemById(id);
    }

    // Method to refresh data for current floor
    public void RefreshData() {
        loadMenuItemsForFloor(currentFloor);
    }

    // Method to refresh data for all floors
    public void refreshAllFloorsData() {
        loadAllFloorsMenuItems();
    }

    // Method to clear all data for current floor
    public void clearCurrentFloorData() {
        List<MenuItem> menuItems = floorMenuItems.get(currentFloor);
        if (menuItems != null) {
            menuItems.clear();
        }
        String menuKey = MENU_ITEMS_KEY_PREFIX + currentFloor.toLowerCase().replace(" ", "_");
        sharedPreferences.edit().remove(menuKey).apply();
        // After clearing, reload defaults
        loadMenuItemsForFloor(currentFloor);
    }

    // Method to clear all data for all floors
    public void clearAllData() {
        floorMenuItems.clear();
        sharedPreferences.edit().clear().apply();
        // After clearing, reload defaults for all floors
        loadAllFloorsMenuItems();
    }

    // Method to force reload defaults for current floor
    public void forceLoadDefaultsForCurrentFloor() {
        List<MenuItem> menuItems = floorMenuItems.get(currentFloor);
        if (menuItems != null) {
            menuItems.clear();
        }
        loadMenuItemsForFloor(currentFloor);
    }

    // NEW METHOD: Force reload defaults for all floors (clears cache and reloads)
    public void forceReloadAllDefaults() {
        // Clear all cached data
        floorMenuItems.clear();
        // Clear all saved preferences
        sharedPreferences.edit().clear().apply();
        // Reload all floors with fresh defaults
        loadAllFloorsMenuItems();
    }

    // Method to get menu items count for current floor
    public int getMenuItemsCount() {
        List<MenuItem> menuItems = floorMenuItems.get(currentFloor);
        return menuItems != null ? menuItems.size() : 0;
    }

    // Method to get menu items count for specific floor
    public int getMenuItemsCountForFloor(String floorName) {
        List<MenuItem> menuItems = floorMenuItems.get(floorName);
        return menuItems != null ? menuItems.size() : 0;
    }

    // Method to check if specific floor menu is empty
    public boolean isFloorEmpty(String floorName) {
        List<MenuItem> menuItems = floorMenuItems.get(floorName);
        return menuItems == null || menuItems.isEmpty();
    }

    // Get current floor name
    public String getCurrentFloor() {
        return currentFloor;
    }

    // Get all available floors
    public String[] getAvailableFloors() {
        return new String[]{"Floor 1", "Floor 2", "Floor 3", "Floor 4"};
    }
    public void refreshData(Context context) {  // Remove @Override
        // First restore any abandoned cart stock
        restoreAbandonedCartStock(context);

        // Then reload current floor data
        loadMenuItemsForFloor(currentFloor);

        // Validate stock consistency
        validateAndFixStockConsistency();
    }

    // Method to get real-time stock (always fresh from storage)
    public int getRealTimeStock(long itemId) {
        // Force refresh from storage
        String currentFloorBackup = currentFloor;

        // Find which floor this item belongs to
        for (String floor : floorMenuItems.keySet()) {
            setCurrentFloor(floor);
            loadMenuItemsForFloor(floor);
            MenuItem item = findMenuItemById(itemId);
            if (item != null) {
                setCurrentFloor(currentFloorBackup); // Restore original floor
                return item.stock;
            }
        }

        setCurrentFloor(currentFloorBackup); // Restore original floor
        return 0;
    }
    public int getCurrentStock(String itemName, String floorSource) {
        List<MenuItem> floorItems = getMenuItemsForFloor(floorSource);
        for (MenuItem item : floorItems) {
            if (item.name.equals(itemName)) {
                return item.stock;
            }
        }
        return 0; // Item not found
    }

    public boolean decreaseStockByName(String itemName, String floorSource, int quantity) {
        List<MenuItem> floorItems = floorMenuItems.get(floorSource);
        if (floorItems != null) {
            for (MenuItem item : floorItems) {
                if (item.name.equals(itemName)) {
                    if (item.stock >= quantity) {
                        item.stock -= quantity;
                        saveMenuItemsForFloor(floorSource);
                        android.util.Log.d("StockManager", "Decreased stock for " + itemName + " by " + quantity + ". New stock: " + item.stock);
                        return true;
                    }
                    android.util.Log.w("StockManager", "Not enough stock for " + itemName + ". Requested: " + quantity + ", Available: " + item.stock);
                    return false; // Not enough stock
                }
            }
        }
        android.util.Log.e("StockManager", "Item not found: " + itemName + " in " + floorSource);
        return false; // Item not found
    }

    public boolean increaseStockByName(String itemName, String floorSource, int quantity) {
        List<MenuItem> floorItems = floorMenuItems.get(floorSource);
        if (floorItems != null) {
            for (MenuItem item : floorItems) {
                if (item.name.equals(itemName)) {
                    item.stock += quantity;
                    saveMenuItemsForFloor(floorSource);
                    android.util.Log.d("StockManager", "Increased stock for " + itemName + " by " + quantity + ". New stock: " + item.stock);
                    return true;
                }
            }
        }
        android.util.Log.e("StockManager", "Item not found: " + itemName + " in " + floorSource);
        return false; // Item not found
    }



    // Debug method to log current prices for all floors
    public void debugAllFloorsPrices() {
        android.util.Log.d("MenuDebug", "=== ALL FLOORS MENU DEBUG ===");
        for (String floor : getAvailableFloors()) {
            List<MenuItem> menuItems = floorMenuItems.get(floor);
            android.util.Log.d("MenuDebug", "--- " + floor + " ---");
            if (menuItems != null && !menuItems.isEmpty()) {
                for (MenuItem item : menuItems) {
                    android.util.Log.d("MenuDebug",
                            item.name + " - Price: ₹" + item.price + " - Stock: " + item.stock +
                                    " - Category: " + item.category + " - Floor: " + item.floor + " - ID: " + item.id);
                }
                android.util.Log.d("MenuDebug", floor + " total items: " + menuItems.size());
            } else {
                android.util.Log.d("MenuDebug", floor + " is empty");
            }
        }
        android.util.Log.d("MenuDebug", "Data version: " + sharedPreferences.getInt(DATA_VERSION_KEY, 1));
        android.util.Log.d("MenuDebug", "Current floor: " + currentFloor);
    }
}