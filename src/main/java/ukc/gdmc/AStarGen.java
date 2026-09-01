package ukc.gdmc;
import java.util.*;

public class AStarGen {
    private final int EntryLocalX;
    private final int ExitLocalX;
    private final int EntryLocalZ;
    private final int ExitLocalZ;
    private final boolean [][] SeaMap;
    private PriorityQueue<NodeData>  OpenList;
    private boolean[][] ClosedMap;
    private NodeData[][] Map;
    private static final int [][] CardinalDirections = {{1,0},{-1,0},{0,1},{0,-1}};
    //All variables necessary for the A* algorithm
    //Start and end values so the A* has an entry and exit point for the pathing
    //PriorityQueue lista = steps to take calculated by comparison of fCost
    //2D Arrays = plotSize * plotSize grids for simple and easy data referencing
    AStarGen(int EntryLocalX, int EntryLocalZ, int ExitLocalX, int ExitLocalZ){
        this.SeaMap = CurrentPlot.plot.SeaMap;
        this.EntryLocalX = EntryLocalX;
        this.EntryLocalZ = EntryLocalZ;
        this.ExitLocalX = ExitLocalX;
        this.ExitLocalZ = ExitLocalZ;
        this.OpenList = new PriorityQueue<>();
        this.ClosedMap = new boolean[PlotOriginPoint.plotSize][PlotOriginPoint.plotSize];
        this.Map = new NodeData[PlotOriginPoint.plotSize][PlotOriginPoint.plotSize];
    }

    public List<NodeData> SearchPathing() {
        int hCost = (Math.abs(EntryLocalX - ExitLocalX) + Math.abs(EntryLocalZ - ExitLocalZ));
        NodeData EntryNode = new NodeData(EntryLocalX, EntryLocalZ, null);
        EntryNode.gCost = 0;
        EntryNode.hCost = hCost;
        OpenList.add(EntryNode);

        //Costs initialisation and node order list creation
        while(!OpenList.isEmpty()){
            NodeData CurrentNode = OpenList.poll();
            if (ClosedMap[CurrentNode.LocalX][CurrentNode.LocalZ]){
                continue; //stale old node check & skip
            }
            if(CurrentNode.LocalX == ExitLocalX && CurrentNode.LocalZ == ExitLocalZ){
                List<NodeData> Pathing = new ArrayList<>();
                NodeData BackTrack = CurrentNode;
                while (BackTrack  != null){
                    Pathing.add(BackTrack);
                    BackTrack = BackTrack.Parent;
                }
                Collections.reverse(Pathing);
                return Pathing;
            }
            ClosedMap[CurrentNode.LocalX][CurrentNode.LocalZ]=true;
            for(int[]Direction: CardinalDirections) {
                int NeighbourNodeX = CurrentNode.LocalX + Direction[0];
                int NeighbourNodeZ = CurrentNode.LocalZ + Direction[1];
                if (NeighbourNodeX < PlotOriginPoint.plotStart || NeighbourNodeX > PlotOriginPoint.plotBorder || NeighbourNodeZ < PlotOriginPoint.plotStart || NeighbourNodeZ > PlotOriginPoint.plotBorder){
                    continue;
                }
                if (ClosedMap[NeighbourNodeX][NeighbourNodeZ]){
                    continue;
                }
                if (SeaMap[NeighbourNodeX][NeighbourNodeZ]){
                    continue;
                }

                //OLD Obstacle code VV
                //Obstacle code comment it out if you need VV this is broken without clear AnchorNodes - Should fix once roofless anchor nodes are introduced
                // 10 Represents the height difference needed to mark the collumn as skippable
                // an encapsulated house would skip on its exterior
                // 10 may need to be reduced depending if we ever standardise the avg height of the builds
                // May become redundant if, we obstacle detect using exclusion class instead but that requires the code to work for post placement

                //NEW Obstacle code
                //Instead of using difference between multiple y level maps surface/alt
                //The obstacle code now just references Obstacle Map data with removed buffer zone
                if (CurrentPlot.exclusions.ObstacleMap[NeighbourNodeX][NeighbourNodeZ]) {
                    continue;
                }
                int Gradient = Math.abs(CurrentPlot.plot.AltMap[CurrentNode.LocalX][CurrentNode.LocalZ] - CurrentPlot.plot.AltMap[NeighbourNodeX][NeighbourNodeZ]);
                int gCost = CurrentNode.gCost + 10 + Gradient;
                int NeighboursHCost = (Math.abs(NeighbourNodeX - ExitLocalX) + Math.abs(NeighbourNodeZ - ExitLocalZ));

                //Introduced reusable nodes to the map
                NodeData NeighbourNode = Map[NeighbourNodeX][NeighbourNodeZ];
                if (NeighbourNode == null){
                    NeighbourNode = new NodeData(NeighbourNodeX, NeighbourNodeZ, CurrentNode);
                    Map[NeighbourNodeX][NeighbourNodeZ] = NeighbourNode;
                } else if (NeighbourNode.gCost <= gCost) {
                    continue;
                }
                else {
                    NeighbourNode.Parent = CurrentNode;
                }

                NeighbourNode.gCost = gCost;
                NeighbourNode.hCost = NeighboursHCost;
                OpenList.add(NeighbourNode);
            }
        }
        return null;
    }
}