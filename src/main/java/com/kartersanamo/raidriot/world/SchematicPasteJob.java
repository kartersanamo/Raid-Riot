package com.kartersanamo.raidriot.world;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SchematicPasteJob {

    private final World world;
    private final CuboidClipboard clipboard;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final int width;
    private final int height;
    private final int length;
    private final byte teamWoolData;

    private int indexX;
    private int indexY;
    private int indexZ;
    private boolean started;

    public SchematicPasteJob(World world, CuboidClipboard clipboard, int originX, int originY, int originZ,
            byte teamWoolData) {
        this.world = world;
        this.clipboard = clipboard;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.teamWoolData = teamWoolData;
        this.width = clipboard.getWidth();
        this.height = clipboard.getHeight();
        this.length = clipboard.getLength();
    }

    public static SchematicPasteJob fromClipboard(World world, CuboidClipboard clipboard,
            int originX, int originY, int originZ, byte teamWoolData) {
        return new SchematicPasteJob(world, clipboard, originX, originY, originZ, teamWoolData);
    }

    public int pasteBatch(int maxBlocks) {
        if (maxBlocks <= 0) {
            return 0;
        }
        started = true;
        int pasted = 0;
        while (pasted < maxBlocks && !isComplete()) {
            BaseBlock block;
            try {
                block = clipboard.getBlock(new Vector(indexX, indexY, indexZ));
            } catch (ArrayIndexOutOfBoundsException ex) {
                advance();
                continue;
            }
            if (block != null && !block.isAir()) {
                Material material = Material.getMaterial(block.getId());
                if (material != null && material != Material.AIR) {
                    Block target = world.getBlockAt(originX + indexX, originY + indexY, originZ + indexZ);
                    byte data = (byte) block.getData();
                    if (material == Material.WOOL) {
                        data = teamWoolData;
                    }
                    target.setType(material);
                    target.setData(data);
                    pasted++;
                }
            }
            advance();
        }
        return pasted;
    }

    public boolean isComplete() {
        return started && indexX >= width;
    }

    private void advance() {
        indexZ++;
        if (indexZ >= length) {
            indexZ = 0;
            indexY++;
            if (indexY >= height) {
                indexY = 0;
                indexX++;
            }
        }
    }
}
