<<<<<<< README.md
MinecraftApi – GDMC Wrapper

This class is our single interface to Minecraft.
It wraps the GDMC HTTP Interface and hides:

-HTTP requests
-JSON formatting
-World coordinate math

Builder code should ONLY use this API and never talk to HTTP directly.


1. What this API provides:

    -Read blocks from the world

    -Place blocks (single or batch)

    -Fill volumes (floors, walls, boxes)

    -Work in local build-area coordinates


    Instead of using world coordinates like: x = 2353, y = 63, z = -78

    We use: (dx, dy, dz)

    Where: (0,0,0) = corner of the build area




2. Setup In Minecraft: Make sure the GDMC HTTP mod is running and set a build area:

    
    /setbuildarea xFrom yFrom zFrom xTo yTo zTo


In Java:

    MinecraftApi api = new MinecraftApi("http://localhost:9000");

    api.refreshBuildArea();

You must call refreshBuildArea() once at the start.



3. Coordinate system (IMPORTANT)

    All methods use local build-area coordinates: (dx, dy, dz)

    These are converted internally to world coordinates:

        worldX = xFrom + dx
        worldY = yFrom + dy
        worldZ = zFrom + dz


So:

    api.setBlockInBuildArea(0,0,0,"minecraft:stone");

Places a block at the corner of the build area, not at world (0,0,0).


4. Place one block

        api.setBlockInBuildArea(5, 1, 5, "minecraft:stone");

    Block IDs may include states:

        "minecraft:oak_stairs[facing=north,half=bottom]"



5. Place many blocks (batch placement)

        List<MinecraftApi.PlacedBlock> blocks = new ArrayList<>();

        blocks.add(new MinecraftApi.PlacedBlock(0,0,0,"minecraft:stone"));
        blocks.add(new MinecraftApi.PlacedBlock(1,0,0,"minecraft:stone"));
        blocks.add(new MinecraftApi.PlacedBlock(2,0,0,"minecraft:stone"));

        api.setBlocksInBuildArea(blocks);


This is fast because all blocks are sent in one request (or small chunks).



6. Fill a box (floors, walls, volumes)

        api.fillInBuildArea(0,0,0, 9,0,9, "minecraft:oak_planks");

This creates a 10×1×10 floor.



7. Read a block
    
        var block = api.getBlockInBuildArea(3,0,5);
        System.out.println(block.id);


open -a TextEdit README.md

## Testing

The terrain scanner module was tested by Banji Sule (Kent ID: 22039017).

Test file: src/main/java/com/gdmc/tester/GDMCTesterBanji.java
Full test suite: banji-scanner-tests branch / banji-testing/ folder

Results:
- Gradle build: 58 tests, 58 PASS, 0 FAIL
- Maven build:  17 tests, 17 PASS, 0 FAIL
- Combined:     75 tests, 100% pass rate
- Environment:  Live Minecraft 1.21.11 / GDMC HTTP Interface 1.8.4

Documentation: Documentation/GDMC_Test_Results_Banji_Sule.docx