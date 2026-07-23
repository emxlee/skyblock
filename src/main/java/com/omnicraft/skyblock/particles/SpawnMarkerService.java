package com.omnicraft.skyblock.particles;

import org.bukkit.Location;

/**
 * Displays an ambient particle marker at the skyblock spawn point, and lets
 * admins move it. Kept as an interface so a PlayerParticles-backed
 * implementation can be dropped in later without touching call sites --
 * see NativeSpawnMarkerService for why this ships with Bukkit's own
 * particle system for now.
 */
public interface SpawnMarkerService {

    void start();

    void stop();

    void setLocation(Location location);

    Location getLocation();
}
