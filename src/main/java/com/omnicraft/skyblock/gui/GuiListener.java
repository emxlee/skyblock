package com.omnicraft.skyblock.gui;

import com.omnicraft.skyblock.SkyblockPlugin;
import com.omnicraft.skyblock.blueprint.Blueprint;
import com.omnicraft.skyblock.blueprint.BlueprintManager;
import com.omnicraft.skyblock.island.Island;
import com.omnicraft.skyblock.island.IslandManager;
import com.omnicraft.skyblock.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {

    private final SkyblockPlugin plugin;
    private final IslandManager islandManager;
    private final BlueprintManager blueprintManager;
    private final SoundUtil soundUtil;

    public GuiListener(SkyblockPlugin plugin, IslandManager islandManager,
                        BlueprintManager blueprintManager, SoundUtil soundUtil) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.blueprintManager = blueprintManager;
        this.soundUtil = soundUtil;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof IslandSelectionHolder holder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) return;

        String blueprintId = holder.getBlueprintId(event.getSlot());
        if (blueprintId == null) return;

        Blueprint blueprint = blueprintManager.get(blueprintId);
        if (blueprint == null) return;

        boolean allowed = player.hasPermission(blueprint.getPermission())
                || player.hasPermission("skyblock.blueprint.*");

        if (!allowed) {
            soundUtil.deny(player);
            player.sendMessage(Component.text("You don't have permission for that island type.", NamedTextColor.RED));
            return;
        }

        if (islandManager.hasIsland(player.getUniqueId())) {
            soundUtil.deny(player);
            player.sendMessage(Component.text("You already have an island.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        soundUtil.select(player);
        player.closeInventory();

        Island island = islandManager.createIsland(player.getUniqueId(), blueprint);
        if (island == null) {
            player.sendMessage(Component.text("Couldn't create your island - contact an admin.", NamedTextColor.RED));
            return;
        }

        player.teleport(island.getCenter().clone().add(0, 1, 0));
        soundUtil.create(player);
        player.sendMessage(Component.text("Your island has been created!", NamedTextColor.GREEN));
    }
}
