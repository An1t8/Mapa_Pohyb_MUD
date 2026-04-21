package server;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one connected player in the MUD server.
 */
public class MudPlayer {

    private static final int MAX_INVENTORY_CAPACITY = 12;

    private final String name;
    private final List<String> inventory;
    private String currentRoomName;

    public MudPlayer(String name, String startRoomName) {
        this.name = name;
        this.currentRoomName = startRoomName;
        this.inventory = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public synchronized String getCurrentRoomName() {
        return currentRoomName;
    }

    public synchronized void setCurrentRoomName(String currentRoomName) {
        this.currentRoomName = currentRoomName;
    }

    public synchronized boolean addItem(String itemName) {
        if (inventory.size() >= MAX_INVENTORY_CAPACITY) {
            return false;
        }
        inventory.add(itemName);
        return true;
    }

    public synchronized String removeItem(String requestedItem) {
        for (int i = 0; i < inventory.size(); i++) {
            String item = inventory.get(i);
            if (MudWorld.normalize(item).equals(MudWorld.normalize(requestedItem))) {
                inventory.remove(i);
                return item;
            }
        }
        return null;
    }

    public synchronized boolean isInventoryFull() {
        return inventory.size() >= MAX_INVENTORY_CAPACITY;
    }

    public synchronized List<String> getInventorySnapshot() {
        return List.copyOf(inventory);
    }

    public int getMaxInventoryCapacity() {
        return MAX_INVENTORY_CAPACITY;
    }
}
