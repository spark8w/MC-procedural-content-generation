package ukc.gdmc;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.querz.nbt.tag.IntTag;

public class StructureLoader {



    // Clean data structure to hold the results
    public static class LoadedStructure {
        public List<BlockData> blocks = new ArrayList<>();
        public int[] markerPosition = null;
        public int[] EndMarkerPosition = null;
        public int width;
        public int height;
        public int length;
    }

    public static class BlockData {
        public int x, y, z;
        public String type;

        public BlockData(int x, int y, int z, String type) {
            this.x = x; this.y = y; this.z = z; this.type = type;
        }
    }

    public static LoadedStructure loadStructure(String path) throws Exception {
        // 1. Load the .nbt file using Querz
        NamedTag namedTag = NBTUtil.read(new File(path));
        CompoundTag root = (CompoundTag) namedTag.getTag();

        // 2. Get the palette (block types)
        ListTag<CompoundTag> palette = root.getListTag("palette").asCompoundTagList();

        // 3. Get the blocks (positions + state)
        ListTag<CompoundTag> blocksNBT = root.getListTag("blocks").asCompoundTagList();

        LoadedStructure result = new LoadedStructure();

        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;

        for (CompoundTag blockTag : blocksNBT) {
            // Get relative position [x, y, z]
            ListTag<?> posTag = blockTag.getListTag("pos");
            int x = ((IntTag) posTag.get(0)).asInt();
            int y = ((IntTag) posTag.get(1)).asInt();
            int z = ((IntTag) posTag.get(2)).asInt();

            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;

            // 4. Map state index to block type name + properties
            int stateIndex = blockTag.getInt("state");
            CompoundTag paletteEntry = palette.get(stateIndex);
            String blockName = paletteEntry.getString("Name");

            // Read block state properties (facing, half, shape, waterlogged etc)
            // Without this, stairs/slabs/doors all place in the wrong direction
            String blockType = blockName;
            if (paletteEntry.containsKey("Properties")) {
                CompoundTag properties = paletteEntry.getCompoundTag("Properties");

                StringBuilder props = new StringBuilder("[");
                boolean first = true;
                for (String key : properties.keySet()) {
                    if (!first) props.append(",");
                    props.append(key).append("=").append(properties.getString(key));
                    first = false;
                }
                props.append("]");
                blockType = blockName + props;
            }
            // 5. Find the marker block (using 'sponge' as the common marker)
            if (blockType.contains("waxed_exposed_copper_door")) {
                result.markerPosition = new int[]{x, y, z};
            }

            // Detect copper stair as town hall specific marker
            if (blockType.contains("deepslate_brick_stairs")) {
                result.EndMarkerPosition = new int[]{x, y, z};
            }


            // 6. Add to clean structure
            result.blocks.add(new BlockData(x, y, z, blockType));
        }

        result.width = maxX + 1;
        result.height = maxY + 1;
        result.length = maxZ + 1;

        return result;
    }
}