package ukc.gdmc;


public class PlotOriginPoint {
    public static final int plotSize = 128;
    public static final int plotBorder = plotSize - 1;
    public static final int plotStart = 0;
    int edgeXeast;
    int edgeXwest;
    int edgeZsouth;
    int edgeZnorth;

    PlotOriginPoint(int centreX, int centreZ){
        edgeXeast  = centreX + (plotSize / 2);
        edgeXwest  = centreX - (plotSize / 2);
        edgeZsouth = centreZ + (plotSize / 2);
        edgeZnorth = centreZ - (plotSize / 2);
    }
    /* x is east or west depending on if you add or subtract
       z is south or north depending on if you add or subtract
       y is the block level
       OLD ->centre x+z create one block i use +/- 32 to create a 64x64 grid from one coordinate
       NEW ->centre x/z - (plotSize /2) calculates the same variable but allows the plot to scale in size rather than be hardcoded.
     */
}
