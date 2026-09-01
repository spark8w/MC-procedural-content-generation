package com.gdmc.model;

public class DeepScanResult {

    public final int[][]     altMap;
    public final int[][]     slopeMap;
    public final boolean[][] seaMap;

    public DeepScanResult(int[][] altMap,
                          int[][] slopeMap,
                          boolean[][] seaMap) {
        this.altMap   = altMap;
        this.slopeMap = slopeMap;
        this.seaMap   = seaMap;
    }
}