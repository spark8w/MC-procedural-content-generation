package ukc.gdmc;

import java.util.ArrayList;
import java.util.List;

public class FoliageData {

    //Stores one captured foliage block.
    public static class CapturedBlock {
        public final int worldX, worldY, worldZ;
        public final int localX, localZ;
        public final String blockId;

        public CapturedBlock(int worldX, int worldY, int worldZ,
                             int localX, int localZ,
                             String blockId) {
            this.worldX  = worldX;
            this.worldY  = worldY;
            this.worldZ  = worldZ;
            this.localX  = localX;
            this.localZ  = localZ;
            this.blockId = blockId;
        }
    }

    // The snapshot
    private static List<CapturedBlock> snapshot = new ArrayList<>();


    // Scans the entire plot surface before any deforestation occurs and records every foliage block it finds. .

    public static void capture(MinecraftApi api) throws Exception {
        if (CurrentPlot.plot == null) {
            System.out.println("FoliageSnapshot: no plot data — capture skipped.");
            return;
        }

        snapshot.clear();
        PlotOriginPoint seed = CurrentPlot.plot.seedPlot;

        System.out.println("FoliageSnapshot: capturing pre-deforestation foliage...");

        // Since surface Y varies per column we fetch a fixed window from minAlt to maxAlt+4
        // across the whole plot, which is always small (foliage sits just above ground).

        int minAlt = Integer.MAX_VALUE;
        int maxAlt = Integer.MIN_VALUE;
        for (int lx = 0; lx < PlotOriginPoint.plotSize; lx++) {
            for (int lz = 0; lz < PlotOriginPoint.plotSize; lz++) {
                int a = CurrentPlot.plot.SurfaceMap[lx][lz];
                if (a < minAlt) minAlt = a;
                if (a > maxAlt) maxAlt = a;
            }
        }
        // Capture from 1 above minimum surface to 3 above maximum (covers tall grass, etc.)
        int scanYstart = minAlt;       // start at surface level (inclusive) to catch +1 slabs etc.
        int scanHeight = (maxAlt - minAlt) + 3; // enough headroom for all ground foliage

        int captured = 0;

        for (int lx = 0; lx < PlotOriginPoint.plotSize; lx++) {
            int worldX = seed.edgeXwest + lx;

            List<MinecraftApi.Block> column = api.getBlocks(
                    worldX, scanYstart, seed.edgeZnorth,
                    1, scanHeight, PlotOriginPoint.plotSize);

            for (MinecraftApi.Block block : column) {
                if (block.id == null || block.id.contains("air")) continue;

                // Only keep foliage-category blocks
                if (!isFoliage(block.id)) continue;

                // Skip multi-block structures (logs, leaves, bamboo columns, cactus)
                if (isTreeComponent(block.id)) continue;

                int lz = block.z - seed.edgeZnorth;
                if (lz < 0 || lz >= PlotOriginPoint.plotSize) continue;

                // Only capture blocks that sit within 1-3 blocks above the recorded surface.
                // This filters out foliage buried under terrain or floating above canopies.
                int surface = CurrentPlot.plot.SurfaceMap[lx][lz];
                if (block.y < surface || block.y > surface + 3) continue;

                snapshot.add(new CapturedBlock(
                        block.x, block.y, block.z,
                        lx, lz,
                        block.id));
                captured++;
            }
        }

        System.out.println("FoliageSnapshot: captured " + captured + " foliage blocks.");
    }


    //Replays the captured foliage blocks back into the world,
    //skipping any cell that is now occupied according to the global exclusion grid.
    //The Y coordinate is sourced from the post-levelling AltMap rather than the
    //original captured worldY, so foliage always sits on the levelled surface
    //instead of sinking into or floating above filled/bridged terrain.
    public static void restore(MinecraftApi api) throws Exception {
        if (snapshot.isEmpty()) {
            System.out.println("FoliageSnapshot: snapshot is empty — restore skipped.");
            return;
        }

        System.out.println("\n=== Foliage Restoration Phase ===");
        System.out.println("FoliageSnapshot: restoring from " + snapshot.size() + " captured blocks...");

        PlotOriginPoint seed = CurrentPlot.plot.seedPlot;
        List<MinecraftApi.PutBlock> toPlace = new ArrayList<>();

        for (CapturedBlock b : snapshot) {
            // Skip cells claimed by buildings, roads, or tree zones
            if (b.localX < PlotOriginPoint.plotStart || b.localX >= PlotOriginPoint.plotSize
                    || b.localZ < PlotOriginPoint.plotStart || b.localZ >= PlotOriginPoint.plotSize) {
                continue;
            }

            if (CurrentPlot.exclusions.exclusionGrid[b.localX][b.localZ]) continue;

            // Use the post-levelling surface Y from AltMap (+1 to place on top of the surface block).
            // TerrainLeveller already updates AltMap in-place, so this reflects the filled terrain.
            int levelledY = CurrentPlot.plot.AltMap[b.localX][b.localZ] + 1;

            // Recalculate world X/Z from local coords in case the seed origin differs from capture time.
            int worldX = seed.edgeXwest + b.localX;
            int worldZ = seed.edgeZnorth + b.localZ;

            toPlace.add(new MinecraftApi.PutBlock(worldX, levelledY, worldZ, b.blockId));
        }

        if (toPlace.isEmpty()) {
            System.out.println("FoliageSnapshot: no free cells remain for foliage restoration.");
            return;
        }

        System.out.println("FoliageSnapshot: placing " + toPlace.size() + " foliage blocks...");
        api.setBlocksWorld(toPlace);
        System.out.println("Foliage restoration complete.");
    }


    // Returns true if the block ID belongs to the foliage category.
    private static boolean isFoliage(String id) {
        for (String kw : TerrainScanner.FOLIAGE_KEYWORDS) {
            if (id.contains(kw)) return true;
        }
        return false;
    }

    //Returns true for blocks that are part of a tree's structural volume
    //(logs, leaves, roots, bamboo, cactus columns) rather than ground-level decorative foliage
    private static boolean isTreeComponent(String id) {
        return id.contains("log")
                || id.contains("leaves")
                || id.contains("root")
                || id.contains("bamboo")
                || id.contains("cactus")
                || id.contains("vine")
                || id.contains("mushroom_stem")
                || id.contains("mushroom_block");
    }
}