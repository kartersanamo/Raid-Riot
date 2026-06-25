package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.item.EventItemService;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class EventItemListener implements Listener {

    private final RaidRiotPlugin plugin;
    private final EventItemService eventItemService;

    public EventItemListener(RaidRiotPlugin plugin, EventItemService eventItemService) {
        this.plugin = plugin;
        this.eventItemService = eventItemService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        scheduleCarrierPurge(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        scheduleCarrierPurge(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        scheduleCarrierPurge(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        scheduleCarrierPurge(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        scheduleCarrierPurge((Player) event.getWhoClicked());
        scheduleInventoryPurge(event.getView().getTopInventory());
        scheduleInventoryPurge(event.getView().getBottomInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        scheduleCarrierPurge(player);
        scheduleInventoryPurge(event.getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        ItemStack stack = event.getItem();
        if (!eventItemService.isEventItem(stack)) {
            return;
        }
        World destinationWorld = eventItemService.worldFromHolder(event.getDestination().getHolder());
        if (eventItemService.shouldRemove(stack, destinationWorld)) {
            event.setCancelled(true);
            purgeIllegalItemEntity(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item entity = event.getEntity();
        ItemStack stack = entity.getItemStack();
        if (eventItemService.shouldRemove(stack, entity.getWorld())) {
            entity.remove();
        }
    }

    private void scheduleCarrierPurge(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> eventItemService.purgeCarrier(player));
    }

    private void scheduleInventoryPurge(Inventory inventory) {
        if (inventory == null || inventory.getType() == InventoryType.PLAYER) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            InventoryHolder holder = inventory.getHolder();
            World world = eventItemService.worldFromHolder(holder);
            int removed = eventItemService.purgeInventory(inventory, world);
            if (removed > 0 && holder instanceof Player) {
                eventItemService.notifyRemoved((Player) holder);
            }
        });
    }

    private void purgeIllegalItemEntity(InventoryMoveItemEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            eventItemService.purgeInventory(event.getSource(), eventItemService.worldFromHolder(
                    event.getSource().getHolder()));
            eventItemService.purgeInventory(event.getDestination(), eventItemService.worldFromHolder(
                    event.getDestination().getHolder()));
        });
    }
}
