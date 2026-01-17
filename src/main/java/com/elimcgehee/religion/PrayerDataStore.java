package com.elimcgehee.religion;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class PrayerDataStore {
    private final File dataFile;
    private final Logger logger;
    private final Map<UUID, Integer> points = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PrayerDataStore(File dataFolder, Logger logger) {
        this.dataFile = new File(dataFolder, "data.yml");
        this.logger = logger;
    }

    public void load() {
        points.clear();
        names.clear();
        cooldowns.clear();

        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            int storedPoints = section.getInt(key + ".points", 0);
            String storedName = section.getString(key + ".name");
            long cooldownUntil = section.getLong(key + ".cooldown_until", 0L);
            points.put(uuid, storedPoints);
            if (storedName != null && !storedName.isEmpty()) {
                names.put(uuid, storedName);
            }
            if (cooldownUntil > 0L) {
                cooldowns.put(uuid, cooldownUntil);
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : points.entrySet()) {
            String base = "players." + entry.getKey();
            config.set(base + ".points", entry.getValue());
            String name = names.get(entry.getKey());
            if (name != null) {
                config.set(base + ".name", name);
            }
            Long cooldownUntil = cooldowns.get(entry.getKey());
            if (cooldownUntil != null && cooldownUntil > 0L) {
                config.set(base + ".cooldown_until", cooldownUntil);
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            logger.warning("Failed to save data.yml: " + e.getMessage());
        }
    }

    public int getPoints(UUID uuid) {
        return points.getOrDefault(uuid, 0);
    }

    public void addPoint(UUID uuid) {
        points.put(uuid, getPoints(uuid) + 1);
    }

    public void setName(UUID uuid, String name) {
        if (name != null && !name.isEmpty()) {
            names.put(uuid, name);
        }
    }

    public long getCooldownUntil(UUID uuid) {
        return cooldowns.getOrDefault(uuid, 0L);
    }

    public void setCooldownUntil(UUID uuid, long until) {
        if (until > 0L) {
            cooldowns.put(uuid, until);
        } else {
            cooldowns.remove(uuid);
        }
    }

    public List<LeaderboardEntry> getTop(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : points.entrySet()) {
            int value = entry.getValue();
            if (value <= 0) {
                continue;
            }
            String name = names.getOrDefault(entry.getKey(), "Unknown");
            entries.add(new LeaderboardEntry(entry.getKey(), name, value));
        }

        entries.sort(Comparator
                .comparingInt(LeaderboardEntry::points).reversed()
                .thenComparing(LeaderboardEntry::name, String.CASE_INSENSITIVE_ORDER));

        if (entries.size() > limit) {
            return entries.subList(0, limit);
        }
        return entries;
    }

    public record LeaderboardEntry(UUID uuid, String name, int points) {
    }
}
