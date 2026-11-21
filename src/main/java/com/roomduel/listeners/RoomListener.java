package com.roomduel.listeners;

import com.roomduel.RoomDuelPlugin;
import com.roomduel.manager.RoomManager;
import com.roomduel.model.Room;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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

        // Spectators (admins watching) are ignored completely: they never count,
        // never trigger the lock, never get pushed back and see no messages.
        if (p.getGameMode() == GameMode.SPECTATOR) {
            if (current != null) {
                manager.leave(current, p.getUniqueId(), null);
            }
            return;
        }

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

    /** Player respawns inside a full/closed room -> send them to the room's exit point (/roomex). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent e) {
        Room room = manager.getRoomAt(e.getRespawnLocation());
        if (room == null || manager.canEnter(room)) return;

        Location exit = room.getExit();
        if (exit != null) {
            e.setRespawnLocation(exit);
        }
    }

    /** Player logs in inside a full/closed room -> send them to the exit point. */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.SPECTATOR) return;

        Room room = manager.getRoomAt(p.getLocation());
        if (room == null || manager.canEnter(room)) return;

        Location exit = room.getExit();
        if (exit != null) {
            p.teleport(exit);
        }
    }

    /** A player inside a room who switches to spectator stops counting as a player. */
    @EventHandler(ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        if (e.getNewGameMode() != GameMode.SPECTATOR) return;
        Player p = e.getPlayer();
        Room room = manager.getPlayerRoom(p.getUniqueId());
        if (room != null) {
            manager.leave(room, p.getUniqueId(), null);
        }
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
