package ukc.gdmc;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScanPhase {
    public static final String[] WeakBiomes ={
            "jungle", "ocean", "frozen", "mushroom", "swamp",
            "mangrove", "the_end", "the_nether", "badlands",
            "peaks", "deep_dark"
    };
    public static void RunScanPhase () throws Exception {
        MinecraftApi aPI = new MinecraftApi(MinecraftApi.SERVER_URL);

        //initial scan that finds plots
        //this scan gets looped n times
        //generates n number of plots for the all plots list
        //
        List<QuickScannedStats> allPlots = new ArrayList<>();
        List<QuickScannedStats> bestPlots = new ArrayList<>();
        Random rngTool = new Random();
        for (int n = 0; n < 30; n++) {
            int centreX = rngTool.nextInt(2001) - 1000; //-1000 to 1000 coordinate boundry
            int centreZ = rngTool.nextInt(2001) - 1000;
            PlotOriginPoint seedPlot = new PlotOriginPoint(centreX, centreZ);
            String PlotBiome = aPI.getBiome(centreX,319,centreZ); // 319 is 1 below world height to gurantee surface biome

            //for each loop to check chunk biome against hardcoded bad biomes, skips plot if biome is weak
            //checks for overlap in weak biomes from api call vs string list of bad biomes
            boolean isWeakBiome = false;
            for (String weakBiome : WeakBiomes){
                if (PlotBiome.contains(weakBiome)){
                    isWeakBiome = true;
                    break;
                }
            }
            // boolean sout to state what biome was detected if, it is weak
            if (isWeakBiome){
                System.out.println("Weak Biome detected: "+ PlotBiome);
                continue;
            }

            TerrainScanner scanner = new TerrainScanner(seedPlot, aPI);
            QuickScannedStats results = scanner.quickScan();
            System.out.println("Gradient:" + results.Gradient);
            System.out.println("SeaCount:" + results.SeaCount);
            System.out.println("West: " + results.seedPlot.edgeXwest);
            System.out.println("East: " + results.seedPlot.edgeXeast);
            System.out.println("South: " + results.seedPlot.edgeZsouth);
            System.out.println("North: " + results.seedPlot.edgeZnorth);
            System.out.println("Plot num: " + n);
            allPlots.add(results);
        }

        //first screening process
        //hard checks for hardcoded numerical limits

        for (QuickScannedStats plot : allPlots) {
            if (plot.Gradient <= PlotOriginPoint.plotSize/4 && plot.SeaCount <= (PlotOriginPoint.plotSize*PlotOriginPoint.plotSize)/8) {
                bestPlots.add(plot);
            }
        }
        System.out.println("All plots: " + allPlots.size());
        System.out.println("Best plots: " + bestPlots.size());


        //second screening process
        //grading decides values of each plot
        //best plot is chosen as the championPlot
        //based on weighted grading where lower is better
        QuickScannedStats championPlot = null;
        int lowestGrade = Integer.MAX_VALUE;

        for (QuickScannedStats plot : bestPlots) {
            int plotGrade = plot.Gradient + (plot.SeaCount * 3);
            if (plotGrade < lowestGrade) {
                lowestGrade = plotGrade;
                championPlot = plot;
            }
        }

        // final part of the quickScan process
        // the data of champion plot gets transferred to deepScan scanner
        if (championPlot != null) {
            System.out.println("Champion gradient: " + championPlot.Gradient);
            System.out.println("Champion SeaCount: " + championPlot.SeaCount);
            System.out.println("Champion West: " + championPlot.seedPlot.edgeXwest);
            System.out.println("Champion East: " + championPlot.seedPlot.edgeXeast);
            System.out.println("Champion South: " + championPlot.seedPlot.edgeZsouth);
            System.out.println("Champion North: " + championPlot.seedPlot.edgeZnorth);

            TerrainScanner deepScanner = new TerrainScanner(championPlot.seedPlot, aPI);
            CurrentPlot.plot = deepScanner.deepScan();
            //calling block plotSize/2,plotSize/2 which is central
            //it is called from all maps to validate data and deepScan method
            System.out.println("AltMap centre: " + CurrentPlot.plot.AltMap[PlotOriginPoint.plotSize / 2][PlotOriginPoint.plotSize / 2]);
            System.out.println("SlopeMap centre: " + CurrentPlot.plot.SlopeMap[PlotOriginPoint.plotSize / 2][PlotOriginPoint.plotSize / 2]);
            System.out.println("SeaMap centre: " + CurrentPlot.plot.SeaMap[PlotOriginPoint.plotSize / 2][PlotOriginPoint.plotSize / 2]);

        } else {
            System.out.println("No valid plot found");
        }
    }
}