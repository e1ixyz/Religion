package com.elimcgehee.religion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrayerPlaceholderExpansion extends PlaceholderExpansion {
    private final ReligionPlugin plugin;
    private final PrayerDataStore dataStore;

    public PrayerPlaceholderExpansion(ReligionPlugin plugin, PrayerDataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "religion";
    }

    @Override
    public @NotNull String getAuthor() {
        return "elimcgehee";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null) {
            return "0";
        }
        if (identifier.equalsIgnoreCase("prayer_points")) {
            return String.valueOf(dataStore.getPoints(player.getUniqueId()));
        }
        return null;
    }
}
