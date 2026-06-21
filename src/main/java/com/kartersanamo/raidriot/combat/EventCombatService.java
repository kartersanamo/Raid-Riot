package com.kartersanamo.raidriot.combat;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

/**
 * Lets SaberFactions allow PvP between same-faction players during events (cross-team only enforced by us).
 */
public final class EventCombatService {

    static final String FACTIONS_FRIENDLY_FIRE_META = "friendlyFire";

    private final RaidRiotPlugin plugin;

    public EventCombatService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void enableForMatch(RaidMatch match) {
        for (UUID id : match.getParticipants()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                enableForParticipant(player);
            }
        }
    }

    public void disableForMatch(RaidMatch match) {
        for (UUID id : match.getParticipants()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                disableForParticipant(player);
            }
        }
    }

    public void enableForParticipant(Player player) {
        player.setMetadata(FACTIONS_FRIENDLY_FIRE_META, new FixedMetadataValue(plugin, true));
    }

    public void disableForParticipant(Player player) {
        player.removeMetadata(FACTIONS_FRIENDLY_FIRE_META, plugin);
    }
}
