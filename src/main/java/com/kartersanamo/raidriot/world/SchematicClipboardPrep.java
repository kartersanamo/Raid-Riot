package com.kartersanamo.raidriot.world;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import org.bukkit.Material;

public final class SchematicClipboardPrep {

    private static final int WOOL_BLOCK_ID = Material.WOOL.getId();

    private SchematicClipboardPrep() {
    }

    public static void applyTeamWool(CuboidClipboard clipboard, byte teamWoolData) {
        int width = clipboard.getWidth();
        int height = clipboard.getHeight();
        int length = clipboard.getLength();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    BaseBlock block;
                    try {
                        block = clipboard.getBlock(new Vector(x, y, z));
                    } catch (Exception ex) {
                        continue;
                    }
                    if (block != null && block.getId() == WOOL_BLOCK_ID) {
                        clipboard.setBlock(new Vector(x, y, z), new BaseBlock(WOOL_BLOCK_ID, teamWoolData));
                    }
                }
            }
        }
    }
}
