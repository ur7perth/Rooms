package com.roomduel.listeners;

import com.roomduel.RoomDuelPlugin;
import com.roomduel.manager.RoomManager;
import com.roomduel.model.Room;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public class RoomListener implements Listener {

    private final RoomDuelPlugin plugin;
    private final RoomManager manager;

    public RoomListener(RoomDuelPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getRoomManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        handleMovement(e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        handleMovement(e);
    }

    private void handleMovement(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        Player p = e.getPlayer();
        Room current = manager.getPlayerRoom(p.getUniqueId());
        Room target = manager.getRoomAt(to);
        if (current == target) return;

        // Entering a room that is full or closed -> push the player back
        if (target != null && !manager.canEnter(target)) {
            plugin.getMessages().sound(p, "room-full");
            if (e instanceof PlayerTeleportEvent) {
                e.setCancelled(true);
            } else {
                e.setTo(pushBack(from, to));
            }
            return;
        }

        if (current != null) {
            manager.leave(current, p.getUniqueId(), p);
        }
        if (target != null) {
            manager.join(target, p);
        }
    }

    private Location pushBack(Location from, Location to) {
        int blocks = plugin.getConfig().getInt("settings.push-back-blocks", 2);
        Vector back = from.toVector().subtract(to.toVector());
        back.setY(0);
        if (back.lengthSquared() < 1.0E-6) return from;
        back.normalize();

        for (int i = blocks; i >= 1; i--) {
            Location target = from.clone().add(back.clone().multiply(i));
            if (isSafe(target)) return target;
        }
        return from;
    }

    private boolean isSafe(Location loc) {
        return loc.getBlock().isPassable() && loc.clone().add(0, 1, 0).getBlock().isPassable();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        removeFromRoom(e.getEntity());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        removeFromRoom(e.getPlayer());
        plugin.getSelectionManager().clear(e.getPlayer().getUniqueId());
    }

    private void removeFromRoom(Player p) {
        Room room = manager.getPlayerRoom(p.getUniqueId());
        if (room != null) {
            manager.leave(room, p.getUniqueId(), null);
        }
    }

    /** Door blocks cannot be broken while the room is closed. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (manager.isDoorBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }
}
