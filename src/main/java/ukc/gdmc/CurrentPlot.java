package ukc.gdmc;

public class CurrentPlot {
    public static DeepScannedStats plot;
    public static ExclusionManager exclusions = new ExclusionManager();
}
// Use CurrentPlot.plot.x
// x can be any of the three 2D arrays or the plot data from DeepScannedStats
// This class is to keep a universal object that holds DeepScannedData
// This data is meant to be referenced within your main scripts when you reference my data for your tasks
// Example e.g. System.out.println("AltMap centre: " + CurrentPlot.plot.AltMap[32][32]);