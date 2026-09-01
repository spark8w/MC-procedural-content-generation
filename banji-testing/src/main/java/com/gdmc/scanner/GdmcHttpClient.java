package com.gdmc.scanner;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GdmcHttpClient {

    private static final String BASE_URL   = "http://localhost:9000";
    private static final int    TIMEOUT_MS = 8000;
    private static final int    WORLD_BOTTOM = -64;
    private static final int    WORLD_HEIGHT = 384;

    public boolean isConnected() {
        try {
            String response = get("/version");return response != null && !response.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public String getVersion() throws Exception {
        return get("/version");
    }

    public String getBlocks(int x, int y, int z,
                            int dx, int dy, int dz) throws Exception {
        String path = String.format(
                "/blocks?x=%d&y=%d&z=%d&dx=%d&dy=%d&dz=%d",
                x, y, z, dx, dy, dz
        );
        return get(path);
    }

    public String getColumn(int x, int z) throws Exception {
        return getBlocks(x, WORLD_BOTTOM, z, 1, WORLD_HEIGHT, 1);
    }

    public void placeBlock(int x, int y, int z,
                           String blockId) throws Exception {
        String path = String.format("/blocks?x=%d&y=%d&z=%d", x, y, z);
        put(path, blockId);
    }

    public void markPlotBorder(int north, int south,
                               int east, int west, int y) throws Exception {
        System.out.println("[GDMC] Marking champion plot...");
        for (int x = west; x <= east; x++) {
            placeBlock(x, y, north, "minecraft:gold_block");
            placeBlock(x, y, south, "minecraft:gold_block");
        }
        for (int z = north; z <= south; z++) {
            placeBlock(west, y, z, "minecraft:gold_block");
            placeBlock(east, y, z, "minecraft:gold_block");
        }
        placeBlock(west, y + 1, north, "minecraft:beacon");
        System.out.println("[GDMC] Done! Look for gold border in-game.");
    }

    public String runCommand(String command) throws Exception {
        return post("/commands", command);
    }

    private String get(String path) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        int code = conn.getResponseCode();
        if (code != 200)
            throw new RuntimeException("GDMC GET failed: " + code);
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private void put(String path, String body) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        conn.disconnect();
    }

    private String post(String path, String body) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }
}