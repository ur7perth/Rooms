package com.roomduel.commands;

import com.roomduel.RoomDuelPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RoomWandCommand implements CommandExecutor {

    private final RoomDuelPlugin plugin;

    public RoomWandCommand(RoomDuelPlugin plugin) {
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

        ItemStack wand = plugin.getSelectionManager().createWand();
        for (ItemStack leftover : player.getInventory().addItem(wand).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
        plugin.getMessages().send(player, "wand-given");
        return true;
    }
}
