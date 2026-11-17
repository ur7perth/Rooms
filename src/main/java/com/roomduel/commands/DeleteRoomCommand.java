package com.roomduel.commands;

import com.roomduel.RoomDuelPlugin;
import com.roomduel.model.Room;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class DeleteRoomCommand implements TabExecutor {

    private final RoomDuelPlugin plugin;

    public DeleteRoomCommand(RoomDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(RoomDuelPlugin.PERMISSION)) {
            plugin.getMessages().send(sender, "no-permission");
            return true;
        }
        if (args.length != 1) {
            plugin.getMessages().send(sender, "usage-deleteroom");
            return true;
        }

        Room room = plugin.getRoomManager().get(args[0]);
        if (room == null) {
            plugin.getMessages().send(sender, "room-not-found", "%room%", args[0]);
            return true;
        }

        String name = room.getName();
        plugin.getRoomManager().delete(room);
        plugin.getMessages().send(sender, "room-deleted", "%room%", name);
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
