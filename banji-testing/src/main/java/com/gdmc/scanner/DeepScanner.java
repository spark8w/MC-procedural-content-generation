package com.gdmc.scanner;

import com.gdmc.model.DeepScanResult;
import com.gdmc.model.PlotCandidate;

public class DeepScanner {

    private static final int SEA_LEVEL = 62;
    private static final int PLOT_SIZE = 64;

    private final GdmcHttpClient http;

    public DeepScanner(GdmcHttpClient http) {
        this.http = http;
    }

    public DeepScanResult scan(PlotCandidate plot) {
        int[][]     altMap = new int[PLOT_SIZE][PLOT_SIZE];
        boolean[][] seaMap = new boolean[PLOT_SIZE][PLOT_SIZE];

        System.out.println("[DeepScan] Scanning: " + plot);

        for (int x = 0; x < PLOT_SIZE; x++) {
            for (int z = 0; z < PLOT_SIZE; z++) {
                ColumnData col = readColumn(plot.west + x, plot.north + z);
                altMap[x][z]   = col.highestSolidY;
                seaMap[x][z]   = col.hasWaterAtSeaLevel;
            }
            if (x % 8 == 0)
                System.out.printf("[DeepScan] %d/%d columns read%n",
                        x * PLOT_SIZE, PLOT_SIZE * PLOT_SIZE);
        }

        int[][] slopeMap = buildSlopeMap(altMap);
        System.out.println("[DeepScan] Complete.");
        return new DeepScanResult(altMap, slopeMap, seaMap);
    }

    private int[][] buildSlopeMap(int[][] altMap) {
        int[][] slopeMap = new int[PLOT_SIZE][PLOT_SIZE];
        for (int x = 0; x < PLOT_SIZE; x++) {
            for (int z = 0; z < PLOT_SIZE; z++) {
                if (x == PLOT_SIZE - 1 || z == PLOT_SIZE - 1)
                    slopeMap[x][z] = 0;
                else
                    slopeMap[x][z] = Math.abs(altMap[x][z] - altMap[x+1][z]);
            }
        }
        return slopeMap;
    }

    private ColumnData readColumn(int x, int z) {
        try {
            String   raw    = http.getColumn(x, z);
            String[] blocks = raw.trim().split("\\r?\\n");
            int     highestSolid  = -64;
            boolean hasWaterAtSea = false;
            for (int i = 0; i < blocks.length; i++) {
                int    y       = -64 + i;
                String blockId = blocks[i].trim().toLowerCase();
                if (y == SEA_LEVEL && blockId.contains("water"))
                    hasWaterAtSea = true;
                if (!blockId.contains("air")
                        && !blockId.contains("water")
                        && !blockId.isEmpty())
                    highestSolid = y;
            }
            return new ColumnData(highestSolid, hasWaterAtSea);
        } catch (Exception e) {
            System.err.println("[DeepScan] WARNING column ("
                    + x + "," + z + "): " + e.getMessage());
            return new ColumnData(64, false);
        }
    }

    private static class ColumnData {
        final int     highestSolidY;
        final boolean hasWaterAtSeaLevel;
        ColumnData(int y, boolean sea) {
            this.highestSolidY      = y;
            this.hasWaterAtSeaLevel = sea;
        }
    }
}