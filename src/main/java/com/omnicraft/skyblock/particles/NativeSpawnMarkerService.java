package com.omnicraft.skyblock.particles;

import com.omnicraft.skyblock.SkyblockPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitTask;

/**
 * Displays a looping particle effect at the configured skyblock spawn point
 * using Bukkit's built-in Particle API. This is what's actually wired up
 * today.
 *
 * Note on PlayerParticles: that plugin's "fixed effects" feature
 * (/pp fixed) covers exactly this use case and would look nicer / be more
 * configurable in-game. I didn't wire it up here because I could not
 * verify the exact method signature for creating a fixed effect
 * programmatically via PlayerParticlesAPI without your server's installed
 * jar on hand, and guessing at a third-party API risks a broken build.
 * If you want that integration, install PlayerParticles, run
 * `/pp fixed create <effect> <style>` at the spawn location in-game as a
 * one-time setup, and let me know your PlayerParticles version -- I can
 * then wire a real PlayerParticlesSpawnMarkerService against its confirmed
 * API and swap it in here with no other code changes needed.
 */
public class NativeSpawnMarkerService implements SpawnMarkerService {

    private final SkyblockPlugin plugin;
    private Location location;
    private BukkitTask task;

    public NativeSpawnMarkerService(SkyblockPlugin plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
    }

    @Override
    public void start() {
        stop();

        boolean enabled = plugin.getConfig().getBoolean("spawn.particle.enabled", true);
        if (!enabled || location == null || location.getWorld() == null) return;

        String particleName = plugin.getConfig().getString("spawn.particle.type", "END_ROD");
        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid spawn.particle.type '" + particleName + "', defaulting to END_ROD.");
            particle = Particle.END_ROD;
        }

        int count = plugin.getConfig().getInt("spawn.particle.count", 6);
        long interval = plugin.getConfig().getLong("spawn.particle.interval-ticks", 20);
        Particle finalParticle = particle;

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (location.getWorld() == null) return;
            location.getWorld().spawnParticle(finalParticle,
                    location.clone().add(0, 1, 0),
                    count, 0.4, 0.6, 0.4, 0.01);
        }, 0L, interval);
    }

    @Override
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
        start(); // restart the loop at the new point
    }

    @Override
    public Location getLocation() {
        return location;
    }
}
