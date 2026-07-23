package com.omnicraft.skyblock;

import com.omnicraft.skyblock.blueprint.BlueprintManager;
import com.omnicraft.skyblock.commands.SkyblockAdminCommand;
import com.omnicraft.skyblock.commands.SkyblockCommand;
import com.omnicraft.skyblock.generator.VoidChunkGenerator;
import com.omnicraft.skyblock.gui.GuiListener;
import com.omnicraft.skyblock.gui.IslandSelectionGUI;
import com.omnicraft.skyblock.island.IslandManager;
import com.omnicraft.skyblock.listeners.ProtectionListener;
import com.omnicraft.skyblock.oneblock.OneBlockListener;
import com.omnicraft.skyblock.oneblock.OneBlockManager;
import com.omnicraft.skyblock.particles.NativeSpawnMarkerService;
import com.omnicraft.skyblock.particles.SpawnMarkerService;
import com.omnicraft.skyblock.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyblockPlugin extends JavaPlugin {

    private static SkyblockPlugin instance;

    private IslandManager islandManager;
    private BlueprintManager blueprintManager;
    private OneBlockManager oneBlockManager;
    private SoundUtil soundUtil;
    private SpawnMarkerService spawnMarkerService;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        ensureWorld(getConfig().getString("world.standard-name", "skyblock_world"));
        ensureWorld(getConfig().getString("world.oneblock-name", "skyblock_oneblock"));

        this.soundUtil = new SoundUtil(getConfig(), getLogger());

        this.blueprintManager = new BlueprintManager(this);
        blueprintManager.load();

        this.oneBlockManager = new OneBlockManager(this);
        oneBlockManager.load();

        this.islandManager = new IslandManager(this, blueprintManager, oneBlockManager);
        islandManager.loadAll();

        this.spawnMarkerService = new NativeSpawnMarkerService(this, loadSpawnLocation());
        spawnMarkerService.start();

        IslandSelectionGUI selectionGUI = new IslandSelectionGUI(this, blueprintManager);

        // Commands
        SkyblockCommand skyblockCommand = new SkyblockCommand(this, islandManager, selectionGUI, oneBlockManager, soundUtil);
        getCommand("sb").setExecutor(skyblockCommand);
        getCommand("sb").setTabCompleter(skyblockCommand);

        SkyblockAdminCommand adminCommand = new SkyblockAdminCommand(this, islandManager, blueprintManager,
                oneBlockManager, spawnMarkerService);
        getCommand("sba").setExecutor(adminCommand);
        getCommand("sba").setTabCompleter(adminCommand);

        // Listeners
        getServer().getPluginManager().registerEvents(new ProtectionListener(islandManager), this);
        getServer().getPluginManager().registerEvents(new OneBlockListener(this, islandManager, oneBlockManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, islandManager, blueprintManager, soundUtil), this);

        // Periodic autosave
        long intervalTicks = getConfig().getLong("storage.autosave-interval-minutes", 5) * 60L * 20L;
        getServer().getScheduler().runTaskTimer(this, islandManager::saveAll, intervalTicks, intervalTicks);

        if (getServer().getPluginManager().getPlugin("Multiverse-Core") != null) {
            getLogger().info("Multiverse-Core detected. Skyblock worlds are self-managed by this plugin; "
                    + "use Multiverse's own commands for portals/import/per-world settings as usual.");
        }
        if (getServer().getPluginManager().getPlugin("WorldEdit") == null
                && getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") == null) {
            getLogger().warning("Neither WorldEdit nor FastAsyncWorldEdit is installed. "
                    + "STANDARD blueprint schematics cannot be pasted; a plain fallback platform will be used instead.");
        }

        getLogger().info("OmnicraftSkyblock enabled.");
    }

    @Override
    public void onDisable() {
        if (spawnMarkerService != null) {
            spawnMarkerService.stop();
        }
        if (islandManager != null) {
            islandManager.saveAll();
        }
        getLogger().info("OmnicraftSkyblock disabled.");
    }

    /**
     * Creates the world with our own void generator if it isn't loaded yet
     * and world.auto-create is true. If auto-create is false, this just
     * logs and leaves it to the admin (e.g. via Multiverse's /mv create
     * with --generator OmnicraftSkyblock).
     */
    private void ensureWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            getLogger().info("Loaded existing world '" + worldName + "'.");
            return;
        }

        if (!getConfig().getBoolean("world.auto-create", true)) {
            getLogger().warning("World '" + worldName + "' is not loaded and world.auto-create is false. "
                    + "Create it yourself, e.g. with Multiverse: /mv create " + worldName + " normal --generator OmnicraftSkyblock");
            return;
        }

        ChunkGenerator generator = new VoidChunkGenerator();
        WorldCreator creator = new WorldCreator(worldName).generator(generator);
        Bukkit.createWorld(creator);
        getLogger().info("Created world '" + worldName + "'.");
    }

    private Location loadSpawnLocation() {
        String worldName = getConfig().getString("spawn.world", "");
        World world;
        if (worldName == null || worldName.isEmpty()) {
            world = Bukkit.getWorld(getConfig().getString("world.standard-name", "skyblock_world"));
        } else {
            world = Bukkit.getWorld(worldName);
        }

        double x = getConfig().getDouble("spawn.x", 0.5);
        double y = getConfig().getDouble("spawn.y", 65.0);
        double z = getConfig().getDouble("spawn.z", 0.5);
        float yaw = (float) getConfig().getDouble("spawn.yaw", 0.0);
        float pitch = (float) getConfig().getDouble("spawn.pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static SkyblockPlugin getInstance() {
        return instance;
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }

    public BlueprintManager getBlueprintManager() {
        return blueprintManager;
    }

    public OneBlockManager getOneBlockManager() {
        return oneBlockManager;
    }

    public SpawnMarkerService getSpawnMarkerService() {
        return spawnMarkerService;
    }
}
