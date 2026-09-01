package ukc.gdmc;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;

// ============================================================
//  ukc.gdmc.LiveWorldReader.java
//  Reads REAL data from your Minecraft world live
//  Make sure Minecraft is open before running this!
// ============================================================
public class LiveWorldReader {

    // The address of the GDMC HTTP server inside Minecraft
    static final String BASE_URL = "http://localhost:9000";

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  GDMC LIVE WORLD READER");
        System.out.println("  Reading real data from Minecraft...");
        System.out.println("==============================================\n");

        // Step 1 — Check connection
        checkConnection();

        // Step 2 — Read the build area
        readBuildArea();

        // Step 3 — Read real block heights
        readHeightMap();
    }

    // -------------------------------------------------------
    // CHECK CONNECTION
    // -------------------------------------------------------
    static void checkConnection() {
        System.out.println("[1] Checking connection to Minecraft...");
        try {
            String response = get("/version");
            System.out.println("    Connected! Minecraft version: " + response);
        } catch (Exception e) {
            System.out.println("    FAILED — Is Minecraft running?");
            System.exit(1);
        }
    }

    // -------------------------------------------------------
    // READ BUILD AREA
    // Shows the coordinates you set with /setbuildarea
    // -------------------------------------------------------
    static void readBuildArea() {
        System.out.println("\n[2] Reading build area from Minecraft...");
        try {
            String response = get("/buildarea");
            System.out.println("    Build area: " + response);
        } catch (Exception e) {
            System.out.println("    Could not read build area: " + e.getMessage());
        }
    }

    // -------------------------------------------------------
    // READ REAL BLOCK HEIGHTS
    // Gets the Y height of real blocks in your Minecraft world
    // -------------------------------------------------------
    static void readHeightMap() {
        System.out.println("\n[3] Reading real block heights from your world...");
        System.out.println("    Sampling 5x5 grid of heights:\n");

        System.out.printf("    %-6s %-6s %-10s%n", "X", "Z", "Height Y");
        System.out.println("    ----------------------");

        try {
            // Sample a 5x5 grid of points in the build area
            for (int x = 0; x <= 60; x += 15) {
                for (int z = 0; z <= 60; z += 15) {
                    // Ask Minecraft for the highest block at this X,Z
                    String url = "/blocks?x=" + x + "&y=255&z=" + z
                            + "&dx=1&dy=1&dz=1";
                    String block = get(url);
                    System.out.printf("    %-6d %-6d %-10s%n",
                            x, z, block.trim());
                }
            }
        } catch (Exception e) {
            System.out.println("    Error reading blocks: " + e.getMessage());
        }

        System.out.println("\n    These are REAL blocks from your Minecraft world!");
    }

    // -------------------------------------------------------
    // HELPER: sends a GET request to the GDMC server
    // Returns the response as a String
    // -------------------------------------------------------
    static String get(String endpoint) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestMethod("GET");
        InputStream stream = conn.getInputStream();
        String response = new String(stream.readAllBytes());
        conn.disconnect();
        return response;
    }
}