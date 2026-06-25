package com.kartersanamo.raidriot.world;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;

import org.bukkit.World;

import java.io.IOException;

public final class SchematicPasteJob {

    private final World world;
    private final CuboidClipboard clipboard;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final int width;
    private final int height;
    private final int length;

    private EditSession session;
    private int indexX;
    private int indexY;
    private int indexZ;
    private boolean started;

    public SchematicPasteJob(World world, CuboidClipboard clipboard, int originX, int originY, int originZ) {
        this.world = world;
        this.clipboard = clipboard;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.width = clipboard.getWidth();
        this.height = clipboard.getHeight();
        this.length = clipboard.getLength();
    }

    public static SchematicPasteJob fromClipboard(World world, CuboidClipboard clipboard,
            int originX, int originY, int originZ) {
        return new SchematicPasteJob(world, clipboard, originX, originY, originZ);
    }

    public int pasteBatch(int maxBlocks) throws IOException, MaxChangedBlocksException {
        if (maxBlocks <= 0) {
            return 0;
        }
        if (session == null) {
            session = SchematicBlockPlacer.createSession(world);
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
                session.setBlock(new Vector(originX + indexX, originY + indexY, originZ + indexZ), block);
                pasted++;
            }
            advance();
        }
        session.flushQueue();
        return pasted;
    }

    public boolean isComplete() {
        return started && indexX >= width;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean hasEditSession() {
        return session != null;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getOriginZ() {
        return originZ;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public int getScanProgressPercent() {
        int total = width * height * length;
        if (total <= 0) {
            return 0;
        }
        if (isComplete()) {
            return 100;
        }
        if (!started) {
            return 0;
        }
        int scanned = indexX * height * length + indexY * length + indexZ;
        return Math.min(99, (int) ((scanned * 100L) / total));
    }

    public void appendStatus(java.util.List<String> lines, String indent) {
        lines.add(indent + "engine: WorldEdit EditSession (fastMode=false, flush/tick)");
        lines.add(indent + "session: " + (session != null ? "open" : "not created"));
        lines.add(indent + "clipboard: " + width + " x " + height + " x " + length);
        lines.add(indent + "origin: " + originX + ", " + originY + ", " + originZ);
        lines.add(indent + "volume scan: " + getScanProgressPercent() + "%"
                + (isComplete() ? " (done)" : (started ? " (pasting)" : " (pending)")));
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
