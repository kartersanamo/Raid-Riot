package com.kartersanamo.raidriot.world;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SchematicCatalog {

    private SchematicCatalog() {
    }

    public static List<String> listSchematicFiles(File dataFolder) {
        File directory = new File(dataFolder, "schematics");
        if (!directory.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<String>();
        for (File file : files) {
            if (file.isFile() && !file.getName().startsWith(".")) {
                names.add(file.getName());
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }
}
