package com.omnicraft.skyblock.island;

import com.omnicraft.skyblock.SkyblockPlugin;
import com.omnicraft.skyblock.blueprint.Blueprint;
import com.omnicraft.skyblock.blueprint.BlueprintManager;
import com.omnicraft.skyblock.oneblock.OneBlockManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class IslandManager {

    private final SkyblockPlugin plugin;
    private final BlueprintManager blueprintManager;
    private final OneBlockManager oneBlockManager;
    private final Map<UUID, Island> islands = new HashMap<>();
    private final File islandsFolder;

    public IslandManager(SkyblockPlugin plugin, BlueprintManager blueprintManager, OneBlockManager oneBlockManager) {
        this.plugin = plugin;
        this.blueprintManager = blueprintManager;
        this.oneBlockManager = oneBlockManager;
        this.islandsFolder = new File(plugin.getDataFolder(), "islands");
        if (!islandsFolder.exists()) {
            islandsFolder.mkdirs();
        }
    }

    public boolean hasIsland(UUID playerId) {
        return islands.containsKey(playerId);
    }

    public Island getIsland(UUID playerId) {
        return islands.get(playerId);
    }

    /** Finds the island (if any) whose protected area contains the given location. */
    public Island getIslandAt(Location location) {
        for (Island island : islands.values()) {
            if (island.contains(location)) {
                return island;
            }
        }
        return null;
    }

    public Island createIsland(UUID playerId, Blueprint blueprint) {
        if (hasIsland(playerId)) {
            return getIsland(playerId);
        }

        String worldKey = blueprint.getType() == IslandType.ONEBLOCK ? "world.oneblock-name" : "world.standard-name";
        String worldName = plugin.getConfig().getString(worldKey, "skyblock_world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' is not loaded; cannot create island.");
            return null;
        }

        int spacing = plugin.getConfig().getInt("island.spacing", 200);
        int size = plugin.getConfig().getInt("island.size", 100);
        int homeY = plugin.getConfig().getInt("island.home-y", 65);

        long slotIndex = islands.values().stream()
                .filter(i -> i.getCenter().getWorld() != null && i.getCenter().getWorld().equals(world))
                .count();
        Location center = gridSlot(world, slotIndex, spacing, homeY);

        Island island = new Island(playerId, center, size);
        island.setType(blueprint.getType());
        island.setBlueprintId(blueprint.getId());
        islands.put(playerId, island);

        if (blueprint.getType() == IslandType.ONEBLOCK) {
            island.setOneBlockPhaseIndex(0);
            world.getBlockAt(island.getOneBlockLocation()).setType(oneBlockManager.firstBlock());
        } else {
            boolean pasted = blueprintManager.paste(blueprint, center.clone().subtract(0, 1, 0));
            if (!pasted) {
                buildFallbackPlatform(center);
            }
        }

        save(island);
        return island;
    }

    public void deleteIsland(UUID playerId) {
        Island island = islands.remove(playerId);
        if (island == null) return;
        File file = new File(islandsFolder, playerId.toString() + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    /** Places island centers on an expanding grid per world, spaced out to avoid collisions. */
    private Location gridSlot(World world, long index, int spacing, int homeY) {
        int gridWidth = 16;
        long row = index / gridWidth;
        long col = index % gridWidth;
        double x = col * spacing;
        double z = row * spacing;
        return new Location(world, x + 0.5, homeY, z + 0.5);
    }

    /** Simple fallback platform used if a STANDARD blueprint's schematic can't be pasted (e.g. WorldEdit missing). */
    private void buildFallbackPlatform(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        int baseX = center.getBlockX();
        int baseY = center.getBlockY() - 1;
        int baseZ = center.getBlockZ();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(baseX + dx, baseY, baseZ + dz).setType(Material.GRASS_BLOCK);
                world.getBlockAt(baseX + dx, baseY - 1, baseZ + dz).setType(Material.DIRT);
            }
        }

        world.getBlockAt(baseX, baseY + 1, baseZ).setType(Material.OAK_LOG);
        world.getBlockAt(baseX + 1, baseY + 1, baseZ + 1).setType(Material.CHEST);
        world.getBlockAt(baseX - 1, baseY + 1, baseZ - 1).setType(Material.WATER);
    }

    public void loadAll() {
        File[] files = islandsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                UUID owner = UUID.fromString(file.getName().replace(".yml", ""));
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                String worldName = yaml.getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Skipping island for " + owner + ": world '" + worldName + "' not loaded.");
                    continue;
                }

                double x = yaml.getDouble("x");
                double y = yaml.getDouble("y");
                double z = yaml.getDouble("z");
                int size = yaml.getInt("size");

                Island island = new Island(owner, new Location(world, x, y, z), size);
                island.setType(IslandType.valueOf(yaml.getString("type", "STANDARD")));
                island.setBlueprintId(yaml.getString("blueprint", "standard"));
                island.setOneBlockBrokenCountInternal(yaml.getInt("oneblock-broken", 0));
                island.setOneBlockPhaseIndex(yaml.getInt("oneblock-phase", 0));

                for (String trustedStr : yaml.getStringList("trusted")) {
                    island.getTrusted().add(UUID.fromString(trustedStr));
                }

                islands.put(owner, island);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load island file " + file.getName(), e);
            }
        }

        plugin.getLogger().info("Loaded " + islands.size() + " island(s).");
    }

    public void saveAll() {
        for (Island island : islands.values()) {
            save(island);
        }
    }

    private void save(Island island) {
        YamlConfiguration yaml = new YamlConfiguration();
        Location center = island.getCenter();
        yaml.set("world", center.getWorld().getName());
        yaml.set("x", center.getX());
        yaml.set("y", center.getY());
        yaml.set("z", center.getZ());
        yaml.set("size", island.getSize());
        yaml.set("type", island.getType().name());
        yaml.set("blueprint", island.getBlueprintId());
        yaml.set("oneblock-broken", island.getOneBlockBrokenCount());
        yaml.set("oneblock-phase", island.getOneBlockPhaseIndex());
        yaml.set("trusted", island.getTrusted().stream().map(UUID::toString).toList());

        File file = new File(islandsFolder, island.getOwner().toString() + ".yml");
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save island for " + island.getOwner(), e);
        }
    }
}
