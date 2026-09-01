package ukc.gdmc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around the GDMC HTTP Interface (localhost:9000).
 *
 * This class hides all HTTP and JSON handling and exposes
 * simple Java methods for:
 *  - reading blocks
 *  - placing blocks (in batches)
 *  - working in build-area local coordinates
 *
 * Builder code should ONLY talk to this class, never directly to HTTP.
 */
public class MinecraftApi {

    /** Single source of truth for the GDMC HTTP Interface address. */
    public static final String SERVER_URL = "http://localhost:9000";

    /** Base URL "http://localhost:9000" */
    private final String base;

    /** Java HTTP client for sending requests */
    private final HttpClient http = HttpClient.newHttpClient();

    /** Jackson mapper for JSON <-> Java object conversion */
    private final ObjectMapper mapper = new ObjectMapper();

    /** Cached build area so we can use local (dx,dy,dz) coordinates */
    private BuildArea cachedArea;

    /** Timeout for HTTP requests */
    private final Duration timeout = Duration.ofSeconds(10);

    /** Max number of blocks per PUT request (prevents huge payloads) */
    private final int batchSize = 5000;

    /** Create API wrapper with base URL */
    public MinecraftApi(String baseUrl) {
        this.base = baseUrl;
    }

    //
    // Build area
    //

    /**
     * GET /buildarea
     * Reads the current build area set in Minecraft using /setbuildarea.
     */
    public BuildArea getBuildArea() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/buildarea"))
                .timeout(timeout)
                .GET()
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException(res.body());

        // Convert JSON -> BuildArea object
        return mapper.readValue(res.body(), BuildArea.class);
    }

    /**
     * Refresh and cache the build area.
     * Call this once at program start (and whenever build area changes).
     */
    public BuildArea refreshBuildArea() throws Exception {
        this.cachedArea = getBuildArea();
        System.out.printf(
                "BuildArea: (%d,%d,%d) -> (%d,%d,%d)%n",
                cachedArea.xFrom, cachedArea.yFrom, cachedArea.zFrom,
                cachedArea.xTo, cachedArea.yTo, cachedArea.zTo
        );
        return this.cachedArea;
    }

    /** Ensure build area is available before using local coordinates */
    private BuildArea ensureBuildArea() throws Exception {
        if (cachedArea == null) refreshBuildArea();
        return cachedArea;
    }

    //
    // Reads
    //

    /**
     * GET /blocks
     * Reads a rectangular region of blocks using world coordinates.
     */
    public List<Block> getBlocks(int x, int y, int z, int dx, int dy, int dz) throws Exception {
        String url = base + "/blocks?x=" + x + "&y=" + y + "&z=" + z
                + "&dx=" + dx + "&dy=" + dy + "&dz=" + dz;

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .GET()
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException(res.body());

        // JSON array -> List<Block>
        return mapper.readValue(res.body(), new TypeReference<>() {});
    }

    /**
     * Read ONE block using local build-area coordinates (dx,dy,dz).
     */
    public Block getBlockInBuildArea(int dx, int dy, int dz) throws Exception {
        BuildArea a = ensureBuildArea();

        // Convert local coords -> world coords
        return getBlocks(a.xFrom + dx, a.yFrom + dy, a.zFrom + dz, 1, 1, 1).get(0);
    }


    /**
     * Place ONE block using local build-area coordinates.
     * Block ID may include states (e.g. oak_stairs[facing=north]).
     */
    public void setBlockInBuildArea(int dx, int dy, int dz, String id) throws Exception {
        setBlocksInBuildArea(List.of(new PlacedBlock(dx, dy, dz, id)));
    }

    //
    // Batch placement
    //

    /**
     * Place MANY blocks in one or more PUT /blocks requests.
     * This is the main method builders should use.
     */
    public void setBlocksInBuildArea(List<PlacedBlock> blocks) throws Exception {
        if (blocks == null || blocks.isEmpty()) return;

        BuildArea a = ensureBuildArea();

        // Origin for the request (world coordinates)
        String url = base + "/blocks";

        // Send blocks in chunks to avoid huge JSON payloads
        for (int i = 0; i < blocks.size(); i += batchSize) {
            int end = Math.min(blocks.size(), i + batchSize);
            List<PlacedBlock> chunk = blocks.subList(i, end);

            // Convert local coordinates -> world coordinates
            List<PutBlock> payload = new ArrayList<>(chunk.size());
            for (PlacedBlock b : chunk) {
                payload.add(new PutBlock(
                        a.xFrom + b.dx,
                        a.yFrom + b.dy,
                        a.zFrom + b.dz,
                        b.id
                ));
            }

            // Convert payload to JSON
            String body = mapper.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new RuntimeException(res.body());
        }
    }

    // Place blocks using direct world coordinates — no build area offset applied
    public void setBlocksWorld(List<PutBlock> blocks) throws Exception {
        if (blocks == null || blocks.isEmpty()) return;

        String url = base + "/blocks";

        for (int i = 0; i < blocks.size(); i += batchSize) {
            int end = Math.min(blocks.size(), i + batchSize);
            List<PutBlock> chunk = blocks.subList(i, end);

            String body = mapper.writeValueAsString(chunk);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new RuntimeException(res.body());
        }
    }


    //
    // Fill helper
    //

    /**
     * Fill a rectangular volume using local build-area coordinates.
     * Bounds are inclusive.
     */
    public void fillInBuildArea(int x1, int y1, int z1,
                                int x2, int y2, int z2,
                                String id) throws Exception {

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        // Generate all blocks to place
        List<PlacedBlock> blocks = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blocks.add(new PlacedBlock(x, y, z, id));
                }
            }
        }

        // Place them in batches
        setBlocksInBuildArea(blocks);
    }

    //
    // Models (JSON mapping)
    //

    /** Model for GET /buildarea response */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BuildArea {
        public int xFrom, yFrom, zFrom, xTo, yTo, zTo;
    }

    /** Model for GET /blocks response */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Block {
        public String id;
        public int x, y, z;
    }

    /** Local-coordinate block placement input */
    public static class PlacedBlock {
        public int dx, dy, dz;
        public String id;

        public PlacedBlock(int dx, int dy, int dz, String id) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.id = id;
        }
    }

    /** Internal model for PUT /blocks payload (world coords) */
    public static class PutBlock {
        public int x, y, z;
        public String id;

        public PutBlock(int x, int y, int z, String id) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.id = id;
        }
    }

    /**  Send minecraft commands through gdmc interface, used here to teleport the player after placement */
    public void runCommand(String command) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/command"))
                .timeout(timeout)
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(command))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException(res.body());
    }

    /*
     * GET /biomes
     * Returns the biome at a given world coordinate.
     * Used by ScanPhase to reject unsuitable biomes before plot selection.
     */
    public String getBiome(int x, int y, int z) throws Exception {
        String url = base + "/biomes?x=" + x + "&y=" + y + "&z=" + z + "&dx=1&dy=1&dz=1";

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .GET()
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException(res.body());

        return res.body();
    }
}