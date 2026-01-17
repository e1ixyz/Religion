package com.elimcgehee.religion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReligionPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final int PRAYER_DURATION_SECONDS = 30;
    private static final long DAY_MILLIS = 20L * 60L * 1000L;

    private final Map<UUID, PrayerSession> activePrayers = new HashMap<>();
    private PrayerDataStore dataStore;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        dataStore = new PrayerDataStore(getDataFolder(), getLogger());
        dataStore.load();

        PluginCommand prayCommand = getCommand("pray");
        if (prayCommand != null) {
            prayCommand.setExecutor(this);
        }

        PluginCommand prayerCommand = getCommand("prayer");
        if (prayerCommand != null) {
            prayerCommand.setExecutor(this);
            prayerCommand.setTabCompleter(this);
        }

        getServer().getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PrayerPlaceholderExpansion(this, dataStore).register();
        }
    }

    @Override
    public void onDisable() {
        for (PrayerSession session : activePrayers.values()) {
            session.cancel();
        }
        activePrayers.clear();
        dataStore.save();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        PrayerSession session = activePrayers.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        if (hasMoved(event.getFrom(), event.getTo())) {
            interruptPrayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PrayerSession session = activePrayers.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.cancel();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        dataStore.setName(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        dataStore.save();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("pray")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can pray.");
                return true;
            }
            handlePrayCommand(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("prayer")) {
            if (args.length == 0) {
                sender.sendMessage("Usage: /prayer leaderboard");
                return true;
            }
            String sub = args[0].toLowerCase();
            if (sub.equals("leaderboard") || sub.equals("lb")) {
                sendLeaderboard(sender);
                return true;
            }
            sender.sendMessage("Usage: /prayer leaderboard");
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("prayer")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            if ("leaderboard".startsWith(prefix)) {
                options.add("leaderboard");
            }
            if ("lb".startsWith(prefix)) {
                options.add("lb");
            }
            return options;
        }
        return List.of();
    }

    private void handlePrayCommand(Player player) {
        UUID uuid = player.getUniqueId();
        if (activePrayers.containsKey(uuid)) {
            player.sendMessage("You are already praying.");
            return;
        }

        if (isOnCooldown(uuid)) {
            player.sendMessage("You may not pray again today.");
            return;
        }

        dataStore.setName(uuid, player.getName());

        BossBar bossBar = Bukkit.createBossBar("Prayer - " + PRAYER_DURATION_SECONDS + "s", BarColor.WHITE,
                BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);

        PrayerSession session = new PrayerSession(bossBar, PRAYER_DURATION_SECONDS);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                PrayerSession current = activePrayers.get(uuid);
                if (current == null) {
                    cancel();
                    bossBar.removeAll();
                    return;
                }
                if (!player.isOnline()) {
                    activePrayers.remove(uuid);
                    current.cancel();
                    return;
                }
                int remaining = current.remainingSeconds;
                if (remaining <= 0) {
                    completePrayer(player);
                    return;
                }
                double progress = remaining / (double) PRAYER_DURATION_SECONDS;
                current.bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                current.bossBar.setTitle("Prayer - " + remaining + "s");
                current.remainingSeconds--;
            }
        }.runTaskTimer(this, 0L, 20L);

        session.task = task;
        activePrayers.put(uuid, session);
    }

    private void interruptPrayer(Player player) {
        PrayerSession session = activePrayers.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.cancel();

        long until = System.currentTimeMillis() + DAY_MILLIS;
        dataStore.setCooldownUntil(player.getUniqueId(), until);
        dataStore.save();

        player.sendMessage("You interrupted your prayer. God is furious. You may not pray again today.");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "smite " + player.getName());
    }

    private void completePrayer(Player player) {
        PrayerSession session = activePrayers.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.cancel();

        dataStore.addPoint(player.getUniqueId());
        dataStore.setName(player.getUniqueId(), player.getName());
        dataStore.save();

        player.sendMessage("Got is pleased with you. +1 prayer point");
    }

    private boolean isOnCooldown(UUID uuid) {
        long until = dataStore.getCooldownUntil(uuid);
        if (until <= 0L) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            dataStore.setCooldownUntil(uuid, 0L);
            dataStore.save();
            return false;
        }
        return true;
    }

    private void sendLeaderboard(CommandSender sender) {
        List<PrayerDataStore.LeaderboardEntry> entries = dataStore.getTop(5);
        if (entries.isEmpty()) {
            sender.sendMessage("No prayers recorded yet.");
            return;
        }
        sender.sendMessage("Prayer Leaderboard:");
        int index = 1;
        for (PrayerDataStore.LeaderboardEntry entry : entries) {
            sender.sendMessage(index + ". " + entry.name() + " - " + entry.points());
            index++;
        }
    }

    private boolean hasMoved(Location from, Location to) {
        if (to == null) {
            return false;
        }
        if (from.getWorld() != to.getWorld()) {
            return true;
        }
        return from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
    }

    private static class PrayerSession {
        private final BossBar bossBar;
        private BukkitTask task;
        private int remainingSeconds;

        private PrayerSession(BossBar bossBar, int remainingSeconds) {
            this.bossBar = bossBar;
            this.remainingSeconds = remainingSeconds;
        }

        private void cancel() {
            if (task != null) {
                task.cancel();
            }
            bossBar.removeAll();
        }
    }
}
