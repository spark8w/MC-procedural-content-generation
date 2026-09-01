package ukc.gdmc;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class TerrainScanner {
    PlotOriginPoint seedPlot;
    private final MinecraftApi API;

    // Single source of truth for all foliage/vegetation identifiers.
    // Used by quickScan, deepScan, and PlanPlacePhase.clearEntirePlotOfTrees.
    public static final String[] FOLIAGE_KEYWORDS = {
            "leaves", "snow", "log", "vine", "bamboo", "fern", "short_grass",
            "tall_grass", "bush", "root", "sapling", "mushroom", "moss",
            "cactus", "azalea", "sugar_cane", "glow_lichen", "spore_blossom",
            "lily", "flower", "litter", "petal"
    };

    TerrainScanner(PlotOriginPoint seedPlot, MinecraftApi API) {
        this.seedPlot = seedPlot;
        this.API = API;
    }

    public QuickScannedStats quickScan() throws Exception {
        List<Integer> intList = new ArrayList<>();
        // OLD -> quickScan is engineered to iterate by 4 blocks when scanning the 64x64 grid shrinking 4096 into 256 for estimation rather than precision
        // NEW -> quickScane still iterates per 4 blocks but plot size still shrinks to a quarter of its size when scanned.
        // Its now using plotSize * plotSize as it allows us to scale the size of the plot.
        for (int x = seedPlot.edgeXwest; x < seedPlot.edgeXeast; x += 4) {
            List<MinecraftApi.Block> row = API.getBlocks(x, -64, seedPlot.edgeZnorth, 1, 384, PlotOriginPoint.plotSize);
            java.util.Map<Integer, Integer> zToSurfaceY = new java.util.HashMap<>();

            for (MinecraftApi.Block block : row) {
                boolean isFoliage = false;
                for (String kw : FOLIAGE_KEYWORDS) {
                    if (block.id.contains(kw)) { isFoliage = true; break; }
                }
                if (block.id.contains("air") || isFoliage) continue;

                int currentY = zToSurfaceY.getOrDefault(block.z, -64);
                if (block.y > currentY) {
                    zToSurfaceY.put(block.z, block.y);
                }
            }

            // Sample every 4th z value
            for (int z = seedPlot.edgeZnorth; z < seedPlot.edgeZsouth; z += 4) {
                intList.add(zToSurfaceY.getOrDefault(z, -64));
            }
        }
        //calculating the overall stats of plots
        System.out.println(intList);
        int maxYval = Collections.max(intList);
        int minYval = Collections.min(intList);
        int Gradient = maxYval - minYval;
        int SeaCount = 0;
        for (int yValue : intList) {
            if (yValue <= 62) { //62 is sea level so we assume y blocks with 62 value is either sea or sea adjacent
                SeaCount++;
            }
        }
        return new QuickScannedStats(Gradient, SeaCount, seedPlot);
    }

    public DeepScannedStats deepScan() throws Exception {
        // OUTDATED -> created 3 64x64 grids for my three maps that scan all 4096 blocks each

        // New -> Creates 3 grids based of squaring the plotSize value.
        int[][] AltMap = new int[PlotOriginPoint.plotSize][PlotOriginPoint.plotSize];
        boolean[][] SeaMap = new boolean[PlotOriginPoint.plotSize][PlotOriginPoint.plotSize];
        int[][] SlopeMap = new int[PlotOriginPoint.plotSize][PlotOriginPoint.plotSize];
        int[][] SurfaceMap = new int[PlotOriginPoint.plotSize][PlotOriginPoint.plotSize];
        String[][] AltBlockMap = new String[PlotOriginPoint.plotSize][PlotOriginPoint.plotSize];

        //full scan plotSize * plotSize normal data collection, the plot is not shrunk
        for (int x = seedPlot.edgeXwest; x < seedPlot.edgeXeast; x++) {
            List<MinecraftApi.Block> row = API.getBlocks(x, -64, seedPlot.edgeZnorth, 1, 384, PlotOriginPoint.plotSize);
            for (MinecraftApi.Block block : row) {
                if (block.id.contains("air")) continue;
                boolean isFoliage = false;
                for (String kw : FOLIAGE_KEYWORDS) {
                    if (block.id.contains(kw)) { isFoliage = true; break; }
                }
                if (isFoliage) continue;
                int Localx = x - seedPlot.edgeXwest;
                int Localz = block.z - seedPlot.edgeZnorth;
                if (block.y > AltMap[Localx][Localz]) {
                    AltMap[Localx][Localz] = block.y;
                    SurfaceMap[Localx][Localz] = block.y;
                    AltBlockMap[Localx][Localz] = block.id;
                }
            }
        }

        // Build SeaMap from AltBlockMap
        for (int lx = 0; lx < PlotOriginPoint.plotSize; lx++) {
            for (int lz = 0; lz < PlotOriginPoint.plotSize; lz++) {
                String altBlock = AltBlockMap[lx][lz];
                SeaMap[lx][lz] = AltMap[lx][lz] <= 62 && altBlock != null
                        && (altBlock.contains("water") || altBlock.contains("lava"));
            }
        }
        //slope map that calculates by cell neighbour comparisons
        for (int sx = seedPlot.edgeXwest; sx < seedPlot.edgeXeast; sx++) {
            for (int sz = seedPlot.edgeZnorth; sz < seedPlot.edgeZsouth; sz++) {
                int Slopex = sx - seedPlot.edgeXwest;
                int Slopez = sz - seedPlot.edgeZnorth;
                int MaxSlope = 0;

                // hard checks to prevent grid overstep
                if (Slopex < PlotOriginPoint.plotBorder){
                    int EastDiff = Math.abs(AltMap[Slopex][Slopez] - AltMap[Slopex+1][Slopez]);
                    if (EastDiff > MaxSlope) {
                        MaxSlope = EastDiff;
                    }
                }
                if (Slopez < PlotOriginPoint.plotBorder) {
                    int SouthDiff = Math.abs(AltMap[Slopex][Slopez] - AltMap[Slopex][Slopez + 1]);
                    if (SouthDiff > MaxSlope) {
                        MaxSlope = SouthDiff;
                    }
                }
                SlopeMap[Slopex][Slopez] = MaxSlope;
            }
        }
        //map all arrays onto the data object and return it for NTerrain
        return new DeepScannedStats (AltMap, SeaMap, SlopeMap, SurfaceMap, seedPlot);
    }
}