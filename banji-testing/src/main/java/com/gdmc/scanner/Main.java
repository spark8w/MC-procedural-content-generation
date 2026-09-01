package com.gdmc.scanner;

import com.gdmc.model.DeepScanResult;
import com.gdmc.model.QuickScanResult;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("============================================");
        System.out.println("  GDMC Scanner — Banji Sule");
        System.out.println("  Minecraft 1.21.11 / GDMC HTTP 1.8.4");
        System.out.println("============================================");

        GdmcHttpClient http = new GdmcHttpClient();

        System.out.println("\n[Step 1] Connecting to Minecraft...");
        if (!http.isConnected()) {
            System.out.println("ERROR: Cannot connect.");
            System.out.println("Make sure:");
            System.out.println("  1. Minecraft 1.21.11 is open");
            System.out.println("  2. A world is loaded");
            System.out.println("  3. GDMC mod 1.8.4 is installed");
            System.out.println("  4. You ran /setbuildarea 0 0 0 256 255 256");
            return;
        }
        System.out.println("Connected! " + http.getVersion());

        System.out.println("\n[Step 2] Running QuickScan...");
        QuickScanner    qs     = new QuickScanner(http);
        QuickScanResult result = qs.scan(42L);
        System.out.println("  All plots:  " + result.allPlots.size());
        System.out.println("  Best plots: " + result.bestPlots.size());

        if (result.champion == null) {
            System.out.println("  No champion found.");
            return;
        }
        System.out.println("  Champion: " + result.champion);

        System.out.println("\n[Step 3] Running DeepScan...");
        DeepScanner    ds   = new DeepScanner(http);
        DeepScanResult deep = ds.scan(result.champion);

        System.out.println("\n[Results]");
        System.out.println("  altMap[0][0]     = " + deep.altMap[0][0]);
        System.out.println("  altMap[32][32]   = " + deep.altMap[32][32]);
        System.out.println("  altMap[63][63]   = " + deep.altMap[63][63]);
        System.out.println("  slopeMap[32][32] = " + deep.slopeMap[32][32]);
        System.out.println("  seaMap[0][0]     = " + deep.seaMap[0][0]);

        System.out.println("\n[Step 4] Marking plot in Minecraft...");
        int y = deep.altMap[0][0] + 1;
        http.markPlotBorder(
                result.champion.north,
                result.champion.south,
                result.champion.east,
                result.champion.west, y
        );
        http.runCommand(
                "say GDMC scan done! Champion plot marked with gold blocks.");

        System.out.println("\n============================================");
        System.out.println("  DONE! Switch to Minecraft — look for");
        System.out.println("  the gold block border on the ground.");
        System.out.println("============================================");
    }
}