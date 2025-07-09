package org.aurora.neptunium;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class ExtraCommands implements CommandExecutor {

    private final File logsFolder = Main.logsFolder;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!command.getName().equalsIgnoreCase("print-logs")) return false;

        if (args.length == 0) {
            listLogs(player);
            return true;
        }

        String filename = args[0];

        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            player.sendMessage(Component.text("Invalid log filename.", NamedTextColor.RED));
            return true;
        }

        File fileLog = new File(logsFolder, filename + ".log");
        File fileGz = new File(logsFolder, filename + ".log.gz");

        File file;
        if (fileLog.exists()) {
            file = fileLog;
        } else if (fileGz.exists()) {
            file = fileGz;
        } else {
            player.sendMessage(Component.text("Log file not found.", NamedTextColor.RED));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(Main.instance, () -> {
            try {
                if (!file.getCanonicalPath().startsWith(logsFolder.getCanonicalPath())) {
                    Bukkit.getScheduler().runTask(Main.instance, () ->
                            player.sendMessage(Component.text("Log file not found.", NamedTextColor.RED))
                    );
                    return;
                }

                List<String> lines = getStrings(file);

                Bukkit.getScheduler().runTask(Main.instance, () -> {
                    player.sendMessage(Component.text("x-x-x-x-x-x-x-x-x", NamedTextColor.GRAY));
                    player.sendMessage(Component.text("Printing " + filename + ":", NamedTextColor.GOLD));
                    for (String line : lines) {
                        player.sendMessage(Component.text(line, NamedTextColor.WHITE));
                    }
                    player.sendMessage(Component.text("x-x-x-x-x-x-x-x-x", NamedTextColor.GRAY));
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Main.instance, () ->
                        player.sendMessage(Component.text("Error reading file.", NamedTextColor.RED))
                );
                e.printStackTrace();
            }
        });

        return true;
    }

    private static @NotNull List<String> getStrings(File file) throws IOException {
        List<String> lines = new LinkedList<>();

        if (file.getName().endsWith(".gz")) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new GZIPInputStream(new FileInputStream(file))))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private void listLogs(Player player) {
        player.sendMessage(Component.text("x-x-x-x-x-x-x-x-x", NamedTextColor.GRAY));

        File[] files = logsFolder.listFiles((dir, name) -> name.endsWith(".log") || name.endsWith(".log.gz"));

        if (files == null || files.length == 0) {
            player.sendMessage(Component.text("No log files found.", NamedTextColor.RED));
        }
        else {
            Arrays.sort(files);
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith(".log.gz")) {
                    name = name.substring(0, name.length() - 7);
                } else if (name.endsWith(".log")) {
                    name = name.substring(0, name.length() - 4);
                }
                Component clickable = Component.text(name, NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/print-logs " + name));
                player.sendMessage(clickable);
            }
        }

        player.sendMessage(Component.text("x-x-x-x-x-x-x-x-x", NamedTextColor.GRAY));
    }
}
