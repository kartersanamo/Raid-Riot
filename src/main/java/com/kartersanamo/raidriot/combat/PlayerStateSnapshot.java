package com.kartersanamo.raidriot.combat;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PlayerStateSnapshot {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack[] enderChest;
    private final int level;
    private final float exp;
    private final double health;
    private final int food;
    private final float saturation;
    private final GameMode gameMode;
    private final Location location;
    private final Collection<PotionEffect> effects;

    private PlayerStateSnapshot(ItemStack[] contents, ItemStack[] armor, ItemStack[] enderChest, int level,
            float exp, double health, int food, float saturation, GameMode gameMode, Location location,
            Collection<PotionEffect> effects) {
        this.contents = cloneArray(contents);
        this.armor = cloneArray(armor);
        this.enderChest = cloneArray(enderChest);
        this.level = level;
        this.exp = exp;
        this.health = health;
        this.food = food;
        this.saturation = saturation;
        this.gameMode = gameMode;
        this.location = location == null ? null : location.clone();
        this.effects = new ArrayList<>(effects);
    }

    public static PlayerStateSnapshot capture(Player player) {
        List<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());
        return new PlayerStateSnapshot(
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getEnderChest().getContents(),
                player.getLevel(),
                player.getExp(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getGameMode(),
                player.getLocation(),
                effects);
    }

    public void apply(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getEnderChest().clear();
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.getInventory().setContents(cloneArray(contents));
        player.getInventory().setArmorContents(cloneArray(armor));
        player.getEnderChest().setContents(cloneArray(enderChest));
        player.setLevel(level);
        player.setExp(exp);
        player.setHealth(Math.min(health, player.getMaxHealth()));
        player.setFoodLevel(food);
        player.setSaturation(saturation);
        player.setGameMode(gameMode);
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
        if (location != null && location.getWorld() != null) {
            player.teleport(location);
        }
        player.updateInventory();
    }

    private static ItemStack[] cloneArray(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}
