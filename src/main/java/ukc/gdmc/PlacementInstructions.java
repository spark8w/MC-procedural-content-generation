package ukc.gdmc;

import java.util.ArrayList;
import java.util.List;

public class PlacementInstructions {

    private final MinecraftApi api;
    public static int [] CurrentRootNode = null;
    public static int [] CurrentAnchorNode = null;

    public PlacementInstructions(MinecraftApi api) {
        this.api = api;
    }

    /*
     * Takes an NBT structure file and places it in the Minecraft world
     * at the given world coordinates.
     *
     * Instead of manually passing in coordinates, we use DeepScannedStats
     * to automatically work out where to place the structure:
     *   - X and Z come from the plot edges in SeedOriginPoint
     *   - Y comes from AltMap which holds the real terrain height at every position
     *
     * localOffsetX and localOffsetZ are how far from the NW corner
     * of the plot the structures will be placed.
     * (5, 5) = 5 blocks in from the northwest corner, is an example.
     *
     *
     * The sponge block inside the NBT acts as our anchor point.
     *
     */
    public void placeStructure(String structurePath,
                               int localOffsetX, int localOffsetZ,
                               int rotation) throws Exception {

        // Load and parse the NBT file into a list of blocks we can work with
        StructureLoader.LoadedStructure structure = StructureLoader.loadStructure(structurePath);

        // when rotated 90 or 270, width and length swap
        int effectiveWidth  = (rotation == 90 || rotation == 270) ? structure.length : structure.width;
        int effectiveLength = (rotation == 90 || rotation == 270) ? structure.width  : structure.length;

        int originX = CurrentPlot.plot.seedPlot.edgeXwest + localOffsetX;
        int originZ = CurrentPlot.plot.seedPlot.edgeZnorth + localOffsetZ;

        // Level the ground under the structure before placing it
        int originY = prepareFoundation(originX, originZ, effectiveWidth, effectiveLength) + 1;



        List<MinecraftApi.PutBlock> blocksToPlace = new ArrayList<>();

        for (StructureLoader.BlockData block : structure.blocks) {



            // Step 1 — make coords relative to structure corner (ignore marker for rotation)
            int localX = block.x;
            int localY = block.y;
            int localZ = block.z;

            // Step 2 — rotate around the structure corner
            int rotX, rotZ;
            switch (rotation) {
                case 90:
                    // 90 degrees clockwise
                    rotX = (effectiveLength - 1) - localZ;
                    rotZ = localX;
                    break;
                case 180:
                    // 180 degrees
                    rotX = (effectiveWidth - 1) - localX;
                    rotZ = (effectiveLength - 1) - localZ;
                    break;
                case 270:
                    // 270 degrees clockwise
                    rotX = localZ;
                    rotZ = (effectiveWidth - 1) - localX;
                    break;
                default:
                    // 0 degrees — no rotation
                    rotX = localX;
                    rotZ = localZ;
                    break;
            }

            // Step 2.5 - Find Anchor and Root nodes while calculating for rotations
            if (block.type.contains("waxed_exposed_copper_door")&& block.type.contains("half=lower")) {
                int DirectionX = 0, DirectionZ = 0;
                if (block.type.contains("facing=north")) { DirectionX =  0; DirectionZ = -1; }
                if (block.type.contains("facing=south")) { DirectionX =  0; DirectionZ =  1; }
                if (block.type.contains("facing=east"))  { DirectionX =  1; DirectionZ =  0; }
                if (block.type.contains("facing=west"))  { DirectionX = -1; DirectionZ =  0; }
                int rotDirectionX, rotDirectionZ;
                switch (rotation) {
                    case 90:
                        // 90° clockwise: (dx, dz) -> (-dz, dx)
                        rotDirectionX = -DirectionZ;
                        rotDirectionZ =  DirectionX;
                        break;
                    case 180:
                        // 180°: (dx, dz) -> (-dx, -dz)
                        rotDirectionX = -DirectionX;
                        rotDirectionZ = -DirectionZ;
                        break;
                    case 270:
                        // 270° clockwise: (dx, dz) -> (dz, -dx)
                        rotDirectionX =  DirectionZ;
                        rotDirectionZ = -DirectionX;
                        break;
                    default:
                        rotDirectionX = DirectionX;
                        rotDirectionZ = DirectionZ;
                        break;
                }
                CurrentAnchorNode = new int[]{localOffsetX + rotX +rotDirectionX, localOffsetZ + rotZ + rotDirectionZ};
            }
            if (block.type.contains("deepslate_brick_stairs")) {
                CurrentRootNode = new int[]{localOffsetX + rotX, localOffsetZ + rotZ};
            }

            // Step 3 — rotate block states
            String blockType = rotateBlockState(block.type, rotation);

            // Step 4 — shift to world position (no marker offset needed since we rotate from corner)
            int x = rotX + originX;
            int y = localY + originY;
            int z = rotZ + originZ;

            blocksToPlace.add(new MinecraftApi.PutBlock(x, y, z, blockType));
        }

        System.out.println("Placing " + blocksToPlace.size() + " blocks at rotation " + rotation + "...");
        api.setBlocksWorld(blocksToPlace);

        // Update AltMap so path gen knows these cells are now occupied by a building
        updateAltMap(localOffsetX, localOffsetZ,
                effectiveWidth, effectiveLength);

        System.out.println("Done! Structure placed at " + originX + ", " + originY + ", " + originZ);
    }
    public void placeTree(String structurePath,
                          int localOffsetX, int localOffsetZ,
                          int rotation) throws Exception {

        StructureLoader.LoadedStructure structure = StructureLoader.loadStructure(structurePath);

        // Width and length swap at 90/270
        int effectiveWidth  = (rotation == 90 || rotation == 270) ? structure.length : structure.width;
        int effectiveLength = (rotation == 90 || rotation == 270) ? structure.width  : structure.length;

        int originX = CurrentPlot.plot.seedPlot.edgeXwest  + localOffsetX;
        int originZ = CurrentPlot.plot.seedPlot.edgeZnorth + localOffsetZ;

        // Surface Y at the NW corner of the footprint
        int clampedX = Math.max(PlotOriginPoint.plotStart, Math.min(PlotOriginPoint.plotBorder, localOffsetX));
        int clampedZ = Math.max(PlotOriginPoint.plotStart, Math.min(PlotOriginPoint.plotBorder, localOffsetZ));
        int originY  = CurrentPlot.plot.AltMap[clampedX][clampedZ] + 1;

        List<MinecraftApi.PutBlock> blocksToPlace = new ArrayList<>();

        for (StructureLoader.BlockData block : structure.blocks) {
            int localX = block.x;
            int localY = block.y;
            int localZ = block.z;

            // Rotate position
            int rotX, rotZ;
            switch (rotation) {
                case 90:
                    rotX = (effectiveLength - 1) - localZ;
                    rotZ = localX;
                    break;
                case 180:
                    rotX = (effectiveWidth  - 1) - localX;
                    rotZ = (effectiveLength - 1) - localZ;
                    break;
                case 270:
                    rotX = localZ;
                    rotZ = (effectiveWidth - 1) - localX;
                    break;
                default:
                    rotX = localX;
                    rotZ = localZ;
                    break;
            }

            // Rotate block states
            String blockType = rotateBlockState(block.type, rotation);

            int x = rotX + originX;
            int y = localY + originY;
            int z = rotZ + originZ;

            blocksToPlace.add(new MinecraftApi.PutBlock(x, y, z, blockType));
        }

        System.out.println("Placing tree: " + blocksToPlace.size() + " blocks at rotation " + rotation + "...");
        api.setBlocksWorld(blocksToPlace);
    }

    /*
     * Rotates directional block state properties to match the structure rotation.
     * When a structure rotates 90 degrees clockwise, a block facing north
     * should now face east, east becomes south, south becomes west, west becomes north.
     *
     * This covers stairs, doors, trapdoors, fences, banners and any other
     * block that uses a facing property.
     */
    private String rotateBlockState(String blockType, int rotation) {
        if (rotation == 0) return blockType;

        // Only bother if the block actually has a facing property
        if (!blockType.contains("facing=")) return blockType;

        switch (rotation) {
            case 90:
                blockType = blockType.replace("facing=north", "facing=TEMP");
                blockType = blockType.replace("facing=west",  "facing=north");
                blockType = blockType.replace("facing=south", "facing=west");
                blockType = blockType.replace("facing=east",  "facing=south");
                blockType = blockType.replace("facing=TEMP",  "facing=east");
                break;
            case 180:
                blockType = blockType.replace("facing=west",  "facing=TEMP");
                blockType = blockType.replace("facing=east",  "facing=west");
                blockType = blockType.replace("facing=TEMP",  "facing=east");
                blockType = blockType.replace("facing=north", "facing=TEMP");
                blockType = blockType.replace("facing=south", "facing=north");
                blockType = blockType.replace("facing=TEMP",  "facing=south");
                break;
            case 270:
                blockType = blockType.replace("facing=north", "facing=TEMP");
                blockType = blockType.replace("facing=east",  "facing=north");
                blockType = blockType.replace("facing=south", "facing=east");
                blockType = blockType.replace("facing=west",  "facing=south");
                blockType = blockType.replace("facing=TEMP",  "facing=west");
                break;
        }
        return blockType;
    }


    /*
     * Before we place a structure, we need to make sure the ground under it is flat.
     * If the terrain is uneven the building will either float or sink into the ground.
     *
     * The idea is simple:
     * 1. Look at every column of terrain under the structure footprint
     * 2. Find the minimum surface height across the whole footprint
     * 3. Anything above that minimum gets replaced with air (excavation)
     * 4. Anything below that minimum gets filled with stone (foundation)
     *
     * We use the minimum rather than the average so the levelled foundation
     * always matches where the structure will be placed — both use the same
     * lowest point as the target level so nothing floats or sinks.
     *
     * We only touch the footprint area — terrain outside stays natural
     */
    private int prepareFoundation(int originX, int originZ,
                                  int width, int length) throws Exception {
        // Find the most common height across the footprint as the target level
        java.util.HashMap<Integer, Integer> heightFrequency = new java.util.HashMap<>();
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < length; dz++) {
                int mapX = (originX - CurrentPlot.plot.seedPlot.edgeXwest) + dx;
                int mapZ = (originZ - CurrentPlot.plot.seedPlot.edgeZnorth) + dz;

                if (mapX < 0 || mapX >= PlotOriginPoint.plotSize || mapZ < 0 || mapZ >= PlotOriginPoint.plotSize) continue;

                int h = CurrentPlot.plot.AltMap[mapX][mapZ];
                heightFrequency.put(h, heightFrequency.getOrDefault(h, 0) + 1);
            }
        }

        int targetY = 0;
        int maxFrequency = 0;
        for (java.util.Map.Entry<Integer, Integer> entry : heightFrequency.entrySet()) {
            if (entry.getValue() > maxFrequency) {
                maxFrequency = entry.getValue();
                targetY = entry.getKey();
            }
        }


        System.out.println("Levelling footprint to Y=" + targetY);

        List<MinecraftApi.PutBlock> blocksToPlace = new ArrayList<>();

        // Second pass — go through each column and fix it
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < length; dz++) {

                int worldX = originX + dx;
                int worldZ = originZ + dz;

                int localX = worldX - CurrentPlot.plot.seedPlot.edgeXwest;
                int localZ = worldZ - CurrentPlot.plot.seedPlot.edgeZnorth;
                // bounds check — skip if outside the plot grid
                if (localX < 0 || localX >= PlotOriginPoint.plotSize || localZ < 0 || localZ >= PlotOriginPoint.plotSize) continue;

                int surfaceY = CurrentPlot.plot.AltMap[localX][localZ];

                if (surfaceY > targetY) {
                    // Terrain is too high — dig it down by replacing blocks with air
                    // We go from the surface down to just above target
                    for (int y = targetY + 1; y <= surfaceY; y++) {
                        blocksToPlace.add(new MinecraftApi.PutBlock(worldX, y, worldZ, "minecraft:air"));
                    }

                } else if (surfaceY < targetY) {
                    // Terrain is too low — fill it up with stone as a foundation
                    // We go from the surface up to the target level
                    for (int y = surfaceY + 1; y <= targetY; y++) {
                        blocksToPlace.add(new MinecraftApi.PutBlock(worldX, y, worldZ, "minecraft:grass_block"));
                    }
                }
                // if surfaceY == targetY its already perfect, nothing to do
            }
        }

        System.out.println("Levelling " + blocksToPlace.size() + " blocks...");
        api.setBlocksWorld(blocksToPlace);
        System.out.println("Footprint levelled.");

        return targetY;
    }

    /*
     * Places the town hall at the centre of the champion plot.
     * The town hall is always centred on the plot — offset by half
     * its dimensions so it sits symmetrically around (32, 32).
     * It's placed before any houses so exclusion zones can block it off.
     */
    public void placeTownHall(String structurePath) throws Exception {

        StructureLoader.LoadedStructure structure = StructureLoader.loadStructure(structurePath);

        // Centre on the plot
        int localX = (PlotOriginPoint.plotSize / 2) - (structure.width / 2);
        int localZ = (PlotOriginPoint.plotSize / 2) - (structure.length / 2);

        System.out.println("Placing town hall at plot centre — local offset: " + localX + ", " + localZ);

        // Place it using the standard placement pipeline
        placeStructure(structurePath, localX, localZ, 0);
    }

    /*
     * Updates AltMap after structures are placed so path generation
     * has accurate terrain data.
     *
     * We set every cell under the structure footprint to the roof height
     * (originY + structure height) so the path gen treats those cells
     * as elevated and routes around them.
     */
    private void updateAltMap(int localOffsetX, int localOffsetZ,
                              int width, int length) {


        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < length; dz++) {
                int mapX = localOffsetX + dx;
                int mapZ = localOffsetZ + dz;

                // Make sure we stay within the grid
                if (mapX >= 0 && mapX < PlotOriginPoint.plotSize && mapZ >= 0 && mapZ < PlotOriginPoint.plotSize) {
                    CurrentPlot.plot.AltMap[mapX][mapZ] = CurrentPlot.plot.SurfaceMap[mapX][mapZ] + 9;

                }
            }
        }
    }
}