package org.aurora.neptunium;

import org.aurora.neptunium.listeners.EndPortalListener;
import org.aurora.neptunium.listeners.NetherPortalListener;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Main extends JavaPlugin implements TabExecutor {

/**/public static Main instance;

/**/public static File logsFolder;

    private Instant netherLockDate;
    private Instant endLockDate;

    private boolean netherLocked;
    private boolean endLocked;

    private final NetherPortalListener netherListener = new NetherPortalListener();
    private final EndPortalListener endListener = new EndPortalListener();

    private final Set<Long> netherWarningsSent = new HashSet<>();
    private final Set<Long> endWarningsSent = new HashSet<>();

    private static final long[] WARNING_TIMES = {
        592200, 172800, 86400, 43200, 21600, 7200, 3600, 1800, 600, 300, 60, 30, 10, 3, 2, 1
    };

    private ZoneId displayZone;

    @Override
    public void onEnable() {
/**/    instance = this;

/**/    logsFolder = new File(getDataFolder().getParentFile().getParentFile(), "logs");
/**/    if (!logsFolder.exists()) logsFolder.mkdirs();

/**/    //getCommand("print-logs").setExecutor(new ExtraCommands());

        saveDefaultConfig();
        loadConfig();

        Objects.requireNonNull(getCommand("dimensionlock")).setExecutor(this);
        Objects.requireNonNull(getCommand("dl")).setExecutor(this);

        Bukkit.getScheduler().runTaskTimer(this, this::checkLocks, 0L, 20L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::broadcastWarnings, 20L, 20L);
    }

    @Override
    public void onDisable() {
        saveConfig();
        HandlerList.unregisterAll(this);
    }

    private void checkLocks() {
        Instant now = Instant.now();

        boolean prevNetherLocked = netherLocked;
        boolean prevEndLocked = endLocked;

        netherLocked = now.isBefore(netherLockDate);
        endLocked = now.isBefore(endLockDate);

        if (netherLocked != prevNetherLocked) {
            updateListener(netherLocked, netherListener, "Nether");
            getConfig().set("nether-lock.locked", netherLocked);
        }

        if (endLocked != prevEndLocked) {
            updateListener(endLocked, endListener, "End");
            getConfig().set("end-lock.locked", endLocked);
        }

        if (netherLocked != prevNetherLocked || endLocked != prevEndLocked) {
            saveConfig();
        }
    }

    private void updateListener(boolean lockActive, org.bukkit.event.Listener listener, String dimension) {
        if (lockActive) {
            Bukkit.getPluginManager().registerEvents(listener, this);
            getLogger().info(dimension + " portals locked.");
        }
        else {
            HandlerList.unregisterAll(listener);
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f));
            getLogger().info(dimension + " portals unlocked.");
        }
    }

    private void broadcastWarnings() {
        Instant now = Instant.now();
        long netherRemaining = netherLockDate.getEpochSecond() - now.getEpochSecond();
        long endRemaining = endLockDate.getEpochSecond() - now.getEpochSecond();

        for (long warnTime : WARNING_TIMES) {
            if (netherLocked && netherRemaining <= warnTime && netherWarningsSent.add(warnTime)) {
                sendBroadcast("Nether", warnTime);
            }
            if (endLocked && endRemaining <= warnTime && endWarningsSent.add(warnTime)) {
                sendBroadcast("End", warnTime);
            }
        }
    }

    private void sendBroadcast(String dimension, long secondsRemaining) {
        String timeStr = formatDuration(secondsRemaining);
        Bukkit.getScheduler().runTask(this, () ->
                Bukkit.broadcastMessage("§5[Server] " + dimension + " portals unlock in §6" + timeStr + "§5!")
        );
    }

    private String formatDuration(long seconds) {
        if (seconds >= 86400) return (seconds / 86400) + "d";
        if (seconds >= 3600) return (seconds / 3600) + "h";
        if (seconds >= 60) return (seconds / 60) + "m";
        return seconds + "s";
    }

    private void loadConfig() {
        netherLocked = getConfig().getBoolean("nether-lock.locked", false);
        endLocked = getConfig().getBoolean("end-lock.locked", false);

        String tz = getConfig().getString("timezone", "UTC");
        try {
            displayZone = ZoneId.of(tz);
        }
        catch (Exception e) {
            getLogger().warning("Invalid timezone '" + tz + "', using UTC.");
            displayZone = ZoneId.of("UTC");
        }

        boolean saveNeeded = false;

        netherLockDate = parseDate("nether-lock.date");
        if (netherLockDate == null) {
            netherLockDate = Instant.parse("2099-01-01T00:00:00Z");
            getConfig().set("nether-lock.date", netherLockDate.toString());
            getLogger().warning("Missing or invalid 'nether-lock.date'. Fallback used.");
            saveNeeded = true;
        }

        endLockDate = parseDate("end-lock.date");
        if (endLockDate == null) {
            endLockDate = Instant.parse("2099-01-01T00:00:00Z");
            getConfig().set("end-lock.date", endLockDate.toString());
            getLogger().warning("Missing or invalid 'end-lock.date'. Fallback used.");
            saveNeeded = true;
        }

        if (saveNeeded) saveConfig();

        HandlerList.unregisterAll(this);
        if (netherLocked) Bukkit.getPluginManager().registerEvents(netherListener, this);
        if (endLocked) Bukkit.getPluginManager().registerEvents(endListener, this);
    }

    private Instant parseDate(String key) {
        try {
            String value = getConfig().getString(key);
            return value != null ? Instant.parse(value) : null;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("dimensionlock") && !command.getName().equalsIgnoreCase("dl")) {
            return false;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy 'at' HH:mm z");

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp()) {
                sender.sendMessage("§cYou do not have permission to execute this command.");
                return true;
            }

            reloadConfig();
            loadConfig();
            checkLocks();

            sender.sendMessage("§a[DimensionLock] Configuration reloaded.");
            return true;
        }

        if (!netherLocked) {
            sender.sendMessage("§5Nether portals are §aunlocked§5.");
        }
        else {
            sender.sendMessage("§5Nether portals unlock on §6" + formatter.format(netherLockDate.atZone(displayZone)) + "§5.");
        }

        if (!endLocked) {
            sender.sendMessage("§5End portals are §aunlocked§5.");
        }
        else {
            sender.sendMessage("§5End portals unlock on §6" + formatter.format(endLockDate.atZone(displayZone)) + "§5.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && command.getName().equalsIgnoreCase("dl")) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }
}
