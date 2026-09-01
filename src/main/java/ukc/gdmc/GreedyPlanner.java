package ukc.gdmc;

import java.util.ArrayList;
import java.util.List;
public class GreedyPlanner {
    // All the important building identifying nodes taken from PlanPlacePhase
    // AnchorNodes list will contain all the copper doors used as the new Anchor, doors x,z values are stored here
    // RootNode will contain the single polished andesite stair as the Root, also contains a specific x,z value
    public static List<int[]> AnchorNodes = new ArrayList<>();
    public static int[] RootNode = null;

    // The main method that runs the entire path generation phase
    // Network connects each Node to the one that's the closest to it
    // Uses Greedy Algo - Decides best/nearest option each stage of network building
    // Uses Manhattan Distance - Distance between two nodes, Used to find pairs in Greedy
    // Uses A* - Decides the most efficient path between two nodes, accounts for multiple maps of plot data
    public static void DeployNetwork(MinecraftApi api) throws Exception {
        if (RootNode == null) {
            System.out.println("Deployment failed, No root nodes found");
            return;
        }
        if (AnchorNodes.isEmpty()) {
            System.out.println("Deployment failed, No anchors node found");
            return;
        }
        //+1 to account for the Town Hall node
        System.out.println("Network includes: "+ (AnchorNodes.size()+1)+" builds");

        // A list to represent each pair of nodes created by Greedy
        // Starts with the RootNode so pathfinds centre outwards
        // Since Root is always central its easiest way to find the natural network
        List<int[]> NodeNetwork = new ArrayList<>();
        NodeNetwork.add(RootNode);

        // Iterates each anchor node over anchor list
        // Doesn't use Root because Greedy will search from root outwards
        for (int[] AnchorNode : AnchorNodes) {
            // Greedy design implementation, NearestNode is always prioritised at each step
            // Even if path could be more optimal post full node map exploration
            int[] NearestNode = findNearestNode(AnchorNode, NodeNetwork);
            System.out.println("Linking Node A[" + AnchorNode[0] + "," + AnchorNode[1] + "] to Node B[" + NearestNode[0] + "," + NearestNode[1] + "]");
            List<NodeData> AStarPath = GeneratePath.MapPath(AnchorNode[0], AnchorNode[1], NearestNode[0], NearestNode[1]);

            // important null check, only adds paths onto the network if it passed A* pathfinding
            // if not the path is skipped from the network
            if (AStarPath != null) {
                GeneratePath.ExecuteConstruction(AStarPath, api);
                NodeNetwork.add(AnchorNode);
                System.out.println("Generation passed, nodes");
            } else {
                System.out.println("Generation failed, isolated house is skipped within the network");
            }
        }
    }

    // Uses the Manhattan distance to find the nearest node from the entire network to the current node
    // Manhattan due to natural paths in minecraft being cardinally placed due to the world being cube based
    // The NearestDistance being Max Val ensures the first node is found
    // After each node on the map is found, then nearest one gets connected, then it loops from the new current to the next
    private static int[] findNearestNode(int[] CurrentNode, List<int[]> NodeNetwork) {
        int[] Nearest = null;
        int NearestDistance = Integer.MAX_VALUE;

        //mDistance = Manhattan Distance
        // Iterates current node called PotentialNode over the entire Node network so it allows the root to expand
        for (int[] PotentialNode : NodeNetwork) {
            int mDistance = Math.abs(CurrentNode[0] - PotentialNode[0]) + Math.abs(CurrentNode[1] - PotentialNode[1]);
            if (mDistance < NearestDistance) {
                NearestDistance = mDistance;
                Nearest = PotentialNode;
            }
        }
        return Nearest;
    }
}