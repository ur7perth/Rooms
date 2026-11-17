package com.roomduel.listeners;

import com.roomduel.RoomDuelPlugin;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class WandListener implements Listener {

    private final RoomDuelPlugin plugin;

    public WandListener(RoomDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action action = e.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;
        if (!plugin.getSelectionManager().isWand(e.getItem())) return;

        Player p = e.getPlayer();
        if (!p.hasPermission(RoomDuelPlugin.PERMISSION)) return;

        e.setCancelled(true);
        Block block = e.getClickedBlock();
        if (block == null) return;

        String x = String.valueOf(block.getX());
        String y = String.valueOf(block.getY());
        String z = String.valueOf(block.getZ());

        if (action == Action.LEFT_CLICK_BLOCK) {
            plugin.getSelectionManager().setPos1(p, block.getLocation());
            plugin.getMessages().send(p, "pos1-set", "%x%", x, "%y%", y, "%z%", z);
        } else {
            plugin.getSelectionManager().setPos2(p, block.getLocation());
            plugin.getMessages().send(p, "pos2-set", "%x%", x, "%y%", y, "%z%", z);
        }
    }
}
