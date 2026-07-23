package com.omnicraft.skyblock.oneblock;

import com.omnicraft.skyblock.SkyblockPlugin;
import com.omnicraft.skyblock.island.Island;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class OneBlockManager {

    private final SkyblockPlugin plugin;
    private final Random random = new Random();
    private final List<Phase> phases = new ArrayList<>();
    private int blocksPerPhase;

    public OneBlockManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        phases.clear();
        blocksPerPhase = plugin.getConfig().getInt("oneblock.blocks-per-phase", 50);

        List<Map<?, ?>> rawPhases = plugin.getConfig().getMapList("oneblock.phases");

        for (Map<?, ?> map : rawPhases) {
            Object nameObj = map.get("name");
            String name = nameObj != null ? String.valueOf(nameObj) : "Phase";
            Object blocksObj = map.get("blocks");
            if (!(blocksObj instanceof List<?> blockNames)) continue;

            List<Material> blocks = new ArrayList<>();
            for (Object matNameObj : blockNames) {
                Material material = Material.matchMaterial(String.valueOf(matNameObj));
                if (material != null) {
                    blocks.add(material);
                } else {
                    plugin.getLogger().warning("Unknown material '" + matNameObj + "' in oneblock phase '" + name + "'");
                }
            }
            if (!blocks.isEmpty()) {
                phases.add(new Phase(name, blocks));
            }
        }

        plugin.getLogger().info("Loaded " + phases.size() + " OneBlock phase(s).");
    }

    /** Picks the next block for this island and advances its phase if the threshold is reached. */
    public Material nextBlock(Island island) {
        if (phases.isEmpty()) return Material.STONE;

        island.incrementOneBlockBrokenCount();
        int phaseIndex = Math.min(island.getOneBlockBrokenCount() / Math.max(1, blocksPerPhase), phases.size() - 1);
        island.setOneBlockPhaseIndex(phaseIndex);

        Phase phase = phases.get(phaseIndex);
        List<Material> blocks = phase.blocks();
        return blocks.get(random.nextInt(blocks.size()));
    }

    public Material firstBlock() {
        if (phases.isEmpty()) return Material.GRASS_BLOCK;
        return phases.get(0).blocks().get(0);
    }

    public String currentPhaseName(Island island) {
        if (phases.isEmpty()) return "Overworld";
        int index = Math.min(island.getOneBlockPhaseIndex(), phases.size() - 1);
        return phases.get(index).name();
    }

    private record Phase(String name, List<Material> blocks) {
    }
}
