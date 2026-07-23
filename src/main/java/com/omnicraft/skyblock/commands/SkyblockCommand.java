package com.omnicraft.skyblock.commands;

import com.omnicraft.skyblock.SkyblockPlugin;
import com.omnicraft.skyblock.gui.IslandSelectionGUI;
import com.omnicraft.skyblock.island.Island;
import com.omnicraft.skyblock.island.IslandManager;
import com.omnicraft.skyblock.island.IslandType;
import com.omnicraft.skyblock.oneblock.OneBlockManager;
import com.omnicraft.skyblock.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class SkyblockCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "home", "delete", "trust", "untrust", "settings"
    );

    private final SkyblockPlugin plugin;
    private final IslandManager islandManager;
    private final IslandSelectionGUI selectionGUI;
    private final OneBlockManager oneBlockManager;
    private final SoundUtil soundUtil;

    public SkyblockCommand(SkyblockPlugin plugin, IslandManager islandManager, IslandSelectionGUI selectionGUI,
                            OneBlockManager oneBlockManager, SoundUtil soundUtil) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.selectionGUI = selectionGUI;
        this.oneBlockManager = oneBlockManager;
        this.soundUtil = soundUtil;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("skyblock.use")) {
            player.sendMessage(Component.text("You don't have permission to use this.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            if (islandManager.hasIsland(player.getUniqueId())) {
                sendHome(player);
            } else {
                selectionGUI.open(player);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player);
            case "home" -> sendHome(player);
            case "delete" -> handleDelete(player, args);
            case "trust" -> handleTrust(player, args, true);
            case "untrust" -> handleTrust(player, args, false);
            case "settings" -> handleSettings(player);
            default -> player.sendMessage(Component.text("Usage: /sb <create|home|delete|trust|untrust|settings>", NamedTextColor.GRAY));
        }
        return true;
    }

    private void handleCreate(Player player) {
        if (islandManager.hasIsland(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have an island. Use /sb home.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("skyblock.island.create")) {
            player.sendMessage(Component.text("You don't have permission to create an island.", NamedTextColor.RED));
            return;
        }
        selectionGUI.open(player);
    }

    private void sendHome(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("You don't have an island yet. Use /sb create.", NamedTextColor.RED));
            return;
        }
        Location home = island.getCenter().clone().add(0, 1, 0);
        player.teleport(home);
        soundUtil.select(player);
        player.sendMessage(Component.text("Welcome home!", NamedTextColor.AQUA));
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage(Component.text("This will permanently delete your island. Run /sb delete confirm to proceed.", NamedTextColor.YELLOW));
            return;
        }
        if (!islandManager.hasIsland(player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have an island.", NamedTextColor.RED));
            return;
        }
        islandManager.deleteIsland(player.getUniqueId());
        player.sendMessage(Component.text("Your island has been deleted.", NamedTextColor.RED));
    }

    private void handleTrust(Player player, String[] args, boolean trust) {
        if (!player.hasPermission("skyblock.island.trust")) {
            player.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("You don't have an island.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /sb " + (trust ? "trust" : "untrust") + " <player>", NamedTextColor.GRAY));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID targetId = target.getUniqueId();

        if (trust) {
            island.getTrusted().add(targetId);
            player.sendMessage(Component.text(args[1] + " can now build on your island.", NamedTextColor.GREEN));
        } else {
            island.getTrusted().remove(targetId);
            player.sendMessage(Component.text(args[1] + " can no longer build on your island.", NamedTextColor.YELLOW));
        }
    }

    private void handleSettings(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage(Component.text("You don't have an island yet. Use /sb create.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("--- Your Island ---", NamedTextColor.AQUA));
        player.sendMessage(Component.text("Type: " + island.getType(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Blueprint: " + island.getBlueprintId(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Size: " + island.getSize() + " blocks", NamedTextColor.GRAY));
        player.sendMessage(Component.text("Trusted players: " + island.getTrusted().size(), NamedTextColor.GRAY));
        if (island.getType() == IslandType.ONEBLOCK) {
            player.sendMessage(Component.text("OneBlock phase: " + oneBlockManager.currentPhaseName(island)
                    + " (" + island.getOneBlockBrokenCount() + " blocks broken)", NamedTextColor.GRAY));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS;
        }
        return List.of();
    }
}
