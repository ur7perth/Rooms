package com.roomduel.commands;

import com.roomduel.RoomDuelPlugin;
import com.roomduel.model.Room;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/** /roomex <room> - sets the exit point where players are sent when they respawn inside a full room. */
public class RoomExCommand implements TabExecutor {

    private final RoomDuelPlugin plugin;

    public RoomExCommand(RoomDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission(RoomDuelPlugin.PERMISSION)) {
            plugin.getMessages().send(player, "no-permission");
            return true;
        }
        if (args.length != 1) {
            plugin.getMessages().send(player, "usage-roomex");
            return true;
        }

        Room room = plugin.getRoomManager().get(args[0]);
        if (room == null) {
            plugin.getMessages().send(player, "room-not-found", "%room%", args[0]);
            return true;
        }

        Location loc = player.getLocation();
        if (room.contains(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
            plugin.getMessages().send(player, "exit-inside");
            return true;
        }

        room.setExit(loc);
        plugin.getRoomManager().save();
        plugin.getMessages().send(player, "exit-set", "%room%", room.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], plugin.getRoomManager().getRoomNames(), out);
        }
        return out;
    }
}
