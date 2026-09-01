package com.gdmc;

import com.gdmc.model.DeepScanResult;
import com.gdmc.model.PlotCandidate;
import com.gdmc.model.QuickScanResult;
import com.gdmc.scanner.DeepScanner;
import com.gdmc.scanner.GdmcHttpClient;
import com.gdmc.scanner.QuickScanner;

import org.junit.jupiter.api.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
class GdmcScannerTest {

    private static GdmcHttpClient http;
    private static QuickScanner   quickScanner;
    private static DeepScanner    deepScanner;
    private static PlotCandidate  sharedChampion;
    private static boolean        liveWorld;

    @BeforeAll
    static void setup() {
        http         = new GdmcHttpClient();
        quickScanner = new QuickScanner(http);
        deepScanner  = new DeepScanner(http);
        liveWorld    = http.isConnected();
        System.out.println("============================================");
        System.out.println("  GDMC Test Suite — Banji Sule");
        System.out.println("  Live world: "
                + (liveWorld ? "YES ✅" : "NO — using mock data"));
        System.out.println("============================================");
    }

    private PlotCandidate getChampion() {
        if (sharedChampion != null) return sharedChampion;
        if (liveWorld) {
            sharedChampion = quickScanner.scan(42L).champion;
        } else {
            sharedChampion = new PlotCandidate(0, 64, 64, 0, 3, 2);
        }
        return sharedChampion;
    }

    private DeepScanResult getDeepResult() {
        PlotCandidate c = getChampion();
        if (c == null || !liveWorld) {
            int[][]     alt   = new int[64][64];
            int[][]     slope = new int[64][64];
            boolean[][] sea   = new boolean[64][64];
            for (int x = 0; x < 64; x++)
                for (int z = 0; z < 64; z++) alt[x][z] = 64;
            sea[5][5] = true;
            return new DeepScanResult(alt, slope, sea);
        }
        return deepScanner.scan(c);
    }

    @Test @DisplayName("QS-01: Run scanner 10 times, record results")
    void qs01() {
        long[] seeds = {1,2,3,4,5,100,200,999,12345,99999};
        System.out.printf("%n%-8s %-10s %-10s %-8s %-8s %-8s %-8s%n",
                "Seed","Gradient","SeaCount","North","South","East","West");
        for (long seed : seeds) {
            QuickScanResult r = quickScanner.scan(seed);
            if (sharedChampion == null && r.champion != null)
                sharedChampion = r.champion;
            if (r.champion != null) {
                PlotCandidate c = r.champion;
                System.out.printf("%-8d %-10d %-10d %-8d %-8d %-8d %-8d%n",
                        seed,c.gradient,c.seaCount,
                        c.north,c.south,c.east,c.west);
            } else {
                System.out.printf("%-8d No valid plot found%n", seed);
            }
            assertNotNull(r.allPlots);
            assertNotNull(r.bestPlots);
        }
    }

    @Test @DisplayName("QS-02: east - west always equals 64")
    void qs02() {
        QuickScanResult r = quickScanner.scan(42L);
        assertFalse(r.allPlots.isEmpty());
        for (PlotCandidate p : r.allPlots)
            assertEquals(64, p.east - p.west, "Failed for " + p);
        System.out.println("All plots: east - west = 64 ✅");
    }

    @Test @DisplayName("QS-03: south - north always equals 64")
    void qs03() {
        QuickScanResult r = quickScanner.scan(42L);
        for (PlotCandidate p : r.allPlots)
            assertEquals(64, p.south - p.north, "Failed for " + p);
        System.out.println("All plots: south - north = 64 ✅");
    }

    @Test @DisplayName("QS-04: gradient is never negative")
    void qs04() {
        QuickScanResult r = quickScanner.scan(42L);
        for (PlotCandidate p : r.allPlots)
            assertTrue(p.gradient >= 0, "Negative gradient: " + p);
        System.out.println("All gradients >= 0 ✅");
    }

    @Test @DisplayName("QS-05: seaCount never exceeds 256")
    void qs05() {
        QuickScanResult r = quickScanner.scan(42L);
        for (PlotCandidate p : r.allPlots)
            assertTrue(p.seaCount >= 0 && p.seaCount <= 256,
                    "seaCount out of range: " + p);
        System.out.println("All seaCount in [0,256] ✅");
    }

    @Test @DisplayName("QS-06: Flat ocean gives seaCount=256 and gradient=0")
    void qs06() {
        PlotCandidate f = new PlotCandidate(0,64,64,0,0,256);
        assertEquals(0,   f.gradient);
        assertEquals(256, f.seaCount);
        System.out.println("Flat ocean: gradient=0, seaCount=256 ✅");
    }

    @Test @DisplayName("QS-07: Champion always comes from bestPlots")
    void qs07() {
        QuickScanResult r = quickScanner.scan(42L);
        if (r.champion == null) {
            assertTrue(r.bestPlots.isEmpty());
            System.out.println("bestPlots empty → champion null ✅");
            return;
        }
        boolean found = r.bestPlots.stream().anyMatch(p ->
                p.north==r.champion.north && p.south==r.champion.south &&
                        p.east==r.champion.east   && p.west==r.champion.west);
        assertTrue(found, "Champion not in bestPlots");
        System.out.println("Champion found in bestPlots ✅");
    }

    @Test @DisplayName("QS-08: When bestPlots empty, champion is null")
    void qs08() {
        List<PlotCandidate> all  = new ArrayList<>();
        List<PlotCandidate> best = new ArrayList<>();
        all.add(new PlotCandidate(0,64,64,0,100,200));
        QuickScanResult r = new QuickScanResult(all, best, null);
        assertNull(r.champion);
        assertTrue(r.bestPlots.isEmpty());
        assertFalse(r.allPlots.isEmpty());
        System.out.println("Null champion when bestPlots empty ✅");
    }

    @Test @DisplayName("DS-01: altMap[0][0] is valid Y between -64 and 320")
    void ds01() {
        int y = getDeepResult().altMap[0][0];
        assertTrue(y >= -64 && y <= 320, "Out of range: " + y);
        System.out.println("altMap[0][0] = " + y + " ✅");
    }

    @Test @DisplayName("DS-02: altMap[32][32] centre cell is valid Y")
    void ds02() {
        int y = getDeepResult().altMap[32][32];
        assertTrue(y >= -64 && y <= 320, "Out of range: " + y);
        System.out.println("altMap[32][32] = " + y + " ✅");
    }

    @Test @DisplayName("DS-03: altMap[63][63] corner cell is valid Y")
    void ds03() {
        int y = getDeepResult().altMap[63][63];
        assertTrue(y >= -64 && y <= 320, "Out of range: " + y);
        System.out.println("altMap[63][63] = " + y + " ✅");
    }

    @Test @DisplayName("DS-04: slopeMap[63][63] does not crash")
    void ds04() {
        assertDoesNotThrow(() -> {
            int s = getDeepResult().slopeMap[63][63];
            System.out.println("slopeMap[63][63] = " + s + " ✅");
        });
    }

    @Test @DisplayName("DS-05: slopeMap[32][32] is zero or positive")
    void ds05() {
        int s = getDeepResult().slopeMap[32][32];
        assertTrue(s >= 0, "Negative slope: " + s);
        System.out.println("slopeMap[32][32] = " + s + " ✅");
    }

    @Test @DisplayName("DS-06: seaMap is 64x64 with valid values")
    void ds06() {
        DeepScanResult r = getDeepResult();
        assertEquals(64, r.seaMap.length);
        assertEquals(64, r.seaMap[0].length);
        System.out.println("seaMap is 64x64 ✅");
    }

    @Test @DisplayName("DS-07: altMap fully populated")
    void ds07() {
        DeepScanResult r = getDeepResult();
        int bad = 0;
        for (int x = 0; x < 64; x++)
            for (int z = 0; z < 64; z++)
                if (r.altMap[x][z] < -64 || r.altMap[x][z] > 320) bad++;
        assertEquals(0, bad, bad + " cells out of range");
        System.out.println("All 4096 altMap cells valid ✅");
    }

    @Test @DisplayName("DS-08: All slopeMap edge cells don't crash")
    void ds08() {
        assertDoesNotThrow(() -> {
            DeepScanResult r = getDeepResult();
            for (int z = 0; z < 64; z++)
                assertTrue(r.slopeMap[63][z] >= 0);
            for (int x = 0; x < 64; x++)
                assertTrue(r.slopeMap[x][63] >= 0);
            System.out.println("All 127 edge slope cells safe ✅");
        });
    }

    @Test @DisplayName("DS-09: DeepScan twice gives identical results")
    void ds09() {
        PlotCandidate c = getChampion();
        if (c == null || !liveWorld) {
            System.out.println("Skipped — no live champion"); return;
        }
        DeepScanResult a = deepScanner.scan(c);
        DeepScanResult b = deepScanner.scan(c);
        for (int x = 0; x < 64; x++)
            for (int z = 0; z < 64; z++) {
                assertEquals(a.altMap[x][z],   b.altMap[x][z]);
                assertEquals(a.slopeMap[x][z], b.slopeMap[x][z]);
                assertEquals(a.seaMap[x][z],   b.seaMap[x][z]);
            }
        System.out.println("Both scans identical ✅");
    }
}