package com.roomduel.commands;

import com.roomduel.RoomDuelPlugin;
import com.roomduel.model.Room;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlockRoomCommand implements TabExecutor {

    private final RoomDuelPlugin plugin;

    public BlockRoomCommand(RoomDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(RoomDuelPlugin.PERMISSION)) {
            plugin.getMessages().send(sender, "no-permission");
            return true;
        }
        if (args.length != 2) {
            plugin.getMessages().send(sender, "usage-blockroom");
            return true;
        }

        Room room = plugin.getRoomManager().get(args[0]);
        if (room == null) {
            plugin.getMessages().send(sender, "room-not-found", "%room%", args[0]);
            return true;
        }

        Material mat = Material.matchMaterial(args[1]);
        if (!isValid(mat)) {
            plugin.getMessages().send(sender, "invalid-block", "%block%", args[1]);
            return true;
        }

        plugin.getRoomManager().setBlockMaterial(room, mat);
        plugin.getMessages().send(sender, "block-changed",
                "%room%", room.getName(), "%block%", mat.name());
        return true;
    }

    private boolean isValid(Material mat) {
        return mat != null
                && !mat.name().startsWith("LEGACY_")
                && mat.isBlock()
                && mat.isSolid()
                && !mat.isAir();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], plugin.getRoomManager().getRoomNames(), out);
        } else if (args.length == 2) {
            List<String> blocks = new ArrayList<>();
            for (Material m : Material.values()) {
                if (isValid(m)) blocks.add(m.name().toLowerCase(Locale.ROOT));
            }
            StringUtil.copyPartialMatches(args[1], blocks, out);
        }
        return out;
    }
}
