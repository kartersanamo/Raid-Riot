package com.kartersanamo.raidriot.item;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EventItemService {

    private final RaidRiotPlugin plugin;
    private BukkitTask scanTask;

    public EventItemService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        scanTask = Bukkit.getScheduler().runTaskTimer(plugin, this::scanOnlinePlayers, 40L, 40L);
    }

    public void stop() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }

    public void markKit(Player player) {
        if (player == null) {
            return;
        }
        String eventWorld = ConfigManager.get().getEventWorld();
        if (eventWorld == null || eventWorld.isEmpty()) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        markStacks(contents, eventWorld);
        inv.setContents(contents);
        ItemStack[] armor = inv.getArmorContents();
        markStacks(armor, eventWorld);
        inv.setArmorContents(armor);
        player.updateInventory();
    }

    public void markStacks(ItemStack[] stacks, String eventWorld) {
        if (stacks == null) {
            return;
        }
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = markStack(stacks[i], eventWorld);
        }
    }

    public ItemStack markStack(ItemStack stack, String eventWorld) {
        if (stack == null || stack.getType() == Material.AIR || eventWorld == null || eventWorld.isEmpty()) {
            return stack;
        }
        ItemStack copy = stack.clone();
        EventItemNbt.mark(copy, UUID.randomUUID(), eventWorld);
        applyEventLore(copy);
        return copy;
    }

    public void unmarkStacks(ItemStack[] stacks) {
        if (stacks == null) {
            return;
        }
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = unmarkStack(stacks[i]);
        }
    }

    public ItemStack unmarkStack(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return stack;
        }
        ItemStack copy = stack.clone();
        EventItemNbt.unmark(copy);
        removeEventLore(copy);
        return copy;
    }

    public boolean isEventItem(ItemStack stack) {
        if (EventItemNbt.isAvailable() && EventItemNbt.isEventItem(stack)) {
            return true;
        }
        return hasEventLore(stack);
    }

    public int purgeCarrier(Player player) {
        return purgeCarrier(player, true);
    }

    public int purgeCarrier(Player player, boolean notify) {
        if (player == null) {
            return 0;
        }
        int removed = 0;
        removed += purgeInventory(player.getInventory(), player.getWorld());
        removed += purgeInventory(player.getEnderChest(), player.getWorld());
        ItemStack cursor = player.getItemOnCursor();
        if (shouldRemove(cursor, player.getWorld())) {
            removed += cursor.getAmount();
            player.setItemOnCursor(null);
        }
        if (removed > 0) {
            player.updateInventory();
            if (notify) {
                notifyRemoved(player);
            }
        }
        return removed;
    }

    public int purgeInventory(Inventory inventory, World locationWorld) {
        if (inventory == null) {
            return 0;
        }
        int removed = 0;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (shouldRemove(stack, locationWorld)) {
                contents[i] = null;
                removed += stack == null ? 0 : stack.getAmount();
            }
        }
        if (removed > 0) {
            inventory.setContents(contents);
        }
        return removed;
    }

    public boolean shouldRemove(ItemStack stack, World locationWorld) {
        if (!isEventItem(stack)) {
            return false;
        }
        String allowedWorld = ConfigManager.get().getEventWorld();
        if (allowedWorld == null || allowedWorld.isEmpty()) {
            return true;
        }
        String itemWorld = EventItemNbt.getWorldName(stack);
        if (itemWorld != null && !allowedWorld.equals(itemWorld)) {
            return true;
        }
        return locationWorld == null || !allowedWorld.equals(locationWorld.getName());
    }

    public World worldFromHolder(InventoryHolder holder) {
        if (holder instanceof Player) {
            return ((Player) holder).getWorld();
        }
        if (holder instanceof org.bukkit.block.DoubleChest) {
            return ((org.bukkit.block.DoubleChest) holder).getWorld();
        }
        if (holder instanceof org.bukkit.block.BlockState) {
            return ((org.bukkit.block.BlockState) holder).getWorld();
        }
        if (holder instanceof org.bukkit.entity.HumanEntity) {
            return ((org.bukkit.entity.HumanEntity) holder).getWorld();
        }
        if (holder instanceof org.bukkit.entity.Entity) {
            return ((org.bukkit.entity.Entity) holder).getWorld();
        }
        return null;
    }

    public void notifyRemoved(Player player) {
        if (player != null && player.isOnline()) {
            ConfigManager.get().send(player, "event-item.deleted");
        }
    }

    private void scanOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            purgeCarrier(player, false);
        }
    }

    private void applyEventLore(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(stack.getType());
        }
        if (meta == null) {
            return;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        removeEventLoreLines(lore);
        lore.add("");
        lore.add(eventLoreLine());
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    private void removeEventLore(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }
        List<String> lore = new ArrayList<>(meta.getLore());
        if (removeEventLoreLines(lore)) {
            if (lore.isEmpty()) {
                meta.setLore(null);
            } else {
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
    }

    private boolean hasEventLore(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta() || !stack.getItemMeta().hasLore()) {
            return false;
        }
        String eventLine = eventLoreLine();
        for (String line : stack.getItemMeta().getLore()) {
            if (eventLine.equals(line)) {
                return true;
            }
        }
        return false;
    }

    private boolean removeEventLoreLines(List<String> lore) {
        boolean changed = false;
        String eventLine = eventLoreLine();
        while (lore.size() >= 2
                && eventLine.equals(lore.get(lore.size() - 1))
                && isBlankLoreLine(lore.get(lore.size() - 2))) {
            lore.remove(lore.size() - 1);
            lore.remove(lore.size() - 1);
            changed = true;
        }
        while (!lore.isEmpty() && eventLine.equals(lore.get(lore.size() - 1))) {
            lore.remove(lore.size() - 1);
            changed = true;
        }
        return changed;
    }

    private String eventLoreLine() {
        return ConfigManager.colorize("&c&lEvent Item");
    }

    private boolean isBlankLoreLine(String line) {
        return line == null || line.isEmpty() || line.trim().isEmpty();
    }
}
