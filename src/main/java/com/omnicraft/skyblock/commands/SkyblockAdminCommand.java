package com.omnicraft.skyblock.commands;

import com.omnicraft.skyblock.SkyblockPlugin;
import com.omnicraft.skyblock.blueprint.Blueprint;
import com.omnicraft.skyblock.blueprint.BlueprintManager;
import com.omnicraft.skyblock.island.Island;
import com.omnicraft.skyblock.island.IslandManager;
import com.omnicraft.skyblock.oneblock.OneBlockManager;
import com.omnicraft.skyblock.particles.SpawnMarkerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class SkyblockAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "blueprint", "spawn", "tp", "delete");

    private final SkyblockPlugin plugin;
    private final IslandManager islandManager;
    private final BlueprintManager blueprintManager;
    private final OneBlockManager oneBlockManager;
    private final SpawnMarkerService spawnMarkerService;

    public SkyblockAdminCommand(SkyblockPlugin plugin, IslandManager islandManager, BlueprintManager blueprintManager,
                                 OneBlockManager oneBlockManager, SpawnMarkerService spawnMarkerService) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.blueprintManager = blueprintManager;
        this.oneBlockManager = oneBlockManager;
        this.spawnMarkerService = spawnMarkerService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skyblock.admin")) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /sba <reload|blueprint|spawn|tp|delete>", NamedTextColor.GRAY));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "blueprint" -> handleBlueprint(sender, args);
            case "spawn" -> handleSpawn(sender, args);
            case "tp" -> handleTeleport(sender, args);
            case "delete" -> handleDelete(sender, args);
            default -> sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("skyblock.admin.reload")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        plugin.reloadConfig();
        blueprintManager.load();
        oneBlockManager.load();
        sender.sendMessage(Component.text("Config, blueprints, and OneBlock phases reloaded.", NamedTextColor.GREEN));
    }

    private void handleBlueprint(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skyblock.admin.blueprint")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /sba blueprint <list|capture|remove>", NamedTextColor.GRAY));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list" -> {
                sender.sendMessage(Component.text("--- Blueprints (" + blueprintManager.getAll().size() + ") ---", NamedTextColor.AQUA));
                for (Blueprint bp : blueprintManager.getAll().values()) {
                    sender.sendMessage(Component.text(bp.getId() + " - " + bp.getDisplayName()
                            + " [" + bp.getType() + "]", NamedTextColor.GRAY));
                }
            }
            case "capture" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can capture a WorldEdit selection.", NamedTextColor.RED));
                    return;
                }
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /sba blueprint capture <id> <icon-material> <display name...>", NamedTextColor.GRAY));
                    return;
                }
                String id = args[2];
                Material icon = Material.matchMaterial(args[3]);
                if (icon == null) icon = Material.GRASS_BLOCK;
                String displayName = args.length > 4 ? String.join(" ", List.of(args).subList(4, args.length)) : id;

                Blueprint result = blueprintManager.captureFromSelection(player, id, displayName, icon);
                if (result != null) {
                    sender.sendMessage(Component.text("Captured blueprint '" + id + "' from your WorldEdit selection.", NamedTextColor.GREEN));
                }
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /sba blueprint remove <id>", NamedTextColor.GRAY));
                    return;
                }
                boolean removed = blueprintManager.getAll().remove(args[2]) != null;
                sender.sendMessage(removed
                        ? Component.text("Removed blueprint (in memory - edit blueprints.yml to persist removal).", NamedTextColor.YELLOW)
                        : Component.text("No blueprint with that id.", NamedTextColor.RED));
            }
            default -> sender.sendMessage(Component.text("Usage: /sba blueprint <list|capture|remove>", NamedTextColor.GRAY));
        }
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skyblock.admin.spawn")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can set the spawn point.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2 || args[1].equalsIgnoreCase("set")) {
            Location loc = player.getLocation();
            plugin.getConfig().set("spawn.world", loc.getWorld().getName());
            plugin.getConfig().set("spawn.x", loc.getX());
            plugin.getConfig().set("spawn.y", loc.getY());
            plugin.getConfig().set("spawn.z", loc.getZ());
            plugin.getConfig().set("spawn.yaw", loc.getYaw());
            plugin.getConfig().set("spawn.pitch", loc.getPitch());
            plugin.saveConfig();

            spawnMarkerService.setLocation(loc);
            sender.sendMessage(Component.text("Skyblock spawn set to your current location.", NamedTextColor.GREEN));
            return;
        }

        if (args[1].equalsIgnoreCase("particle")) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /sba spawn particle <on|off>", NamedTextColor.GRAY));
                return;
            }
            boolean on = args[2].equalsIgnoreCase("on");
            plugin.getConfig().set("spawn.particle.enabled", on);
            plugin.saveConfig();
            if (on) {
                spawnMarkerService.start();
            } else {
                spawnMarkerService.stop();
            }
            sender.sendMessage(Component.text("Spawn particle marker " + (on ? "enabled" : "disabled") + ".", NamedTextColor.GREEN));
        }
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skyblock.admin.teleport")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(Component.text("Only players can teleport.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /sba tp <player>", NamedTextColor.GRAY));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Island island = islandManager.getIsland(target.getUniqueId());
        if (island == null) {
            sender.sendMessage(Component.text(args[1] + " doesn't have an island.", NamedTextColor.RED));
            return;
        }
        admin.teleport(island.getCenter().clone().add(0, 1, 0));
        sender.sendMessage(Component.text("Teleported to " + args[1] + "'s island.", NamedTextColor.GREEN));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skyblock.admin.delete")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            sender.sendMessage(Component.text("Usage: /sba delete <player> confirm", NamedTextColor.GRAY));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID targetId = target.getUniqueId();
        if (islandManager.getIsland(targetId) == null) {
            sender.sendMessage(Component.text(args[1] + " doesn't have an island.", NamedTextColor.RED));
            return;
        }
        islandManager.deleteIsland(targetId);
        sender.sendMessage(Component.text("Deleted " + args[1] + "'s island.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("blueprint")) {
            return List.of("list", "capture", "remove");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return List.of("set", "particle");
        }
        return List.of();
    }
}
