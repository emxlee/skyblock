package com.omnicraft.skyblock.listeners;

import com.omnicraft.skyblock.island.Island;
import com.omnicraft.skyblock.island.IslandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Entity;

/**
 * Prevents players from breaking/placing blocks or hurting entities
 * inside an island they are not a member of. Locations that fall
 * outside every registered island (open ocean of void) are left
 * unrestricted so players can still build in unclaimed space if desired.
 */
public class ProtectionListener implements Listener {

    private final IslandManager islandManager;

    public ProtectionListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        Entity victim = event.getEntity();
        Island island = islandManager.getIslandAt(victim.getLocation());
        if (island == null) return; // unclaimed space, no restriction

        if (!island.isMember(player.getUniqueId())) {
            event.setCancelled(true);
            deny(player);
        }
    }

    private boolean canModify(Player player, Location location) {
        Island island = islandManager.getIslandAt(location);
        if (island == null) {
            return true; // not inside any claimed island
        }
        return island.isMember(player.getUniqueId());
    }

    private void deny(Player player) {
        player.sendActionBar(Component.text("You don't have permission to build here.", NamedTextColor.RED));
    }
}
