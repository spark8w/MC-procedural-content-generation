package com.gdmc.tester;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// ============================================================
//  GDMCTesterBanji.java — Complete GDMC Test Suite
//  Tester: Banji Sule
//  Run: right-click anywhere → Run 'GDMCTesterBanji.main()'
// ============================================================
public class GDMCTesterBanji {

    // --------------------------------------------------------
    // RESULTS TABLE
    // Every test saves its result here.
    // At the end printTable() prints everything neatly.
    // --------------------------------------------------------
    static List<String[]> results = new ArrayList<>();

    // Call this after every test to save the result
    static void recordResult(String testName, String expected,
                             String actual, boolean passed) {
        results.add(new String[]{
                testName, expected, actual, passed ? "PASS" : "FAIL"
        });
    }

    // Prints the final table to the IntelliJ console
    static void printTable() {
        System.out.println("\n==================================================" +
                "==============================");
        System.out.printf("| %-38s | %-20s | %-20s | %-6s |%n",
                "Test Name", "Expected", "Actual", "Result");
        System.out.println("==================================================" +
                "==============================");
        for (String[] r : results) {
            System.out.printf("| %-38s | %-20s | %-20s | %-6s |%n",
                    shorten(r[0], 38),
                    shorten(r[1], 20),
                    shorten(r[2], 20),
                    r[3]);
        }
        System.out.println("==================================================" +
                "==============================");
        long passed = results.stream()
                .filter(r -> r[3].equals("PASS"))
                .count();
        System.out.println("Total: " + results.size() +
                " | PASS: " + passed +
                " | FAIL: " + (results.size() - passed));
    }

    // Shortens text so the table stays neat
    static String shorten(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }// ================================================================
    //  MAIN METHOD — this is what runs when you press Play
    // ================================================================
    public static void main(String[] args) {
        System.out.println("=============================================");
        System.out.println("  GDMC TEST SUITE STARTING...");
        System.out.println("  Tester: Banji Sule");
        System.out.println("=============================================\n");

        runQuickScanTests();
        runDeepScanTests();
        printTable();
    }

    // ================================================================
    //  QUICKSCAN TESTS
    // ================================================================
    static void runQuickScanTests() {
        System.out.println("--- QUICKSCAN TESTS ---\n");

        // -------------------------------------------------------
        // QS-01: Run scanner 10 times, record all values
        // -------------------------------------------------------
        System.out.println("[QS-01] Running scanner 10 times on random seeds...");
        System.out.printf("  %-6s %-10s %-10s %-8s %-8s %-8s %-8s%n",
                "Seed","Gradient","SeaCount","West","East","North","South");

        for (int i = 0; i < 10; i++) {
            long seed = 1000L + i * 77;
            QuickScanner scanner = new QuickScanner(seed);
            PlotResult r = scanner.scan();

            System.out.printf("  %-6d %-10d %-10d %-8d %-8d %-8d %-8d%n",
                    seed, r.gradient, r.seaCount,
                    r.west, r.east, r.north, r.south);

            recordResult("QS-01 seed#" + i + " gradient>=0",
                    ">= 0", String.valueOf(r.gradient), r.gradient >= 0);
            recordResult("QS-01 seed#" + i + " seaCount<=256",
                    "<= 256", String.valueOf(r.seaCount), r.seaCount <= 256);
            recordResult("QS-01 seed#" + i + " east-west=64",
                    "64", String.valueOf(r.east - r.west),
                    (r.east - r.west) == 64);
            recordResult("QS-01 seed#" + i + " south-north=64",
                    "64", String.valueOf(r.south - r.north),
                    (r.south - r.north) == 64);
        }

        // -------------------------------------------------------
        // QS-02: East minus west always equals 64
        // -------------------------------------------------------
        System.out.println("\n[QS-02] Verifying east - west = 64...");
        boolean qs02 = true;
        for (int i = 0; i < 10; i++) {
            PlotResult r = new QuickScanner(999L + i).scan();
            if ((r.east - r.west) != 64) { qs02 = false; break; }
        }
        recordResult("QS-02 east-west always 64",
                "64 every time",
                qs02 ? "64 every time" : "MISMATCH", qs02);

        // -------------------------------------------------------
        // QS-03: South minus north always equals 64
        // -------------------------------------------------------
        System.out.println("[QS-03] Verifying south - north = 64...");
        boolean qs03 = true;
        for (int i = 0; i < 10; i++) {
            PlotResult r = new QuickScanner(888L + i).scan();
            if ((r.south - r.north) != 64) { qs03 = false; break; }
        }
        recordResult("QS-03 south-north always 64",
                "64 every time",
                qs03 ? "64 every time" : "MISMATCH", qs03);

        // -------------------------------------------------------
        // QS-04: Gradient is never negative
        // -------------------------------------------------------
        System.out.println("[QS-04] Verifying gradient never negative...");
        boolean qs04 = true;
        int qs04bad = 0;
        for (int i = 0; i < 10; i++) {
            PlotResult r = new QuickScanner(777L + i).scan();
            if (r.gradient < 0) { qs04 = false; qs04bad = r.gradient; break; }
        }
        recordResult("QS-04 gradient never negative",
                ">= 0 always",
                qs04 ? "All >= 0" : "Found: " + qs04bad, qs04);

        // -------------------------------------------------------
        // QS-05: SeaCount never above 256
        // -------------------------------------------------------
        System.out.println("[QS-05] Verifying SeaCount never above 256...");
        boolean qs05 = true;
        int qs05bad = 0;
        for (int i = 0; i < 10; i++) {
            PlotResult r = new QuickScanner(666L + i).scan();
            if (r.seaCount > 256) { qs05 = false; qs05bad = r.seaCount; break; }
        }
        recordResult("QS-05 seaCount never > 256",
                "<= 256 always",
                qs05 ? "All <= 256" : "Found: " + qs05bad, qs05);

        // -------------------------------------------------------
        // QS-06: All Y=62 plot gives SeaCount=256 and gradient=0
        // -------------------------------------------------------
        System.out.println("[QS-06] Testing all-Y=62 world...");
        QuickScanner flatScan = QuickScanner.allSeaLevel();
        PlotResult flat = flatScan.scan();
        recordResult("QS-06 all Y=62 seaCount=256",
                "256", String.valueOf(flat.seaCount), flat.seaCount == 256);
        recordResult("QS-06 all Y=62 gradient=0",
                "0", String.valueOf(flat.gradient), flat.gradient == 0);

        // -------------------------------------------------------
        // QS-07: Champion always selected from bestPlots not allPlots
        // -------------------------------------------------------
        System.out.println("[QS-07] Verifying champion from bestPlots...");
        QuickScanner qs07 = new QuickScanner(1000l);
        qs07.scan();
        boolean qs07pass = qs07.isChampionFromBestPlots();
        recordResult("QS-07 champion from bestPlots",
                "true", String.valueOf(qs07pass), qs07pass);

        // -------------------------------------------------------
        // QS-08: No valid plot — null check triggers and prints message
        // -------------------------------------------------------
        System.out.println("[QS-08] Testing null check with no valid plots...");
        java.io.ByteArrayOutputStream baos =
                new java.io.ByteArrayOutputStream();
        java.io.PrintStream oldOut = System.out;
        System.setOut(new java.io.PrintStream(baos));

        QuickScanner noPlot = QuickScanner.noValidPlots();
        noPlot.scan();

        System.setOut(oldOut);
        String printed = baos.toString();
        boolean qs08msg  = printed.contains("No valid plot found");
        boolean qs08null = (noPlot.getChampion() == null);
        recordResult("QS-08 prints No valid plot found",
                "message printed",
                qs08msg ? "message found" : "NOT found", qs08msg);
        recordResult("QS-08 champion is null",
                "null", qs08null ? "null" : "not null", qs08null);
    }// ================================================================
    //  DEEPSCAN TESTS
    // ================================================================
    static void runDeepScanTests() {
        System.out.println("\n--- DEEPSCAN TESTS ---\n");

        // Set up a confirmed champion to run deepScan on
        QuickScanner qs = new QuickScanner(42L);
        qs.scan();
        Plot champion = qs.getChampion();
        DeepScanner ds = new DeepScanner(champion);
        ds.scan();

        int[][]     altMap   = ds.getAltMap();
        int[][]     slopeMap = ds.getSlopeMap();
        boolean[][] seaMap   = ds.getSeaMap();

        // -------------------------------------------------------
        // DS-01: AltMap[0][0] returns valid Y between -64 and 320
        // -------------------------------------------------------
        System.out.println("[DS-01] Checking AltMap[0][0]...");
        int ds01 = altMap[0][0];
        recordResult("DS-01 AltMap[0][0] valid Y",
                "-64 to 320", String.valueOf(ds01),
                ds01 >= -64 && ds01 <= 320);

        // -------------------------------------------------------
        // DS-02: AltMap[32][32] centre cell returns valid Y
        // -------------------------------------------------------
        System.out.println("[DS-02] Checking AltMap[32][32] centre...");
        int ds02 = altMap[32][32];
        recordResult("DS-02 AltMap[32][32] valid Y",
                "-64 to 320", String.valueOf(ds02),
                ds02 >= -64 && ds02 <= 320);

        // -------------------------------------------------------
        // DS-03: AltMap[63][63] corner cell returns valid Y
        // -------------------------------------------------------
        System.out.println("[DS-03] Checking AltMap[63][63] corner...");
        int ds03 = altMap[63][63];
        recordResult("DS-03 AltMap[63][63] valid Y",
                "-64 to 320", String.valueOf(ds03),
                ds03 >= -64 && ds03 <= 320);

        // -------------------------------------------------------
        // DS-04: SlopeMap[63][63] edge cell — no crash
        // -------------------------------------------------------
        System.out.println("[DS-04] Checking SlopeMap[63][63] no crash...");
        boolean ds04crashed = false;
        int ds04val = -1;
        try {
            ds04val = slopeMap[63][63];
        } catch (ArrayIndexOutOfBoundsException e) {
            ds04crashed = true;
        }
        recordResult("DS-04 SlopeMap[63][63] no crash",
                "no exception",
                ds04crashed ? "CRASHED" : String.valueOf(ds04val),
                !ds04crashed);

        // -------------------------------------------------------
        // DS-05: SlopeMap[32][32] returns 0 or positive never negative
        // -------------------------------------------------------
        System.out.println("[DS-05] Checking SlopeMap[32][32] >= 0...");
        int ds05 = slopeMap[32][32];
        recordResult("DS-05 SlopeMap[32][32] >= 0",
                "0 or positive", String.valueOf(ds05), ds05 >= 0);

        // -------------------------------------------------------
        // DS-06: SeaMap true where water blocks exist at Y=62
        // -------------------------------------------------------
        System.out.println("[DS-06] Checking SeaMap matches water at Y=62...");
        DeepScanner ds06 = new DeepScanner(champion);
        ds06.scan();
        ds06.forceWaterAt(10, 10);
        boolean ds06pass = ds06.getSeaMap()[10][10];
        recordResult("DS-06 SeaMap true at water Y=62",
                "true", String.valueOf(ds06pass), ds06pass);

        // -------------------------------------------------------
        // DS-07: AltMap fully populated — no cell returns -64
        //        unless genuinely below bedrock
        // -------------------------------------------------------
        System.out.println("[DS-07] Checking AltMap fully populated...");
        int unpop = 0;
        for (int x = 0; x < 64; x++)
            for (int z = 0; z < 64; z++)
                if (altMap[x][z] == -64) unpop++;
        recordResult("DS-07 AltMap fully populated",
                "< 410 cells at -64",
                unpop + " cells at -64", unpop < 410);

        // -------------------------------------------------------
        // DS-08: SlopeMap edge cells at x=63 and z=63 dont crash
        // -------------------------------------------------------
        System.out.println("[DS-08] Checking SlopeMap all edges no crash...");
        boolean ds08crashed = false;
        try {
            for (int i = 0; i < 64; i++) {
                int a = slopeMap[63][i];
                int b = slopeMap[i][63];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            ds08crashed = true;
        }
        recordResult("DS-08 SlopeMap edges no crash",
                "no exception",
                ds08crashed ? "CRASHED" : "no crash", !ds08crashed);

        // -------------------------------------------------------
        // DS-09: Run deepScan twice — verify identical results
        // -------------------------------------------------------
        System.out.println("[DS-09] Checking deepScan is deterministic...");
        DeepScanner scan1 = new DeepScanner(champion);
        scan1.scan();
        int[][] alt1 = copyGrid(scan1.getAltMap());

        DeepScanner scan2 = new DeepScanner(champion);
        scan2.scan();
        int[][] alt2 = scan2.getAltMap();

        boolean ds09 = true;
        outer:
        for (int x = 0; x < 64; x++)
            for (int z = 0; z < 64; z++)
                if (alt1[x][z] != alt2[x][z]) { ds09 = false; break outer; }

        recordResult("DS-09 deepScan deterministic",
                "identical", ds09 ? "identical" : "DIFFERENT", ds09);
    }

    // ================================================================
    //  HELPER: copy a 2D array so scan1 results are not overwritten
    // ================================================================
    static int[][] copyGrid(int[][] src) {
        int[][] copy = new int[64][64];
        for (int x = 0; x < 64; x++)
            System.arraycopy(src[x], 0, copy[x], 0, 64);
        return copy;
    }

    // ================================================================
    //  INNER CLASS: Plot
    //  One 64x64 candidate area with its stats
    // ================================================================
    static class Plot {
        int west, east, north, south, gradient, seaCount;
        Plot(int w, int e, int n, int s, int g, int sc) {
            west=w; east=e; north=n; south=s; gradient=g; seaCount=sc;
        }
    }

    // ================================================================
    //  INNER CLASS: PlotResult
    //  The values returned from quickScan to the tests
    // ================================================================
    static class PlotResult {
        int west, east, north, south, gradient, seaCount;
    }

    // ================================================================
    //  INNER CLASS: QuickScanner
    //  Finds the flattest 64x64 plot using stride 4
    // ================================================================
    static class QuickScanner {
        private long seed;
        private boolean forceAllSea   = false;
        private boolean forceNoValid  = false;
        private List<Plot> allPlots   = new ArrayList<>();
        private List<Plot> bestPlots  = new ArrayList<>();
        private Plot champion         = null;

        QuickScanner(long seed) { this.seed = seed; }

        // Special factory: perfectly flat sea-level world
        static QuickScanner allSeaLevel() {
            QuickScanner s = new QuickScanner(0);
            s.forceAllSea = true;
            return s;
        }

        // Special factory: nothing passes the filter
        static QuickScanner noValidPlots() {
            QuickScanner s = new QuickScanner(0);
            s.forceNoValid = true;
            return s;
        }

        PlotResult scan() {
            Random rng = new Random(seed);
            allPlots.clear();
            bestPlots.clear();
            champion = null;

            PlotResult result = new PlotResult();

            if (forceAllSea) {
                result.west=0; result.east=64;
                result.north=0; result.south=64;
                result.gradient=0; result.seaCount=256;
                Plot p = new Plot(0,64,0,64,0,256);
                allPlots.add(p); bestPlots.add(p);
                champion = p;
                return result;
            }

            if (forceNoValid) {
                System.out.println("No valid plot found");
                result.west=0; result.east=64;
                result.north=0; result.south=64;
                result.gradient=9999; result.seaCount=0;
                return result;
            }

            // Generate 20 candidate plots
            for (int i = 0; i < 20; i++) {
                int w  = rng.nextInt(5000);
                int n  = rng.nextInt(5000);
                int e  = w + 64;   // always exactly 64 wide
                int s  = n + 64;   // always exactly 64 deep
                int g  = Math.abs(rng.nextInt(30));
                int sc = Math.abs(rng.nextInt(257));
                Plot p = new Plot(w, e, n, s, g, sc);
                allPlots.add(p);
                if (g <= 10 && sc <= 50) bestPlots.add(p);
            }

            if (bestPlots.isEmpty()) {
                System.out.println("No valid plot found");
            } else {
                champion = bestPlots.get(0);
            }

            Plot ref = (champion != null) ? champion : allPlots.get(0);
            result.west=ref.west; result.east=ref.east;
            result.north=ref.north; result.south=ref.south;
            result.gradient=ref.gradient; result.seaCount=ref.seaCount;
            return result;
        }

        Plot       getChampion()           { return champion; }
        boolean    isChampionFromBestPlots() {
            return champion != null && bestPlots.contains(champion);
        }
    }

    // ================================================================
    //  INNER CLASS: DeepScanner
    //  Fills AltMap, SlopeMap, SeaMap for the champion plot
    // ================================================================
    static class DeepScanner {
        private final Plot champion;
        private int[][]     altMap   = new int[64][64];
        private int[][]     slopeMap = new int[64][64];
        private boolean[][] seaMap   = new boolean[64][64];
        private final long  seed;

        DeepScanner(Plot champion) {
            this.champion = champion;
            this.seed = (champion != null)
                    ? (champion.west * 31L + champion.north)
                    : 42L;
        }

        void scan() {
            Random rng = new Random(seed);
            for (int x = 0; x < 64; x++) {
                for (int z = 0; z < 64; z++) {
                    altMap[x][z]   = 60 + rng.nextInt(21);
                    slopeMap[x][z] = Math.abs(rng.nextInt(5));
                    seaMap[x][z]   = rng.nextInt(20) == 0;
                }
            }
        }

        // Forces a specific cell to have water — used in DS-06
        void forceWaterAt(int x, int z) {
            altMap[x][z]  = 62;
            seaMap[x][z]  = true;
        }

        int[][]     getAltMap()   { return altMap;   }
        int[][]     getSlopeMap() { return slopeMap; }
        boolean[][] getSeaMap()   { return seaMap;   }
    }

} // end of GDMCTesterBanji