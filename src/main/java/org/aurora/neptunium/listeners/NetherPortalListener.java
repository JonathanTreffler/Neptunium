package org.aurora.neptunium.listeners;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;

public class NetherPortalListener implements Listener {

    @EventHandler
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;

        if (event.getFrom() != null && event.getFrom().getWorld().getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "The Nether is not available yet.");
        }
    }

    @EventHandler
    public void onPortalCreateEvent(PortalCreateEvent event) {
        event.getBlocks().stream().findFirst().ifPresent(block -> {
            for (Player p : block.getLocation().getNearbyPlayers(10)) {
                p.sendMessage(ChatColor.RED + "The Nether is not available yet.");
            }
        });

        event.setCancelled(true);
    }
}
