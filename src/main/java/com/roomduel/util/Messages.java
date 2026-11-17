package com.roomduel.util;

import com.roomduel.RoomDuelPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;

public class Messages {

    private final RoomDuelPlugin plugin;

    public Messages(RoomDuelPlugin plugin) {
        this.plugin = plugin;
    }

    public String get(String path, String... replacements) {
        String s = plugin.getConfig().getString("messages." + path, "&cMissing message: " + path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            s = s.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public void send(CommandSender sender, String path, String... replacements) {
        String msg = get(path, replacements);
        if (msg.isEmpty()) return;
        String prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.prefix", ""));
        sender.sendMessage(prefix + msg);
    }

    public void actionBar(Player player, String path, String... replacements) {
        String msg = get(path, replacements);
        if (msg.isEmpty()) return;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    public void sound(Player player, String key) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("sounds." + key);
        if (sec == null) return;
        String name = sec.getString("name", "");
        if (name.isEmpty() || name.equalsIgnoreCase("none")) return;
        try {
            Sound sound = Sound.valueOf(name.toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound,
                    (float) sec.getDouble("volume", 1.0), (float) sec.getDouble("pitch", 1.0));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid sound in config.yml (sounds." + key + "): " + name);
        }
    }
}
