package ukc.gdmc;

public class NodeData implements Comparable<NodeData>{
    public final int LocalX;
    // NEW -> the comments below are old the grids now scale dependent on plotStart,Border,Size as the plot was hardcoded to
    // OLD BELOW VV
    // 64x64 but that changes into for example 0 , 255 , 256 for the start border and size. 256 x 256.
    // Changes were implemented to accommodates for scalability.
    // But the below comments still address the core concept of the classes code.

    //Lx and Lz are both local coordinates that map onto the 64x64 grid
    //But they do not reference real world coords 0-63 like the Array grids
    //Lx goes 0= most west and 63= most east
    //Lz goes 0= most north and 63= most south

    /*
    G cost top left - How far the nodes is from the anchor node
    H cost top right - How far the node is from the goal node
    F cost = G cost + H cost
    */

    public final int LocalZ;
    public int gCost;
    public int hCost;
    public int getFCost(){
        return gCost + hCost;
    }
    public NodeData Parent;
    NodeData(int LocalX, int LocalZ, NodeData Parent){
        this.LocalX = LocalX;
        this.LocalZ = LocalZ;
        this.Parent = Parent;
    }
    //
    @Override
    public int compareTo(NodeData other){
        int CompareFCost = Integer.compare(this.getFCost(), other.getFCost());
        if (CompareFCost == 0){
            return Integer.compare(this.hCost, other.hCost);
        }
        return CompareFCost;
    }
}