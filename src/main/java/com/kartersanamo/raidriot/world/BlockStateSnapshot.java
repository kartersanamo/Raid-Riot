package com.kartersanamo.raidriot.world;

import org.bukkit.Material;
import org.bukkit.block.Block;

final class BlockStateSnapshot {

    private final Material type;
    private final byte data;

    BlockStateSnapshot(Material type, byte data) {
        this.type = type;
        this.data = data;
    }

    static BlockStateSnapshot capture(Block block) {
        return new BlockStateSnapshot(block.getType(), block.getData());
    }

    void apply(Block block) {
        block.setType(type);
        block.setData(data);
    }
}
