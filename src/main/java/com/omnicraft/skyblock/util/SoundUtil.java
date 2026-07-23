package com.omnicraft.skyblock.util;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Plays configured sound effects for GUI interactions. Falls back silently
 * (logs once) if a configured sound name doesn't match a Sound enum entry,
 * so a typo in config.yml never throws at click-time.
 */
public class SoundUtil {

    private final FileConfiguration config;
    private final Logger logger;

    public SoundUtil(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void play(Player player, String key) {
        String soundName = config.getString("sounds." + key);
        if (soundName == null) return;

        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid sound '" + soundName + "' configured for sounds." + key);
            return;
        }

        float volume = (float) config.getDouble("sounds.volume", 1.0);
        float pitch = (float) config.getDouble("sounds.pitch", 1.0);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public void click(Player player) {
        play(player, "click");
    }

    public void select(Player player) {
        play(player, "select");
    }

    public void deny(Player player) {
        play(player, "deny");
    }

    public void create(Player player) {
        play(player, "create");
    }
}
