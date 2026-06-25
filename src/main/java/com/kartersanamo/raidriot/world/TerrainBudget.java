package com.kartersanamo.raidriot.world;

public final class TerrainBudget {

    public int blocks;
    public int chunks;
    public int columns;

    public TerrainBudget(int blocks, int chunks, int columns) {
        this.blocks = blocks;
        this.chunks = chunks;
        this.columns = columns;
    }

    public TerrainBudget half() {
        return new TerrainBudget(Math.max(0, blocks / 2), Math.max(0, chunks / 2), Math.max(0, columns / 2));
    }
}
