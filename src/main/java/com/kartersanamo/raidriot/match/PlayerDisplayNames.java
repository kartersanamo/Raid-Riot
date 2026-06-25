package com.kartersanamo.raidriot.match;

import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class PlayerDisplayNames {

    private PlayerDisplayNames() {
    }

    public static String colored(RaidMatch match, Player player) {
        if (player == null) {
            return "";
        }
        return colored(match, player.getUniqueId(), player.getName());
    }

    public static String colored(RaidMatch match, UUID playerId) {
        return colored(match, playerId, resolveName(playerId));
    }

    public static String colored(RaidMatch match, UUID playerId, String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        TeamSide side = match != null ? match.getTeamFor(playerId) : null;
        return colored(name, side);
    }

    public static String colored(String name, TeamSide side) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        if (side == null) {
            return "&7" + name;
        }
        return ConfigManager.get().getTeamChatColor(side) + name;
    }

    public static String joinTeammates(RaidMatch match, UUID self, TeamSide side, String separator) {
        List<ColoredName> entries = new ArrayList<>();
        for (UUID id : match.getEnrolledParticipants()) {
            if (id.equals(self) || match.getTeamFor(id) != side) {
                continue;
            }
            String plain = resolveName(id);
            if (plain == null || plain.isEmpty()) {
                continue;
            }
            entries.add(new ColoredName(plain, colored(match, id, plain)));
        }
        Collections.sort(entries, (left, right) -> left.plain.compareToIgnoreCase(right.plain));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(entries.get(i).colored);
        }
        return builder.toString();
    }

    public static List<String> coloredSorted(RaidMatch match, Iterable<UUID> playerIds, TeamSide side) {
        List<ColoredName> entries = new ArrayList<>();
        for (UUID id : playerIds) {
            if (match.getTeamFor(id) != side) {
                continue;
            }
            String plain = resolveName(id);
            if (plain == null || plain.isEmpty()) {
                continue;
            }
            entries.add(new ColoredName(plain, colored(match, id, plain)));
        }
        Collections.sort(entries, (left, right) -> left.plain.compareToIgnoreCase(right.plain));
        List<String> names = new ArrayList<>();
        for (ColoredName entry : entries) {
            names.add(entry.colored);
        }
        return names;
    }

    public static String resolveName(UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        return offline.getName();
    }

    private static final class ColoredName {
        private final String plain;
        private final String colored;

        private ColoredName(String plain, String colored) {
            this.plain = plain;
            this.colored = colored;
        }
    }
}
