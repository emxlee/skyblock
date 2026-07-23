package com.omnicraft.skyblock.gui;

import com.omnicraft.skyblock.blueprint.Blueprint;
import com.omnicraft.skyblock.blueprint.BlueprintManager;
import com.omnicraft.skyblock.util.GradientUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class IslandSelectionGUI {

    private final JavaPlugin plugin;
    private final BlueprintManager blueprintManager;

    public IslandSelectionGUI(JavaPlugin plugin, BlueprintManager blueprintManager) {
        this.plugin = plugin;
        this.blueprintManager = blueprintManager;
    }

    public void open(Player player) {
        int rows = Math.max(1, Math.min(6, plugin.getConfig().getInt("gui.rows", 3)));
        String titleRaw = plugin.getConfig().getString("gui.title", "Choose Your Island");
        Component title = GradientUtil.parse(titleRaw);

        IslandSelectionHolder holder = new IslandSelectionHolder();
        Inventory inventory = plugin.getServer().createInventory(holder, rows * 9, title);
        holder.setInventory(inventory);

        for (Blueprint blueprint : blueprintManager.getAll().values()) {
            boolean allowed = player.hasPermission(blueprint.getPermission())
                    || player.hasPermission("skyblock.blueprint.*");

            int slot = blueprint.getSlot();
            if (slot < 0 || slot >= inventory.getSize()) {
                slot = inventory.first(Material.AIR);
                if (slot == -1) continue;
            }

            ItemStack item = new ItemStack(allowed ? blueprint.getIcon() : Material.BARRIER);
            ItemMeta meta = item.getItemMeta();

            Component name = GradientUtil.gradient(blueprint.getDisplayName(),
                    blueprint.getGradientStart(), blueprint.getGradientEnd())
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(name);

            List<Component> lore = new ArrayList<>();
            for (String line : blueprint.getLore()) {
                lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            if (!allowed) {
                lore.add(Component.empty());
                lore.add(Component.text("You don't have permission for this island.", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);

            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            holder.put(slot, blueprint.getId());
        }

        player.openInventory(inventory);
    }
}
