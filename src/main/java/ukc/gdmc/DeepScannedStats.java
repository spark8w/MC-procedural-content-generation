package ukc.gdmc;

public class DeepScannedStats {
    public int [][] AltMap; //Altitude map
    public boolean [][] SeaMap ;
    public int[][] SlopeMap ;
    public int[][] SurfaceMap;
    public PlotOriginPoint seedPlot;


    DeepScannedStats(int[][] AltMap, boolean[][] SeaMap, int [][] SlopeMap, int[][] SurfaceMap, PlotOriginPoint seedPlot){
        this.AltMap = AltMap;
        this.SeaMap = SeaMap;
        this.SlopeMap = SlopeMap;
        this.SurfaceMap = SurfaceMap;
        this.seedPlot = seedPlot;
    }
    /*
    Three 2D Arrays to map out multiple grids of data
    AltMap = Y level data map
    SeaMap = Water related data
    SlopeMap = Neighbour block related steepness
    SeedPlot = Champion plot data
     */
}