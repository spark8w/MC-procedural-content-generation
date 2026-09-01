package ukc.gdmc;

public class ApiTest {
    public static void main(String[] args) throws Exception {
        MinecraftApi api = new MinecraftApi("http://localhost:9000");

        api.refreshBuildArea();

        // Clear small area first (a bit larger so reruns don't leave leftovers)
        api.fillInBuildArea(0, 0, 0, 12, 8, 12, "minecraft:air");

        // Visual marker for local origin (0,0,0)
        api.setBlockInBuildArea(0, 0, 0, "minecraft:red_concrete");

        // Floor
        api.fillInBuildArea(0, 0, 0, 10, 0, 10, "minecraft:stone");

        // Walls
        api.fillInBuildArea(0, 1, 0, 10, 5, 0, "minecraft:oak_planks");
        api.fillInBuildArea(0, 1, 10, 10, 5, 10, "minecraft:oak_planks");
        api.fillInBuildArea(0, 1, 0, 0, 5, 10, "minecraft:oak_planks");
        api.fillInBuildArea(10, 1, 0, 10, 5, 10, "minecraft:oak_planks");

        // Roof
        api.fillInBuildArea(0, 6, 0, 10, 6, 10, "minecraft:cobblestone");

        // Door opening
        api.fillInBuildArea(5, 1, 0, 5, 2, 0, "minecraft:air");

        // Windows
        api.fillInBuildArea(2, 2, 10, 3, 3, 10, "minecraft:glass");
        api.fillInBuildArea(7, 2, 10, 8, 3, 10, "minecraft:glass");

        // Read block test
        var block = api.getBlockInBuildArea(5, 0, 5);
        System.out.println("Block at center: " + block.id);


        ApiTest.class.getResourceAsStream("/structures/house.nbt");
    }
}