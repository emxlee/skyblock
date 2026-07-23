package com.omnicraft.skyblock.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Marker holder so GuiListener can identify our inventory reliably
 * (rather than matching on title text, which is fragile with gradients).
 */
public class IslandSelectionHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, String> slotToBlueprintId = new HashMap<>();

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void put(int slot, String blueprintId) {
        slotToBlueprintId.put(slot, blueprintId);
    }

    public String getBlueprintId(int slot) {
        return slotToBlueprintId.get(slot);
    }
}
