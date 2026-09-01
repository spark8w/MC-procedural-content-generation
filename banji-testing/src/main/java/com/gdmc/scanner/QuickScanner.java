package com.gdmc.scanner;

import com.gdmc.model.PlotCandidate;
import com.gdmc.model.QuickScanResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuickScanner {

    private static final int PLOT_SIZE           = 64;
    private static final int STRIDE              = 4;
    private static final int GRADIENT_THRESHOLD  = 8;
    private static final int SEA_COUNT_THRESHOLD = 10;
    private static final int SEA_LEVEL           = 62;
    private static final int SCAN_WIDTH          = 256;

    private final GdmcHttpClient http;

    public QuickScanner(GdmcHttpClient http) {
        this.http = http;
    }

    public QuickScanResult scan(long seed) {
        List<PlotCandidate> allPlots  = new ArrayList<>();
        List<PlotCandidate> bestPlots = new ArrayList<>();

        Random rng    = new Random(seed);
        int    startX = rng.nextInt(STRIDE);
        int    startZ = rng.nextInt(STRIDE);

        System.out.printf("[QuickScan] seed=%d startX=%d startZ=%d%n",
                seed, startX, startZ);

        for (int wx = startX; wx + PLOT_SIZE <= SCAN_WIDTH; wx += STRIDE) {
            for (int wz = startZ; wz + PLOT_SIZE <= SCAN_WIDTH; wz += STRIDE) {
                PlotCandidate plot = evaluatePlot(wx, wz);
                allPlots.add(plot);
                if (plot.gradient <= GRADIENT_THRESHOLD
                        && plot.seaCount <= SEA_COUNT_THRESHOLD) {
                    bestPlots.add(plot);
                }
            }
        }

        PlotCandidate champion = null;
        if (bestPlots.isEmpty()) {
            System.out.println("[QuickScan] No valid plot found");
        } else {
            champion = bestPlots.stream()
                    .min((a, b) -> Integer.compare(a.gradient, b.gradient))
                    .orElse(null);
            System.out.println("[QuickScan] Champion: " + champion);
        }

        return new QuickScanResult(allPlots, bestPlots, champion);
    }

    private PlotCandidate evaluatePlot(int wx, int wz) {
        int minY     = Integer.MAX_VALUE;
        int maxY     = Integer.MIN_VALUE;
        int seaCount = 0;

        for (int x = 0; x < PLOT_SIZE; x += STRIDE) {
            for (int z = 0; z < PLOT_SIZE; z += STRIDE) {
                ColumnData col = readColumn(wx + x, wz + z);
                if (col.highestSolidY < minY) minY = col.highestSolidY;
                if (col.highestSolidY > maxY) maxY = col.highestSolidY;
                if (col.hasWaterAtSeaLevel)   seaCount++;
            }
        }

        int gradient = (maxY == Integer.MIN_VALUE) ? 0 : (maxY - minY);
        return new PlotCandidate(
                wz, wz + PLOT_SIZE,
                wx + PLOT_SIZE, wx,
                gradient, seaCount
        );
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
            System.err.println("[QuickScan] WARNING column ("
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