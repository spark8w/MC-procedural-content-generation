package ukc.gdmc;

public class ExclusionManager {
    public boolean[][] exclusionGrid = new boolean[PlotOriginPoint.plotSize][PlotOriginPoint.plotSize];
    public boolean[][] ObstacleMap = new boolean[PlotOriginPoint.plotSize][PlotOriginPoint.plotSize];


    /**
     * Initializes the grid using environmental data (Water/Cliffs).
     */
    public void createEnvironmentalExclusions(DeepScannedStats data) {
        for (int x = PlotOriginPoint.plotStart; x < PlotOriginPoint.plotSize; x++) {
            for (int z = PlotOriginPoint.plotStart; z < PlotOriginPoint.plotSize; z++) {
                // Mark water and moderate slopes as excluded
                if (data.SeaMap[x][z] || data.SlopeMap[x][z] > 2) {
                    exclusionGrid[x][z] = true;
                }
                // Mark ravine-adjacent cells — a sudden drop of 6+ blocks between
                // neighbours indicates a ravine edge. Builds here would need a
                // foundation far too deep to fill safely, so we exclude them entirely.
                if (data.SlopeMap[x][z] > 6) {
                    // Buffer the exclusion 2 cells outward so building footprints
                    // can't straddle the ravine lip even with a partial overlap.
                    for (int bx = x - 2; bx <= x + 2; bx++) {
                        for (int bz = z - 2; bz <= z + 2; bz++) {
                            if (bx >= PlotOriginPoint.plotStart && bx < PlotOriginPoint.plotSize
                                    && bz >= PlotOriginPoint.plotStart && bz < PlotOriginPoint.plotSize) {
                                exclusionGrid[bx][bz] = true;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Marks a rectangle as excluded after a building is placed.
     */
    public void markBuildingZone(int localX, int localZ, int width, int depth) {
        int buffer = 4;
        for (int x = localX - buffer; x < localX + width + buffer; x++) {
            for (int z = localZ - buffer; z < localZ + depth + buffer; z++) {
                if (x >= PlotOriginPoint.plotStart && x < PlotOriginPoint.plotSize && z >= PlotOriginPoint.plotStart && z < PlotOriginPoint.plotSize) {
                    exclusionGrid[x][z] = true;
                }
            }
        }
    }
    public void markObstacleMap(int localX, int localZ, int width, int depth) {
        for (int x = localX; x < localX + width; x++) {
            for (int z = localZ; z < localZ + depth; z++) {
                if (x >= PlotOriginPoint.plotStart && x < PlotOriginPoint.plotSize && z >= PlotOriginPoint.plotStart && z < PlotOriginPoint.plotSize) {
                    ObstacleMap[x][z] = true;
                }
            }
        }
    }

    //Marks the obstacle map using the actual placed-block footprint instead of the rectangular bounding box
    public void markBuildingFootprint(java.util.List<StructureLoader.BlockData> blocks,
                                      int localX, int localZ,
                                      int rotation,
                                      int rawWidth, int rawLength) {
        for (StructureLoader.BlockData block : blocks) {
            // Air blocks are not a physical obstacle
            if (block.type.contains("air")) continue;

            // Rotate block.x / block.z using pre-rotation dimensions as pivots.
            int effectiveWidth  = (rotation == 90 || rotation == 270) ? rawLength : rawWidth;
            int effectiveLength = (rotation == 90 || rotation == 270) ? rawWidth  : rawLength;

            int rotX, rotZ;
            switch (rotation) {
                case 90:
                    rotX = (effectiveLength - 1) - block.z;
                    rotZ = block.x;
                    break;
                case 180:
                    rotX = (effectiveWidth  - 1) - block.x;
                    rotZ = (effectiveLength - 1) - block.z;
                    break;
                case 270:
                    rotX = block.z;
                    rotZ = (effectiveWidth - 1) - block.x;
                    break;
                default:
                    rotX = block.x;
                    rotZ = block.z;
                    break;
            }

            int mapX = localX + rotX;
            int mapZ = localZ + rotZ;

            if (mapX >= PlotOriginPoint.plotStart && mapX < PlotOriginPoint.plotSize
                    && mapZ >= PlotOriginPoint.plotStart && mapZ < PlotOriginPoint.plotSize) {
                ObstacleMap[mapX][mapZ] = true;
            }
        }
    }

    public void markRoadTile(int localX, int localZ) {
        if (localX >= PlotOriginPoint.plotStart && localX < PlotOriginPoint.plotSize
                && localZ >= PlotOriginPoint.plotStart && localZ < PlotOriginPoint.plotSize) {
            exclusionGrid[localX][localZ] = true;
        }
    }

    public boolean isAreaClear(int localX, int localZ, int width, int depth) {
        for (int x = localX; x < localX + width; x++) {
            for (int z = localZ; z < localZ + depth; z++) {
                if (x < PlotOriginPoint.plotStart || x >= PlotOriginPoint.plotSize
                        || z < PlotOriginPoint.plotStart || z >= PlotOriginPoint.plotSize
                        || exclusionGrid[x][z]) {
                    return false; // Area is blocked
                }
            }
        }
        return true; // Area is safe for building
    }

    public void markTreeZone(int localX, int localZ, int width, int depth) {
        int buffer = 2; // trees only need 1 block gap
        for (int x = localX - buffer; x < localX + width + buffer; x++) {
            for (int z = localZ - buffer; z < localZ + depth + buffer; z++) {
                if (x >= PlotOriginPoint.plotStart && x < PlotOriginPoint.plotSize
                        && z >= PlotOriginPoint.plotStart && z < PlotOriginPoint.plotSize) {
                    exclusionGrid[x][z] = true;
                }
            }
        }
    }
}