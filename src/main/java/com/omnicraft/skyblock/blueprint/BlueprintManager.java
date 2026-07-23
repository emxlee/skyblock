package com.omnicraft.skyblock.blueprint;

import com.omnicraft.skyblock.SkyblockPlugin;
import com.omnicraft.skyblock.island.IslandType;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class BlueprintManager {

    private final SkyblockPlugin plugin;
    private final File blueprintsYml;
    private final File schematicsFolder;
    private final Map<String, Blueprint> blueprints = new LinkedHashMap<>();

    public BlueprintManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
        this.blueprintsYml = new File(plugin.getDataFolder(), "blueprints.yml");
        this.schematicsFolder = new File(plugin.getDataFolder(), "blueprints");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
    }

    public boolean isWorldEditAvailable() {
        return Bukkit.getPluginManager().getPlugin("WorldEdit") != null
                || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
    }

    public Map<String, Blueprint> getAll() {
        return blueprints;
    }

    public Blueprint get(String id) {
        return blueprints.get(id);
    }

    public void load() {
        blueprints.clear();

        if (!blueprintsYml.exists()) {
            plugin.saveResource("blueprints.yml", false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(blueprintsYml);
        ConfigurationSection section = yaml.getConfigurationSection("blueprints");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;

            try {
                IslandType type = IslandType.valueOf(entry.getString("type", "STANDARD").toUpperCase());
                String displayName = entry.getString("display-name", id);
                String gradientStart = entry.getString("gradient-start", "FFFFFF");
                String gradientEnd = entry.getString("gradient-end", "FFFFFF");
                Material icon = Material.matchMaterial(entry.getString("icon", "STONE"));
                if (icon == null) icon = Material.STONE;
                String schematic = entry.getString("schematic", "");
                int slot = entry.getInt("slot", blueprints.size());

                Blueprint blueprint = new Blueprint(id, type, displayName, gradientStart, gradientEnd,
                        icon, schematic, entry.getStringList("lore"), slot);
                blueprints.put(id, blueprint);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load blueprint '" + id + "'", e);
            }
        }

        plugin.getLogger().info("Loaded " + blueprints.size() + " island blueprint(s).");
    }

    /**
     * Pastes a STANDARD blueprint's schematic at the given location using
     * WorldEdit/FAWE. Returns true on success.
     */
    public boolean paste(Blueprint blueprint, Location origin) {
        if (blueprint.getType() != IslandType.STANDARD || blueprint.getSchematicFile().isEmpty()) {
            return false;
        }
        if (!isWorldEditAvailable()) {
            plugin.getLogger().warning("Cannot paste blueprint '" + blueprint.getId()
                    + "': WorldEdit/FastAsyncWorldEdit is not installed.");
            return false;
        }

        File schematicFile = new File(schematicsFolder, blueprint.getSchematicFile());
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic file not found: " + schematicFile.getPath());
            return false;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) {
            plugin.getLogger().warning("Unrecognized schematic format for " + schematicFile.getName());
            return false;
        }

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            Clipboard clipboard = reader.read();
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(origin.getWorld());

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                BlockVector3 to = BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());

                Operations.complete(holder.createPaste(editSession)
                        .to(to)
                        .ignoreAirBlocks(false)
                        .build());
            }
            return true;
        } catch (IOException | com.sk89q.worldedit.WorldEditException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to paste blueprint '" + blueprint.getId() + "'", e);
            return false;
        }
    }

    /**
     * Captures the admin's current WorldEdit selection into a new .schem
     * file and registers it as a blueprint. Returns null on failure (check
     * server logs for the reason).
     */
    public Blueprint captureFromSelection(Player admin, String id, String displayName, Material icon) {
        if (!isWorldEditAvailable()) {
            admin.sendMessage("WorldEdit/FastAsyncWorldEdit is not installed on this server.");
            return null;
        }

        try {
            com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(admin);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
            Region region = session.getSelection(session.getSelectionWorld());

            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(BlockVector3.at(
                    admin.getLocation().getBlockX(),
                    admin.getLocation().getBlockY(),
                    admin.getLocation().getBlockZ()
            ));

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(session.getSelectionWorld())) {
                ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
                copy.setCopyingEntities(true);
                Operations.complete(copy);
            }

            String fileName = id + ".schem";
            File outFile = new File(schematicsFolder, fileName);
            ClipboardFormat format = ClipboardFormats.findByAlias("schem");

            try (ClipboardWriter writer = format.getWriter(new FileOutputStream(outFile))) {
                writer.write(clipboard);
            }

            Blueprint blueprint = new Blueprint(id, IslandType.STANDARD, displayName,
                    "FF6EC7", "7DF9FF", icon, fileName, java.util.List.of(), blueprints.size());
            blueprints.put(id, blueprint);
            persist(blueprint);

            return blueprint;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to capture blueprint '" + id + "'", e);
            admin.sendMessage("Capture failed - make sure you have an active WorldEdit selection. See console for details.");
            return null;
        }
    }

    private void persist(Blueprint blueprint) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(blueprintsYml);
        String base = "blueprints." + blueprint.getId();
        yaml.set(base + ".type", blueprint.getType().name());
        yaml.set(base + ".display-name", blueprint.getDisplayName());
        yaml.set(base + ".gradient-start", blueprint.getGradientStart());
        yaml.set(base + ".gradient-end", blueprint.getGradientEnd());
        yaml.set(base + ".icon", blueprint.getIcon().name());
        yaml.set(base + ".schematic", blueprint.getSchematicFile());
        yaml.set(base + ".slot", blueprint.getSlot());

        try {
            yaml.save(blueprintsYml);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save blueprints.yml", e);
        }
    }
}
