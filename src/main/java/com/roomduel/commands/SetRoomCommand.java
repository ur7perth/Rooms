package com.roomduel.commands;

import com.roomduel.RoomDuelPlugin;
import com.roomduel.model.Room;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class SetRoomCommand implements CommandExecutor {

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

    private final RoomDuelPlugin plugin;

    public SetRoomCommand(RoomDuelPlugin plugin) {
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
            plugin.getMessages().send(player, "usage-setroom");
            return true;
        }

        String name = args[0];
        if (!NAME.matcher(name).matches()) {
            plugin.getMessages().send(player, "invalid-name");
            return true;
        }
        if (plugin.getRoomManager().exists(name)) {
            plugin.getMessages().send(player, "room-exists", "%room%", name);
            return true;
        }

        Location a = plugin.getSelectionManager().getPos1(player);
        Location b = plugin.getSelectionManager().getPos2(player);
        if (a == null || b == null) {
            plugin.getMessages().send(player, "selection-incomplete");
            return true;
        }
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            plugin.getMessages().send(player, "selection-world");
            return true;
        }

        Room room = plugin.getRoomManager().create(name, a, b);
        String size = (room.getMaxX() - room.getMinX() + 1) + "x"
                + (room.getMaxY() - room.getMinY() + 1) + "x"
                + (room.getMaxZ() - room.getMinZ() + 1);
        plugin.getMessages().send(player, "room-created", "%room%", room.getName(), "%size%", size);
        return true;
    }
}
