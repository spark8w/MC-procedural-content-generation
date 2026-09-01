package ukc.gdmc;

import java.util.ArrayList;
import java.util.List;

public class TerrainLeveller {

    public static void levelArea(MinecraftApi api, int x, int z, int width, int depth, int targetY) throws Exception {
        System.out.println("TerrainLeveller: Levelling terrain to Y=" + targetY + " (fills low ground, cuts high ground)");

        String baseBlock = (targetY <= 63) ? "minecraft:stone" : "minecraft:grass_block";
        List<MinecraftApi.PutBlock> blocksToPlace = new ArrayList<>();

        for (int bx = x; bx <= x + width; bx++) {
            for (int bz = z; bz <= z + depth; bz++) {

                int localBx = bx - CurrentPlot.plot.seedPlot.edgeXwest;
                int localBz = bz - CurrentPlot.plot.seedPlot.edgeZnorth;

                if (localBx < 0 || localBx >= PlotOriginPoint.plotSize ||
                        localBz < 0 || localBz >= PlotOriginPoint.plotSize) continue;

                int surfaceY = CurrentPlot.plot.AltMap[localBx][localBz];

                if (surfaceY >= targetY) {
                    // CUT DOWN: air out everything from targetY up to surfaceY + 3
                    // (+3 headroom catches grass, snow, slabs, or other decoration sitting on top)
                    for (int cutY = targetY; cutY <= surfaceY + 3; cutY++) {
                        blocksToPlace.add(new MinecraftApi.PutBlock(bx, cutY, bz, "minecraft:air"));
                    }
                    // Ensure the surface block directly below targetY is solid
                    blocksToPlace.add(new MinecraftApi.PutBlock(bx, targetY - 1, bz, baseBlock));
                    // Update AltMap so A* and placement logic see the levelled surface
                    CurrentPlot.plot.AltMap[localBx][localBz] = targetY - 1;
                    continue;
                }

                // Deep gap (ravine/cliff): fill the entire column from existing surface up to targetY.
                // Previously only 2 blocks were placed here, leaving ravines open and foundations floating.
                if (targetY - surfaceY > 3) {
                    for (int fillY = surfaceY + 1; fillY <= targetY - 1; fillY++) {
                        // Top surface block uses terrain material; everything below is stone
                        String fillBlock = (fillY == targetY - 1) ? baseBlock : "minecraft:stone";
                        blocksToPlace.add(new MinecraftApi.PutBlock(bx, fillY, bz, fillBlock));
                    }
                    CurrentPlot.plot.AltMap[localBx][localBz] = targetY - 1;
                    continue;
                }

                // Shallow gap: fill downward from targetY-1 to surfaceY
                for (int fillY = targetY - 1; fillY > surfaceY; fillY--) {
                    blocksToPlace.add(new MinecraftApi.PutBlock(bx, fillY, bz, baseBlock));
                }

                CurrentPlot.plot.AltMap[localBx][localBz] = targetY - 1;
            }
        }

        if (!blocksToPlace.isEmpty()) {
            api.setBlocksWorld(blocksToPlace);
            System.out.println("TerrainLeveller: done.");
        }
    }
}