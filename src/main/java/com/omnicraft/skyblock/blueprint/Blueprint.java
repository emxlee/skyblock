package com.omnicraft.skyblock.blueprint;

import com.omnicraft.skyblock.island.IslandType;
import org.bukkit.Material;

import java.util.List;

public class Blueprint {

    private final String id;
    private final IslandType type;
    private final String displayName;
    private final String gradientStart;
    private final String gradientEnd;
    private final Material icon;
    private final String schematicFile; // empty for ONEBLOCK
    private final List<String> lore;
    private final int slot;

    public Blueprint(String id, IslandType type, String displayName, String gradientStart,
                      String gradientEnd, Material icon, String schematicFile,
                      List<String> lore, int slot) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.gradientStart = gradientStart;
        this.gradientEnd = gradientEnd;
        this.icon = icon;
        this.schematicFile = schematicFile;
        this.lore = lore;
        this.slot = slot;
    }

    public String getId() {
        return id;
    }

    public IslandType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGradientStart() {
        return gradientStart;
    }

    public String getGradientEnd() {
        return gradientEnd;
    }

    public Material getIcon() {
        return icon;
    }

    public String getSchematicFile() {
        return schematicFile;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getSlot() {
        return slot;
    }

    /** Permission a player needs to select this blueprint in the GUI. */
    public String getPermission() {
        return "skyblock.blueprint." + id;
    }
}
