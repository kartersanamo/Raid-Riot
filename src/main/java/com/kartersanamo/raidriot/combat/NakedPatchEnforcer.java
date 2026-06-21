package com.kartersanamo.raidriot.combat;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class NakedPatchEnforcer {

    private final RaidRiotPlugin plugin;

    public NakedPatchEnforcer(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean mustCancelPatch(Player player, RaidMatch match) {
        if (match == null || !match.isActive() || !match.isParticipant(player)) {
            return false;
        }
        if (!match.isInOwnPatchRegion(player)) {
            return false;
        }
        return !isNaked(player);
    }

    private boolean isNaked(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece != null && piece.getType() != org.bukkit.Material.AIR) {
                return false;
            }
        }
        return true;
    }
}
