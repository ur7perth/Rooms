package com.roomduel.manager;

import com.roomduel.RoomDuelPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

    private final RoomDuelPlugin plugin;
    private final NamespacedKey wandKey;
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public SelectionManager(RoomDuelPlugin plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "room_wand");
    }

    public ItemStack createWand() {
        ItemStack item = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Room Wand");
        meta.setLore(List.of(
                "§7Left-click a block: §ePos 1",
                "§7Right-click a block: §ePos 2",
                "§8Then use /setroom <name>"));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public void setPos1(Player p, Location loc) { pos1.put(p.getUniqueId(), loc); }
    public void setPos2(Player p, Location loc) { pos2.put(p.getUniqueId(), loc); }
    public Location getPos1(Player p) { return pos1.get(p.getUniqueId()); }
    public Location getPos2(Player p) { return pos2.get(p.getUniqueId()); }

    public void clear(UUID id) {
        pos1.remove(id);
        pos2.remove(id);
    }

    /** Draws the selection box with particles for players holding the wand. */
    public void startParticleTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!isWand(p.getInventory().getItemInMainHand())) continue;
                Location a = pos1.get(p.getUniqueId());
                Location b = pos2.get(p.getUniqueId());
                if (a == null && b == null) continue;
                if (a == null) a = b;
                if (b == null) b = a;
                if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())
                        || !a.getWorld().equals(p.getWorld())) continue;
                drawBox(p, a, b);
            }
        }, 20L, 5L);
    }

    private void drawBox(Player p, Location a, Location b) {
        double x1 = Math.min(a.getBlockX(), b.getBlockX());
        double y1 = Math.min(a.getBlockY(), b.getBlockY());
        double z1 = Math.min(a.getBlockZ(), b.getBlockZ());
        double x2 = Math.max(a.getBlockX(), b.getBlockX()) + 1;
        double y2 = Math.max(a.getBlockY(), b.getBlockY()) + 1;
        double z2 = Math.max(a.getBlockZ(), b.getBlockZ()) + 1;

        double[] xs = {x1, x2};
        double[] ys = {y1, y2};
        double[] zs = {z1, z2};

        for (double y : ys) for (double z : zs) line(p, x1, y, z, x2, y, z);
        for (double x : xs) for (double z : zs) line(p, x, y1, z, x, y2, z);
        for (double x : xs) for (double y : ys) line(p, x, y, z1, x, y, z2);
    }

    private void line(Player p, double x1, double y1, double z1, double x2, double y2, double z2) {
        double length = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
        int points = (int) Math.min(80, Math.max(2, length * 2));
        Particle.DustOptions dust = new Particle.DustOptions(Color.LIME, 1.0f);
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            p.spawnParticle(Particle.REDSTONE,
                    x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t,
                    1, 0, 0, 0, 0, dust);
        }
    }
}
