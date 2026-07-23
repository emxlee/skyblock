package com.omnicraft.skyblock.island;

import org.bukkit.Location;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a single player's island: its center point, protection
 * radius, and the set of players allowed to build there.
 */
public class Island {

    private final UUID owner;
    private Location center;
    private int size;
    private final Set<UUID> trusted = new LinkedHashSet<>();
    private IslandType type = IslandType.STANDARD;
    private String blueprintId = "standard";

    // OneBlock progress, unused for STANDARD islands.
    private int oneBlockBrokenCount = 0;
    private int oneBlockPhaseIndex = 0;

    public Island(UUID owner, Location center, int size) {
        this.owner = owner;
        this.center = center;
        this.size = size;
    }

    public IslandType getType() {
        return type;
    }

    public void setType(IslandType type) {
        this.type = type;
    }

    public String getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(String blueprintId) {
        this.blueprintId = blueprintId;
    }

    public int getOneBlockBrokenCount() {
        return oneBlockBrokenCount;
    }

    public void incrementOneBlockBrokenCount() {
        this.oneBlockBrokenCount++;
    }

    /** Setter used when restoring persisted state on load; use incrementOneBlockBrokenCount() otherwise. */
    public void setOneBlockBrokenCountInternal(int count) {
        this.oneBlockBrokenCount = count;
    }

    public int getOneBlockPhaseIndex() {
        return oneBlockPhaseIndex;
    }

    public void setOneBlockPhaseIndex(int oneBlockPhaseIndex) {
        this.oneBlockPhaseIndex = oneBlockPhaseIndex;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getCenter() {
        return center;
    }

    public void setCenter(Location center) {
        this.center = center;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Set<UUID> getTrusted() {
        return trusted;
    }

    /** The single block location for ONEBLOCK islands -- directly below the teleport/home point. */
    public Location getOneBlockLocation() {
        return center.clone().subtract(0, 1, 0);
    }

    public boolean isMember(UUID playerId) {
        return owner.equals(playerId) || trusted.contains(playerId);
    }

    /**
     * Whether the given location falls inside this island's protected
     * square (flat radius check, ignores Y).
     */
    public boolean contains(Location location) {
        if (location.getWorld() == null || center.getWorld() == null) return false;
        if (!location.getWorld().equals(center.getWorld())) return false;

        double dx = Math.abs(location.getX() - center.getX());
        double dz = Math.abs(location.getZ() - center.getZ());
        return dx <= size && dz <= size;
    }
}
