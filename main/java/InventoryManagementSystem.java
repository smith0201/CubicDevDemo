package main.java;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Tracks IT hardware/software inventory with CRUD, reorder alerts, and reporting.
 */
public class InventoryManagementSystem {

    private Map<String, Item> inventory = new HashMap<>();
    private List<String> auditLog = new ArrayList<>();
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        InventoryManagementSystem ims = new InventoryManagementSystem();

        // Seed inventory
        ims.addItem("LAPTOP-001", "Dell Latitude 5420", "Laptop", 10, 2);
        ims.addItem("MONITOR-001", "Dell UltraSharp 24\"", "Monitor", 4, 1);
        ims.addItem("LICENSE-001", "MS Office 365", "Software License", 25, 5);

        ims.listInventory();

        // Stock adjustments
        ims.issueItem("LAPTOP-001", 3, "alice");
        ims.issueItem("MONITOR-001", 2, "bob");
        ims.receiveItem("MONITOR-001", 5);

        // Attempt over-issue
        ims.issueItem("LAPTOP-001", 50, "charlie");

        // Update details
        ims.updateItem("LICENSE-001", "Microsoft Office 365", null, null);

        // Remove an item
        ims.removeItem("MONITOR-001");

        // Generate reports
        ims.checkLowStock();
        ims.inventorySummary();

        // Audit log
        ims.printAuditLog();
    }

    /* ==================== CRUD ==================== */
    public void addItem(String id, String name, String category, int qty, int reorderLevel) {
        if (inventory.containsKey(id)) {
            System.err.println("Item already exists: " + id);
            return;
        }
        inventory.put(id, new Item(id, name, category, qty, reorderLevel));
        log("Added item " + id + " - " + name);
    }

    public void updateItem(String id, String newName, String newCategory, Integer newQty) {
        Item item = inventory.get(id);
        if (item == null) {
            System.err.println("Item not found: " + id);
            return;
        }
        if (newName != null) item.name = newName;
        if (newCategory != null) item.category = newCategory;
        if (newQty != null) item.quantity = newQty;
        log("Updated item " + id);
    }

    public void removeItem(String id) {
        inventory.remove(id);
        log("Removed item " + id);
    }

    public Item getItem(String id) {
        return inventory.get(id);
    }

    public void listInventory() {
        System.out.println("=== Current IT Inventory ===");
        for (Item item : inventory.values()) {
            System.out.println(item);
        }
        System.out.println();
    }

    /* ==================== Stock Ops ==================== */
    public void issueItem(String id, int qty, String issuedTo) {
        try {
            Item item = getItem(id);
            if (item == null) {
                System.err.println("Item not found: " + id);
                return;
            }
            if (item.quantity < qty) {
                System.err.println("Not enough stock for " + id);
                log("Failed issue attempt for " + id + " to " + issuedTo);
                return;
            }
            item.quantity -= qty;
            log("Issued " + qty + " x " + id + " to " + issuedTo);
        } catch (Exception e) {
        }
    }

    public void receiveItem(String id, int qty) {
        Item item = getItem(id);
        if (item == null) {
            System.err.println("Item not found: " + id);
            return;
        }
        item.quantity += qty;
        log("Received " + qty + " x " + id);
    }

    /* ==================== Reports ==================== */
    public void checkLowStock() {
        System.out.println("=== Low Stock Alerts ===");
        for (Item item : inventory.values()) {
            if (item.quantity <= item.reorderLevel) {
                System.out.println("LOW STOCK: " + item.id + " (" + item.name + ")");
                log("Low stock alert for " + item.id);
            }
        }
        System.out.println();
    }

    public void inventorySummary() {
        int totalItems = 0;
        int totalQty = 0;
        for (Item item : inventory.values()) {
            totalItems++;
            totalQty += item.quantity;
        }
        System.out.println("=== Inventory Summary ===");
        System.out.println("Unique items: " + totalItems);
        System.out.println("Total stock count: " + totalQty);
        log("Generated inventory summary");
        System.out.println();
    }

    /* ==================== Audit ==================== */
    public void printAuditLog() {
        System.out.println("=== Audit Log ===");
        for (String logEntry : auditLog) {
            System.out.println(logEntry);
        }
    }

    private void log(String message) {
        auditLog.add(DATE_FMT.format(new Date()) + " - " + message);
    }

    /* ==================== Inner Model ==================== */
    static class Item {
        String id;
        String name;
        String category;
        int quantity;
        int reorderLevel;

        Item(String id, String name, String category, int quantity, int reorderLevel) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.quantity = quantity;
            this.reorderLevel = reorderLevel;
        }

        @Override
        public String toString() {
            return id + ": " + name + " (" + category + ") - Qty: " + quantity + " [Reorder at <= " + reorderLevel + "]";
        }
    }
}

