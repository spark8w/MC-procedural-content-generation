package ukc.gdmc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 * Reads the deep-scanned terrain data from CurrentPlot.plot and places
 * multiple houses within the plotSize x plotSize scanned area.
 *
 * Spacing and environmental exclusions are handled by the global
 * CurrentPlot.exclusions grid so every phase (houses, roads, trees) shares
 * the same collision data.
 *
 * Placement is fully delegated to PlacementInstructions.
 */
public class PlanPlacePhase {

    private static final int TARGET_HOUSE_COUNT = 8;
    private static final int MAX_ATTEMPTS = TARGET_HOUSE_COUNT * 11;
    private static final int MAX_ALT_VARIANCE = 1;

    private static final String[] HOUSE_NBT_PATHS = {
            "src/main/resources/structures/bakery.nbt",
            "src/main/resources/structures/butcher.nbt",
            "src/main/resources/structures/smallhouse.nbt",
            "src/main/resources/structures/largehouse.nbt",
    };

    public static void RunHousePlacementPhase() throws Exception {
        int bakeryCount  = 0;
        int butcherCount = 0;
        int MAX_BAKERY   = 1;
        int MAX_BUTCHER  = 1;

        DeepScannedStats data = CurrentPlot.plot;
        MinecraftApi api = new MinecraftApi(MinecraftApi.SERVER_URL);

        // 1. EXCLUSIONS: Seed the shared global grid with water and steep slopes
        CurrentPlot.exclusions.createEnvironmentalExclusions(data);

        // 2. DEFORESTATION: Clear the entire plot BEFORE any placement begins
        clearEntirePlotOfTrees(api, data);

        // 3. PLACEMENT PREP
        PlacementInstructions placer = new PlacementInstructions(api);
        Random rng = new Random();

        int placedCount = 0;
        int attempts    = 0;

        // 4. TOWN HALL: Place at plot centre before any houses
        placer.placeTownHall("src/main/resources/structures/townhall.nbt");
        // Calculate town hall position for exclusion marking
        StructureLoader.LoadedStructure townHallStructure = StructureLoader.loadStructure("src/main/resources/structures/townhall.nbt");
        int townHallLocalX = (PlotOriginPoint.plotSize / 2) - (townHallStructure.width / 2);
        int townHallLocalZ = (PlotOriginPoint.plotSize / 2) - (townHallStructure.length / 2);

        // Exclusion buffer (bounding box + 4-block margin keeps houses away from the hall)
        CurrentPlot.exclusions.markBuildingZone(townHallLocalX, townHallLocalZ, townHallStructure.width, townHallStructure.length);
        System.out.println("Town hall placed at local: " + townHallLocalX + ", " + townHallLocalZ);
        // Obstacle map uses the real block footprint so A* can thread through
        // gaps inside the bounding box (e.g. courtyards, L-shapes)
        CurrentPlot.exclusions.markObstacleMap(
                townHallLocalX, townHallLocalZ,
                townHallStructure.width, townHallStructure.length);


        // Clear terrain mounds banked against the town hall walls
        int targetY = CurrentPlot.plot.AltMap[PlotOriginPoint.plotSize / 2][PlotOriginPoint.plotSize / 2];
        clearTownHallSurroundings(api, townHallLocalX, townHallLocalZ,
                townHallStructure.width, townHallStructure.length, targetY);

        if (PlacementInstructions.CurrentRootNode != null) {
            int[] raw = PlacementInstructions.CurrentRootNode;
            // Push the root node outside the town hall ObstacleMap.
            // Walk outward from the raw marker position until we find a clear cell.
            int[] safeRoot = pushNodeOutsideObstacles(raw, townHallLocalX, townHallLocalZ,
                    townHallStructure.width, townHallStructure.length);
            GreedyPlanner.RootNode = safeRoot;

            // Lock the node coordinate in the exclusion grid
            CurrentPlot.exclusions.exclusionGrid[safeRoot[0]][safeRoot[1]] = true;

            System.out.println("Root Node (adjusted): " + GreedyPlanner.RootNode[0] + "," + GreedyPlanner.RootNode[1]);
        } else {
            System.out.println("Root Node not found");
        }


        // 5. PLACEMENT LOOP
        while (placedCount < TARGET_HOUSE_COUNT && attempts < MAX_ATTEMPTS) {
            attempts++;

            String chosenPath = HOUSE_NBT_PATHS[rng.nextInt(HOUSE_NBT_PATHS.length)];

            if (chosenPath.contains("bakery")  && bakeryCount  >= MAX_BAKERY)  continue;
            if (chosenPath.contains("butcher") && butcherCount >= MAX_BUTCHER) continue;

            System.out.println("\n[Attempt " + attempts + " | Placed " + placedCount
                    + "/" + TARGET_HOUSE_COUNT + "] Schematic: " + chosenPath);

            StructureLoader.LoadedStructure structure = StructureLoader.loadStructure(chosenPath);
            int houseW = structure.width;
            int houseD = structure.length;
            System.out.println("Footprint: " + houseW + " x " + houseD);

            List<int[]> candidates = findCandidates(data, houseW, houseD);

            if (candidates.isEmpty()) {
                System.out.println("No valid position found — retrying with different schematic.");
                continue;
            }

            int[] chosen  = candidates.get(rng.nextInt(candidates.size()));
            int localX    = chosen[0];
            int localZ    = chosen[1];

            int rotation = rng.nextInt(4) * 90;

            // Compute effective dimensions AFTER rotation so exclusion zones match reality
            int effectiveW = (rotation == 90 || rotation == 270) ? houseD : houseW;
            int effectiveD = (rotation == 90 || rotation == 270) ? houseW : houseD;

            placer.placeStructure(chosenPath, localX, localZ, rotation);

            // Obstacle map: use actual block footprint, not bounding box, so A* can
            // route through empty space inside irregular/L-shaped building outlines
            // Pass raw pre-rotation dimensions — markBuildingFootprint pivots in schematic space
            CurrentPlot.exclusions.markBuildingFootprint(
                    structure.blocks, localX, localZ, rotation, structure.width, structure.length);

            // Use effective (post-rotation) dimensions so the buffer zone covers the actual footprint
            CurrentPlot.exclusions.markBuildingZone(localX, localZ, effectiveW, effectiveD);

            if (PlacementInstructions.CurrentAnchorNode != null) {
                int[] raw = PlacementInstructions.CurrentAnchorNode;
                int[] safeAnchor = pushNodeOutsideObstacles(raw, localX, localZ, effectiveW, effectiveD);
                GreedyPlanner.AnchorNodes.add(safeAnchor);

                // Lock the node coordinate in the exclusion grid
                CurrentPlot.exclusions.exclusionGrid[safeAnchor[0]][safeAnchor[1]] = true;

                System.out.println("Anchor Node found (adjusted): " + safeAnchor[0]
                        + "," + safeAnchor[1]);
            } else {
                System.out.println("Anchor Node not found");
            }

            placedCount++;

            if (chosenPath.contains("bakery"))  bakeryCount++;
            if (chosenPath.contains("butcher")) butcherCount++;
        }

        if (placedCount < TARGET_HOUSE_COUNT) {
            System.out.println("\nPlot exhausted after " + attempts + " attempts — could not fit more houses.");
        }
        System.out.println("Placement complete: " + placedCount + " / " + TARGET_HOUSE_COUNT + " houses placed.");

        int tpX = CurrentPlot.plot.seedPlot.edgeXwest;
        int tpZ = CurrentPlot.plot.seedPlot.edgeZnorth;
        int tpY = CurrentPlot.plot.AltMap[PlotOriginPoint.plotStart][PlotOriginPoint.plotStart] + 10;
        api.runCommand("tp @p " + tpX + " " + tpY + " " + tpZ);
        System.out.println("Teleporting to settlement at " + tpX + ", " + tpY + ", " + tpZ);
    }

    private static void clearTownHallSurroundings(MinecraftApi api,
                                                  int townHallLocalX, int townHallLocalZ,
                                                  int townHallWidth, int townHallLength,
                                                  int targetY) throws Exception {
        int hBuffer = 5; // horizontal clearance beyond the footprint
        int vBuffer = 4; // vertical clearance above targetY

        PlotOriginPoint seed = CurrentPlot.plot.seedPlot;

        int startLocalX = townHallLocalX - hBuffer;
        int endLocalX   = townHallLocalX + townHallWidth + hBuffer;
        int startLocalZ = townHallLocalZ - hBuffer;
        int endLocalZ   = townHallLocalZ + townHallLength + hBuffer;

        List<MinecraftApi.PutBlock> toAir = new ArrayList<>();

        for (int lx = startLocalX; lx < endLocalX; lx++) {
            for (int lz = startLocalZ; lz < endLocalZ; lz++) {

                // Skip the actual footprint — only clear the surrounding shell
                boolean insideFootprint = lx >= townHallLocalX && lx < townHallLocalX + townHallWidth
                        && lz >= townHallLocalZ && lz < townHallLocalZ + townHallLength;
                if (insideFootprint) continue;

                // Clamp to plot bounds
                if (lx < PlotOriginPoint.plotStart || lx >= PlotOriginPoint.plotSize) continue;
                if (lz < PlotOriginPoint.plotStart || lz >= PlotOriginPoint.plotSize) continue;

                int worldX = seed.edgeXwest + lx;
                int worldZ = seed.edgeZnorth + lz;

                // Clear from targetY up to targetY + vBuffer (mound sits at and above ground level)
                for (int dy = 0; dy <= vBuffer; dy++) {
                    int worldY = targetY + dy;
                    toAir.add(new MinecraftApi.PutBlock(worldX, worldY, worldZ, "minecraft:air"));
                }
            }
        }

        if (!toAir.isEmpty()) {
            api.setBlocksWorld(toAir);
            System.out.println("Town hall surroundings cleared: " + toAir.size() + " blocks removed.");
        }
    }

    private static void clearEntirePlotOfTrees(MinecraftApi api, DeepScannedStats data) throws Exception {
        System.out.println("Initiating complete plot clearing (deforestation & foliage removal)...");
        PlotOriginPoint seed = data.seedPlot;

        String[] keywordsToClear = TerrainScanner.FOLIAGE_KEYWORDS;

        int startX  = seed.edgeXwest;
        int startZ  = seed.edgeZnorth;
        int dx      = PlotOriginPoint.plotSize;
        int dz      = PlotOriginPoint.plotSize;

        int minAlt = Integer.MAX_VALUE;
        for (int x = PlotOriginPoint.plotStart; x < PlotOriginPoint.plotSize; x++) {
            for (int z = PlotOriginPoint.plotStart; z < PlotOriginPoint.plotSize; z++) {
                if (data.AltMap[x][z] < minAlt) minAlt = data.AltMap[x][z];
            }
        }

        int startY = minAlt - 2;
        int dy     = 64;

        List<MinecraftApi.Block> blocks = api.getBlocks(startX, startY, startZ, dx, dy, dz);
        List<MinecraftApi.PutBlock> blocksToClear = new ArrayList<>();

        for (MinecraftApi.Block b : blocks) {
            if (b.id != null) {
                for (String keyword : keywordsToClear) {
                    if (b.id.contains(keyword)) {
                        blocksToClear.add(new MinecraftApi.PutBlock(b.x, b.y, b.z, "minecraft:air"));
                        break;
                    }
                }
            }
        }

        if (!blocksToClear.isEmpty()) {
            System.out.println("Found " + blocksToClear.size() + " foliage/tree blocks. Clearing...");
            api.setBlocksWorld(blocksToClear);
            System.out.println("Plot clearing complete! Site is ready for construction.");
        } else {
            System.out.println("No removable foliage found in the plot area.");
        }
    }

    // Uses the shared global exclusion grid — water, slopes, buildings, and roads are all tracked there
    private static List<int[]> findCandidates(DeepScannedStats data, int houseW, int houseD) {
        List<int[]> candidates = new ArrayList<>();

        int maxOriginX = PlotOriginPoint.plotSize - 1 - houseW;
        int maxOriginZ = PlotOriginPoint.plotSize - 1 - houseD;

        for (int ox = 2; ox <= maxOriginX; ox++) {
            for (int oz = 2; oz <= maxOriginZ; oz++) {
                if (CurrentPlot.exclusions.isAreaClear(ox, oz, houseW, houseD)
                        && altVarianceOk(data.AltMap, ox, oz, houseW, houseD)) {
                    candidates.add(new int[]{ox, oz});
                }
            }
        }
        return candidates;
    }

    private static boolean altVarianceOk(int[][] altMap, int ox, int oz, int houseW, int houseD) {
        int minAlt = Integer.MAX_VALUE;
        int maxAlt = Integer.MIN_VALUE;

        for (int dx = 0; dx < houseW; dx++) {
            for (int dz = 0; dz < houseD; dz++) {
                int alt = altMap[ox + dx][oz + dz];
                if (alt < minAlt) minAlt = alt;
                if (alt > maxAlt) maxAlt = alt;
            }
        }
        return (maxAlt - minAlt) <= MAX_ALT_VARIANCE;
    }

    /**
     * If a node lands inside the building's obstacle footprint, offset it
     * outward  until it sits on a cell that is both outside ObstacleMap and outside
     * exclusionGrid.
     */
    private static int[] pushNodeOutsideObstacles(int[] node,
                                                  int buildingLocalX, int buildingLocalZ,
                                                  int buildingWidth, int buildingDepth) {
        int cx = buildingLocalX + buildingWidth  / 2;
        int cz = buildingLocalZ + buildingDepth / 2;
        int nx = node[0];
        int nz = node[1];

        // Direction vector away from building centre
        int dx = nx - cx;
        int dz = nz - cz;

        // Normalise to unit cardinal step (favour whichever axis has greater offset)
        int stepX, stepZ;
        if (Math.abs(dx) >= Math.abs(dz)) {
            stepX = dx >= 0 ? 1 : -1;
            stepZ = 0;
        } else {
            stepX = 0;
            stepZ = dz >= 0 ? 1 : -1;
        }

        // Walk until clear, up to the full plot width as safety ceiling
        for (int i = 0; i < PlotOriginPoint.plotSize; i++) {
            boolean inObstacle = nx >= PlotOriginPoint.plotStart
                    && nx < PlotOriginPoint.plotSize
                    && nz >= PlotOriginPoint.plotStart
                    && nz < PlotOriginPoint.plotSize
                    && CurrentPlot.exclusions.ObstacleMap[nx][nz];

            if (!inObstacle) break;

            nx += stepX;
            nz += stepZ;
        }

        // Clamp to plot bounds
        nx = Math.max(PlotOriginPoint.plotStart, Math.min(PlotOriginPoint.plotBorder, nx));
        nz = Math.max(PlotOriginPoint.plotStart, Math.min(PlotOriginPoint.plotBorder, nz));

        System.out.println("Node raw=" + node[0] + "," + node[1]
                + " pushed to=" + nx + "," + nz);

        return new int[]{nx, nz};
    }
}