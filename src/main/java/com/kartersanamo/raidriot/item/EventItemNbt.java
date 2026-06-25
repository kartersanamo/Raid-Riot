package com.kartersanamo.raidriot.item;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists Raid Riot event-item metadata in NMS NBT (Spigot 1.8.x).
 * Uses flat string tags on the item root compound: event UUID + allowed world.
 */
final class EventItemNbt {

    private static final String ID_KEY = "RaidRiotEventId";
    private static final String WORLD_KEY = "RaidRiotEventWorld";

    private static final boolean AVAILABLE;
    private static final Method AS_NMS_COPY;
    private static final Method AS_BUKKIT_COPY;
    private static final Method HAS_TAG;
    private static final Method GET_TAG;
    private static final Method SET_TAG;
    private static final Method COMPOUND_HAS_KEY;
    private static final Method COMPOUND_SET_STRING;
    private static final Method COMPOUND_REMOVE;
    private static final Method COMPOUND_GET_STRING;
    private static final Class<?> NBT_COMPOUND_CLASS;

    static {
        Method asNmsCopy = null;
        Method asBukkitCopy = null;
        Method hasTag = null;
        Method getTag = null;
        Method setTag = null;
        Method compoundHasKey = null;
        Method compoundSetString = null;
        Method compoundRemove = null;
        Method compoundGetString = null;
        Class<?> nbtCompoundClass = null;
        boolean available = false;

        try {
            String version = Bukkit.getServer().getClass().getPackage().getName();
            version = version.substring(version.lastIndexOf('.') + 1);

            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
            Class<?> nmsItemStack = Class.forName("net.minecraft.server." + version + ".ItemStack");
            nbtCompoundClass = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");

            asNmsCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", nmsItemStack);
            hasTag = nmsItemStack.getMethod("hasTag");
            getTag = nmsItemStack.getMethod("getTag");
            setTag = nmsItemStack.getMethod("setTag", nbtCompoundClass);
            compoundHasKey = nbtCompoundClass.getMethod("hasKey", String.class);
            compoundSetString = nbtCompoundClass.getMethod("setString", String.class, String.class);
            compoundRemove = nbtCompoundClass.getMethod("remove", String.class);
            compoundGetString = nbtCompoundClass.getMethod("getString", String.class);
            available = true;
        } catch (ReflectiveOperationException ex) {
            Logger.getLogger("RaidRiot").log(Level.SEVERE,
                    "Event item NBT is unavailable on this server version; event items cannot be secured.", ex);
        }

        AS_NMS_COPY = asNmsCopy;
        AS_BUKKIT_COPY = asBukkitCopy;
        HAS_TAG = hasTag;
        GET_TAG = getTag;
        SET_TAG = setTag;
        COMPOUND_HAS_KEY = compoundHasKey;
        COMPOUND_SET_STRING = compoundSetString;
        COMPOUND_REMOVE = compoundRemove;
        COMPOUND_GET_STRING = compoundGetString;
        NBT_COMPOUND_CLASS = nbtCompoundClass;
        AVAILABLE = available;
    }

    private EventItemNbt() {
    }

    static boolean isAvailable() {
        return AVAILABLE;
    }

    static void mark(ItemStack stack, UUID itemId, String worldName) {
        if (!AVAILABLE || stack == null || itemId == null || worldName == null || worldName.isEmpty()) {
            return;
        }
        try {
            Object nms = AS_NMS_COPY.invoke(null, stack);
            Object root = readOrCreateRoot(nms);
            COMPOUND_SET_STRING.invoke(root, ID_KEY, itemId.toString());
            COMPOUND_SET_STRING.invoke(root, WORLD_KEY, worldName);
            SET_TAG.invoke(nms, root);
            copyBack(nms, stack);
        } catch (ReflectiveOperationException ex) {
            Logger.getLogger("RaidRiot").log(Level.WARNING, "Failed to mark event item NBT", ex);
        }
    }

    static void unmark(ItemStack stack) {
        if (!AVAILABLE || stack == null) {
            return;
        }
        try {
            Object nms = AS_NMS_COPY.invoke(null, stack);
            if (!(Boolean) HAS_TAG.invoke(nms)) {
                return;
            }
            Object root = GET_TAG.invoke(nms);
            boolean changed = false;
            if ((Boolean) COMPOUND_HAS_KEY.invoke(root, ID_KEY)) {
                COMPOUND_REMOVE.invoke(root, ID_KEY);
                changed = true;
            }
            if ((Boolean) COMPOUND_HAS_KEY.invoke(root, WORLD_KEY)) {
                COMPOUND_REMOVE.invoke(root, WORLD_KEY);
                changed = true;
            }
            if (!changed) {
                return;
            }
            if (isEmptyCompound(root)) {
                SET_TAG.invoke(nms, null);
            } else {
                SET_TAG.invoke(nms, root);
            }
            copyBack(nms, stack);
        } catch (ReflectiveOperationException ex) {
            Logger.getLogger("RaidRiot").log(Level.WARNING, "Failed to unmark event item NBT", ex);
        }
    }

    static boolean isEventItem(ItemStack stack) {
        return getItemId(stack) != null && getWorldName(stack) != null;
    }

    static UUID getItemId(ItemStack stack) {
        String raw = readString(stack, ID_KEY);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    static String getWorldName(ItemStack stack) {
        String world = readString(stack, WORLD_KEY);
        if (world == null || world.isEmpty()) {
            return null;
        }
        return getItemId(stack) == null ? null : world;
    }

    private static String readString(ItemStack stack, String key) {
        if (!AVAILABLE || stack == null) {
            return null;
        }
        try {
            Object nms = AS_NMS_COPY.invoke(null, stack);
            if (!(Boolean) HAS_TAG.invoke(nms)) {
                return null;
            }
            Object root = GET_TAG.invoke(nms);
            if (!(Boolean) COMPOUND_HAS_KEY.invoke(root, key)) {
                return null;
            }
            return (String) COMPOUND_GET_STRING.invoke(root, key);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static void copyBack(Object nms, ItemStack stack) throws ReflectiveOperationException {
        ItemStack updated = (ItemStack) AS_BUKKIT_COPY.invoke(null, nms);
        stack.setType(updated.getType());
        stack.setAmount(updated.getAmount());
        stack.setDurability(updated.getDurability());
        stack.setItemMeta(updated.getItemMeta());
    }

    private static Object readOrCreateRoot(Object nms) throws ReflectiveOperationException {
        if ((Boolean) HAS_TAG.invoke(nms)) {
            return GET_TAG.invoke(nms);
        }
        return NBT_COMPOUND_CLASS.newInstance();
    }

    private static boolean isEmptyCompound(Object compound) {
        return compound == null || compound.toString().isEmpty() || "{}".equals(compound.toString());
    }
}
