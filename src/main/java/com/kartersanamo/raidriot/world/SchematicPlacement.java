package com.kartersanamo.raidriot.world;

import com.sk89q.worldedit.CuboidClipboard;

public final class SchematicPlacement {

    private SchematicPlacement() {
    }

    public static int lowestSolidY(CuboidClipboard clip) {
        return SchematicAnalysis.analyze(clip).lowestNonAirY;
    }
}
