package com.omnicraft.skyblock.oneblock;

import com.omnicraft.skyblock.SkyblockPlugin;
import com.omnicraft.skyblock.island.Island;
import com.omnicraft.skyblock.island.IslandManager;
import com.omnicraft.skyblock.island.IslandType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class OneBlockListener implements Listener {

    private final SkyblockPlugin plugin;
    private final IslandManager islandManager;
    private final OneBlockManager oneBlockManager;

    public OneBlockListener(SkyblockPlugin plugin, IslandManager islandManager, OneBlockManager oneBlockManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.oneBlockManager = oneBlockManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        Island island = islandManager.getIslandAt(loc);
        if (island == null || island.getType() != IslandType.ONEBLOCK) return;

        Location oneBlockLoc = island.getOneBlockLocation();
        if (!sameBlock(loc, oneBlockLoc)) return;

        Material next = oneBlockManager.nextBlock(island);
        String previousPhase = oneBlockManager.currentPhaseName(island);

        // Regenerate one tick later so the natural break/drop happens first.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            event.getBlock().setType(next);
            String newPhase = oneBlockManager.currentPhaseName(island);
            if (!newPhase.equals(previousPhase)) {
                event.getPlayer().sendMessage(Component.text("Entering phase: " + newPhase, NamedTextColor.AQUA));
            }
        });
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getWorld() != null && a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
