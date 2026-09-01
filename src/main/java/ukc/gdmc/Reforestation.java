package ukc.gdmc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Reforestation Phase — runs after house placement AND road generation.
//   1. Tree placement  — places custom NBT tree schematics in free space
//   2. Foliage restore — replays the pre-deforestation snapshot (via FoliageSnapshot) on cells that are still
//   unoccupied after all construction

public class Reforestation {

    // How many trees to successfully plant
    private static final int TARGET_TREE_COUNT = 30;

    // Safety ceiling to avoid an infinite loop on a dense plot
    private static final int MAX_ATTEMPTS = TARGET_TREE_COUNT * 6;

    // Trees tolerate more slope than buildings — they look fine on slight inclines
    private static final int MAX_ALT_VARIANCE = 4;

    // Pool of custom tree NBT schematics, repeat entries to increase spawn probability.
    private static final String[] TREE_NBT_PATHS = {
            "src/main/resources/structures/trees/largetree2.nbt",
            "src/main/resources/structures/trees/largetree1.nbt",
            "src/main/resources/structures/trees/smalltree1.nbt",
            "src/main/resources/structures/trees/smalltree2.nbt",
            "src/main/resources/structures/trees/smalltree1.nbt",
            "src/main/resources/structures/trees/smalltree2.nbt",
            "src/main/resources/structures/trees/medtree.nbt", // doubled = more common
    };

    public static void RunReforestationPhase() throws Exception {
        if (CurrentPlot.plot == null) {
            System.out.println("Reforestation skipped — no plot data available.");
            return;
        }

        MinecraftApi api             = new MinecraftApi(MinecraftApi.SERVER_URL);
        PlacementInstructions placer = new PlacementInstructions(api);
        Random rng                   = new Random();

        int plantedCount = 0;
        int attempts     = 0;

        System.out.println("\n=== Reforestation Phase ===");
        System.out.println("Target: " + TARGET_TREE_COUNT + " trees");

        while (plantedCount < TARGET_TREE_COUNT && attempts < MAX_ATTEMPTS) {
            attempts++;

            String treePath = TREE_NBT_PATHS[rng.nextInt(TREE_NBT_PATHS.length)];

            StructureLoader.LoadedStructure treeStructure;
            try {
                treeStructure = StructureLoader.loadStructure(treePath);
            } catch (Exception e) {
                System.out.println("Could not load tree schematic: " + treePath + " — skipping.");
                continue;
            }

            int treeW = treeStructure.width;
            int treeD = treeStructure.length;

            List<int[]> candidates = findCandidates(treeW, treeD);

            if (candidates.isEmpty()) {
                System.out.println("[Attempt " + attempts + "] No valid spot for " + treePath + " — skipping.");
                continue;
            }

            int[] chosen = candidates.get(rng.nextInt(candidates.size()));
            int localX   = chosen[0];
            int localZ   = chosen[1];
            int rotation = rng.nextInt(4) * 90;

            placer.placeTree(treePath, localX, localZ, rotation);

            // Reserve the footprint (+2 buffer) so canopies don't merge
            CurrentPlot.exclusions.markTreeZone(localX, localZ, treeW, treeD);

            plantedCount++;
            System.out.println("[" + plantedCount + "/" + TARGET_TREE_COUNT + "] Tree planted at local ("
                    + localX + ", " + localZ + ") rotation=" + rotation);
        }

        if (plantedCount < TARGET_TREE_COUNT) {
            System.out.println("Plot space exhausted after " + attempts
                    + " attempts — planted " + plantedCount + " trees.");
        } else {
            System.out.println("Reforestation complete: " + plantedCount + " trees planted.");
        }

        // Restore the original ground foliage that deforestation removed,
        // only on cells that are still free after all construction is done.
        FoliageData.restore(new MinecraftApi(MinecraftApi.SERVER_URL));
    }


    private static List<int[]> findCandidates(int treeW, int treeD) {
        List<int[]> candidates = new ArrayList<>();

        int maxOriginX = PlotOriginPoint.plotSize - 1 - treeW;
        int maxOriginZ = PlotOriginPoint.plotSize - 1 - treeD;

        for (int ox = 1; ox <= maxOriginX; ox++) {
            for (int oz = 1; oz <= maxOriginZ; oz++) {
                if (CurrentPlot.exclusions.isAreaClear(ox, oz, treeW, treeD)
                        && altVarianceOk(ox, oz, treeW, treeD)) {
                    candidates.add(new int[]{ox, oz});
                }
            }
        }
        return candidates;
    }

    private static boolean altVarianceOk(int ox, int oz, int treeW, int treeD) {
        int minAlt = Integer.MAX_VALUE;
        int maxAlt = Integer.MIN_VALUE;

        for (int dx = 0; dx < treeW; dx++) {
            for (int dz = 0; dz < treeD; dz++) {
                int alt = CurrentPlot.plot.AltMap[ox + dx][oz + dz];
                if (alt < minAlt) minAlt = alt;
                if (alt > maxAlt) maxAlt = alt;
            }
        }
        return (maxAlt - minAlt) <= MAX_ALT_VARIANCE;
    }
}