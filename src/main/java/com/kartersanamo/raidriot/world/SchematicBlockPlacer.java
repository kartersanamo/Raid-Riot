package com.kartersanamo.raidriot.world;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.IOException;

public final class SchematicBlockPlacer {

    private SchematicBlockPlacer() {
    }

    public static EditSession createSession(World world) throws IOException {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (!(plugin instanceof WorldEditPlugin)) {
            throw new IOException("WorldEdit must be installed to paste bases.");
        }
        WorldEditPlugin we = (WorldEditPlugin) plugin;
        EditSession session = we.getWorldEdit().getEditSessionFactory()
                .getEditSession(BukkitUtil.getLocalWorld(world), -1);
        session.setFastMode(false);
        return session;
    }

    public static void pasteAt(World world, CuboidClipboard clip, int originX, int originY, int originZ)
            throws IOException, MaxChangedBlocksException {
        EditSession session = createSession(world);
        int w = clip.getWidth();
        int h = clip.getHeight();
        int len = clip.getLength();
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < len; z++) {
                for (int y = 0; y < h; y++) {
                    BaseBlock bb;
                    try {
                        bb = clip.getBlock(new Vector(x, y, z));
                    } catch (Exception ex) {
                        continue;
                    }
                    if (bb == null || bb.isAir()) {
                        continue;
                    }
                    session.setBlock(new Vector(originX + x, originY + y, originZ + z), bb);
                }
            }
        }
        session.flushQueue();
    }
}
