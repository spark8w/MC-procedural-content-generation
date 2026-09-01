package ukc.gdmc;

import java.util.ArrayList;
import java.util.List;

public class GeneratePath {
    //A* maps a path between two local coordinates
    //EntryLocal and ExitLocal represent the starting and end goal nodes.
    public static List<NodeData> MapPath (int EntryLocalX, int EntryLocalZ, int ExitLocalX, int ExitLocalZ){
        AStarGen PathingGenerator = new AStarGen(EntryLocalX, EntryLocalZ, ExitLocalX, ExitLocalZ);
        List<NodeData> PathGenerated = PathingGenerator.SearchPathing();

        if (PathGenerated != null) {
            System.out.println("A* Passed, Node Distance: " + PathGenerated.size());
        } else {
            System.out.println("A* Failed, No path available through anchors");
        }

        return PathGenerated;
    }

    //Converts local coord path data into world coord path data
    //Surface map is used instead of AltMap as AltMap gets updated and skews data
    //An exclusion method is called to ensure pathdata is transferred for reforestation referencing

    public static void ExecuteConstruction(List<NodeData> PathingGenerated, MinecraftApi api) throws Exception{
        if (PathingGenerated == null){
            System.out.println("Carpet deployment failed, Null path data");
            return;
        }
        // All blocks are placed during a single api call
        List<MinecraftApi.PutBlock> CarpetPath = new ArrayList<>();

        for (NodeData Block : PathingGenerated){
            //coord translation
            int GlobalX = Block.LocalX + CurrentPlot.plot.seedPlot.edgeXwest;
            int GlobalZ = Block.LocalZ + CurrentPlot.plot.seedPlot.edgeZnorth;
            // places y +1 so that carpet is placed directly on top of the block listed in the pathing
            int GlobalY = CurrentPlot.plot.SurfaceMap[Block.LocalX][Block.LocalZ] + 1;

            CarpetPath.add(new MinecraftApi.PutBlock(GlobalX,GlobalY, GlobalZ, "minecraft:red_carpet"));
            //markRoadTile ensures pathing data is stored for reference
            CurrentPlot.exclusions.markRoadTile(Block.LocalX, Block.LocalZ);
        }
        //single bulk HTTP request
        //Entire path is placed
        api.setBlocksWorld(CarpetPath);
        System.out.println("Carpet deployment successful, Placed Carpet: " + CarpetPath.size());
    }
}