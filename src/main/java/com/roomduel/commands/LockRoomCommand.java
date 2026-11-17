package com.roomduel.commands;

import com.roomduel.RoomDuelPlugin;
import com.roomduel.model.Room;
import com.roomduel.util.TimeParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/** /lockroom <time> [room] - how long to wait before the door closes. */
public class LockRoomCommand implements TabExecutor {

    private final RoomDuelPlugin plugin;

    public LockRoomCommand(RoomDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(RoomDuelPlugin.PERMISSION)) {
            plugin.getMessages().send(sender, "no-permission");
            return true;
        }
        if (args.length < 1 || args.length > 2) {
            plugin.getMessages().send(sender, "usage-lockroom");
            return true;
        }

        Double seconds = TimeParser.parseSeconds(args[0]);
        if (seconds == null) {
            plugin.getMessages().send(sender, "invalid-time");
            return true;
        }
        String shown = TimeParser.format(seconds);

        if (args.length == 2) {
            Room room = plugin.getRoomManager().get(args[1]);
            if (room == null) {
                plugin.getMessages().send(sender, "room-not-found", "%room%", args[1]);
                return true;
            }
            room.setLockSeconds(seconds);
            plugin.getRoomManager().save();
            plugin.getMessages().send(sender, "lock-set-room", "%time%", shown, "%room%", room.getName());
        } else {
            plugin.getConfig().set("settings.lock-delay-seconds", seconds);
            plugin.saveConfig();
            plugin.getMessages().send(sender, "lock-set", "%time%", shown);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("0.5s", "1s", "3s", "5s", "10s"), out);
        } else if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], plugin.getRoomManager().getRoomNames(), out);
        }
        return out;
    }
}
