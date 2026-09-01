package ukc.gdmc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MasterMain {
    public static void main(String[] args) throws Exception {
        // Phase 0: Initialise MC server
        MinecraftApi APImain = new MinecraftApi(MinecraftApi.SERVER_URL);

        // Phase 1: Scan terrain, pick the best plot, deep-scan it into CurrentPlot.plot
        ScanPhase.RunScanPhase();

        if (CurrentPlot.plot == null) {
            System.out.println("No valid plot was found — aborting placement.");
            return;
        }

        // Phase 2: Snapshot the natural surface foliage BEFORE deforestation wipes it.
        FoliageData.capture(APImain);

        // Phase 3: Level ONLY the town hall footprint (+ a small buffer).
        // Levelling the whole 128x128 plot flattens natural terrain and looks unnatural.
        // Houses handle their own micro-levelling via TerrainLeveller when placed.
        PlotOriginPoint seed = CurrentPlot.plot.seedPlot;

        StructureLoader.LoadedStructure thStructure =
                StructureLoader.loadStructure("src/main/resources/structures/townhall.nbt");

        int thLocalX = (PlotOriginPoint.plotSize / 2) - (thStructure.width  / 2);
        int thLocalZ = (PlotOriginPoint.plotSize / 2) - (thStructure.length / 2);

        // Compute targetY as the median Y across the town hall footprint —
        // more stable than a single centre block.
        List<Integer> footprintYs = new ArrayList<>();
        for (int fx = thLocalX; fx < thLocalX + thStructure.width; fx++) {
            for (int fz = thLocalZ; fz < thLocalZ + thStructure.length; fz++) {
                if (fx >= 0 && fx < PlotOriginPoint.plotSize &&
                        fz >= 0 && fz < PlotOriginPoint.plotSize) {
                    footprintYs.add(CurrentPlot.plot.AltMap[fx][fz]);
                }
            }
        }
        Collections.sort(footprintYs);
        int targetY = footprintYs.get(footprintYs.size() / 2) + 1;
        System.out.println("MasterMain: targetY (median of town hall footprint) = " + targetY);

        // Small buffer around the town hall so the building sits flush with its
        // immediate surroundings rather than floating on a raised pad.
        int levelBuffer = 6;
        int levelWorldX = seed.edgeXwest + thLocalX - levelBuffer;
        int levelWorldZ = seed.edgeZnorth + thLocalZ - levelBuffer;
        int levelWidth  = thStructure.width  + (levelBuffer * 2);
        int levelDepth  = thStructure.length + (levelBuffer * 2);

        TerrainLeveller.levelArea(APImain,
                levelWorldX, levelWorldZ,
                levelWidth, levelDepth,
                targetY);

        // Phase 4: Place buildings (includes deforestation internally)
        PlanPlacePhase.RunHousePlacementPhase();

        // Phase 5: Generate road network between buildings
        GreedyPlanner.DeployNetwork(APImain);

        // Phase 6: Reforestation — plant custom tree schematics, then restore
        //          original ground foliage on remaining free cells.
        Reforestation.RunReforestationPhase();
    }
}
/*
 This is the master main class which will call upon other phase related main method entry points
 I have commented out irrelevant calls for myself John right now
 Feel free to comment and uncomment whichever methods are relevant
 Next stage I will make this main into a console terminal, so you can type what phase that
 bunches all the methods necessary for each phase but right now we will use this as bare bones.
 Right now rushes building phase has multiple scripts but mine has one
 So I could call mine with Scan in the console
 but rush would need load, place etc instead of just build so, I will implement this last to wire
 all of the pipeline together. For now just use this as if it was your main but just reference your local
 "Mains" from your own tasks/phases. just comment out others methods.
 I say comment in and out relevant methods as each method varies in size for example runScanPhase
 takes on average 1-3mins to complete but LoadStructure takes 10s.
 It's to have clean console results instead of lots of result driven bloat.
*/