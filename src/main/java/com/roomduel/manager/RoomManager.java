package com.roomduel.manager;

import com.roomduel.RoomDuelPlugin;
import com.roomduel.model.Room;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class RoomManager {

    private final RoomDuelPlugin plugin;
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final Map<UUID, Room> playerRooms = new HashMap<>();
    private final File file;

    public RoomManager(RoomDuelPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rooms.yml");
    }

    // ------------------------------------------------------------------
    // Storage
    // ------------------------------------------------------------------

    public void load() {
        rooms.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("rooms");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            Room room = new Room(key, s.getString("world", "world"),
                    s.getInt("min.x"), s.getInt("min.y"), s.getInt("min.z"),
                    s.getInt("max.x"), s.getInt("max.y"), s.getInt("max.z"));
            Material mat = Material.matchMaterial(s.getString("block", "GLASS"));
            room.setBlockMaterial(mat != null ? mat : Material.GLASS);
            if (s.contains("lock-seconds")) room.setLockSeconds(s.getDouble("lock-seconds"));
            if (s.contains("unlock-seconds")) room.setUnlockSeconds(s.getDouble("unlock-seconds"));
            rooms.put(key.toLowerCase(Locale.ROOT), room);
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Room r : rooms.values()) {
            String p = "rooms." + r.getName() + ".";
            yml.set(p + "world", r.getWorldName());
            yml.set(p + "min.x", r.getMinX());
            yml.set(p + "min.y", r.getMinY());
            yml.set(p + "min.z", r.getMinZ());
            yml.set(p + "max.x", r.getMaxX());
            yml.set(p + "max.y", r.getMaxY());
            yml.set(p + "max.z", r.getMaxZ());
            yml.set(p + "block", r.getBlockMaterial().name());
            yml.set(p + "lock-seconds", r.getLockSeconds());
            yml.set(p + "unlock-seconds", r.getUnlockSeconds());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save rooms.yml: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Room access
    // ------------------------------------------------------------------

    public Room create(String name, Location a, Location b) {
        Room room = new Room(name, a.getWorld().getName(),
                a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                b.getBlockX(), b.getBlockY(), b.getBlockZ());
        Material mat = Material.matchMaterial(plugin.getConfig().getString("settings.default-block", "GLASS"));
        room.setBlockMaterial(mat != null ? mat : Material.GLASS);
        rooms.put(name.toLowerCase(Locale.ROOT), room);
        save();
        return room;
    }

    public void delete(Room room) {
        forceOpen(room);
        for (UUID id : new ArrayList<>(room.getPlayers())) {
            playerRooms.remove(id);
        }
        room.getPlayers().clear();
        rooms.remove(room.getName().toLowerCase(Locale.ROOT));
        save();
    }

    public Room get(String name) {
        return name == null ? null : rooms.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return get(name) != null;
    }

    public List<Room> getRooms() {
        return new ArrayList<>(rooms.values());
    }

    public List<String> getRoomNames() {
        List<String> names = new ArrayList<>();
        for (Room r : rooms.values()) names.add(r.getName());
        return names;
    }

    public Room getRoomAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        for (Room r : rooms.values()) {
            if (r.contains(loc.getWorld(), x, y, z)) return r;
        }
        return null;
    }

    public Room getPlayerRoom(UUID id) {
        return playerRooms.get(id);
    }

    public boolean isDoorBlock(Block block) {
        for (Room r : rooms.values()) {
            if (r.getPlacedBlocks().contains(block)) return true;
        }
        return false;
    }

    public void shutdown() {
        for (Room r : rooms.values()) forceOpen(r);
    }

    // ------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------

    public int playersRequired() {
        return Math.max(2, plugin.getConfig().getInt("settings.players-per-room", 2));
    }

    public double lockSeconds(Room room) {
        return room.getLockSeconds() != null ? room.getLockSeconds()
                : plugin.getConfig().getDouble("settings.lock-delay-seconds", 3.0);
    }

    public double unlockSeconds(Room room) {
        return room.getUnlockSeconds() != null ? room.getUnlockSeconds()
                : plugin.getConfig().getDouble("settings.unlock-delay-seconds", 3.0);
    }

    private long toTicks(double seconds) {
        return Math.round(seconds * 20.0);
    }

    // ------------------------------------------------------------------
    // Join / leave
    // ------------------------------------------------------------------

    public boolean canEnter(Room room) {
        Room.State s = room.getState();
        return (s == Room.State.OPEN || s == Room.State.CLOSING)
                && room.getPlayers().size() < playersRequired();
    }

    public void join(Room room, Player player) {
        room.getPlayers().add(player.getUniqueId());
        playerRooms.put(player.getUniqueId(), room);
        plugin.getMessages().actionBar(player, "actionbar-enter", "%room%", room.getName());
        evaluate(room);
    }

    /** @param notify the player to show "You Left" to, or null for no message. */
    public void leave(Room room, UUID id, Player notify) {
        room.getPlayers().remove(id);
        playerRooms.remove(id);
        if (notify != null) {
            plugin.getMessages().actionBar(notify, "actionbar-left", "%room%", room.getName());
        }
        evaluate(room);
    }

    // ------------------------------------------------------------------
    // State machine
    // ------------------------------------------------------------------

    private void evaluate(Room room) {
        int n = room.getPlayers().size();
        int need = playersRequired();
        switch (room.getState()) {
            case OPEN -> {
                if (n >= need) startClosing(room);
            }
            case CLOSING -> {
                if (n < need) {
                    cancelTask(room);
                    room.setState(Room.State.OPEN);
                }
            }
            case CLOSED -> {
                if (n < need) startUnlocking(room);
            }
            case UNLOCKING -> {
                if (n >= need) {
                    cancelTask(room);
                    room.setState(Room.State.CLOSED);
                }
            }
        }
    }

    private void startClosing(Room room) {
        cancelTask(room);
        room.setState(Room.State.CLOSING);
        long ticks = toTicks(lockSeconds(room));
        if (ticks <= 0) {
            closeDoor(room);
            return;
        }
        room.setTask(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            room.setTask(null);
            if (room.getState() == Room.State.CLOSING && room.getPlayers().size() >= playersRequired()) {
                closeDoor(room);
            }
        }, ticks));
    }

    private void closeDoor(Room room) {
        cancelTask(room);
        room.setState(Room.State.CLOSED);
        placeShell(room);
        for (Player p : playersIn(room)) {
            plugin.getMessages().actionBar(p, "actionbar-door-closed", "%room%", room.getName());
            plugin.getMessages().sound(p, "door-close");
        }
        evaluate(room);
    }

    private void startUnlocking(Room room) {
        cancelTask(room);
        room.setState(Room.State.UNLOCKING);
        final long total = toTicks(unlockSeconds(room));
        if (total <= 0) {
            openDoor(room);
            return;
        }
        final long[] remaining = {total};
        final int[] lastShown = {-1};
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                long rem = remaining[0];
                int secs = (int) Math.ceil(rem / 20.0);
                if (secs != lastShown[0]) {
                    lastShown[0] = secs;
                    for (Player p : playersIn(room)) {
                        plugin.getMessages().actionBar(p, "actionbar-unlock-countdown",
                                "%seconds%", String.valueOf(secs), "%room%", room.getName());
                    }
                }
                if (rem <= 0) {
                    cancel();
                    room.setTask(null);
                    openDoor(room);
                    return;
                }
                remaining[0]--;
            }
        };
        room.setTask(runnable.runTaskTimer(plugin, 0L, 1L));
    }

    private void openDoor(Room room) {
        cancelTask(room);
        removeShell(room);
        room.setState(Room.State.OPEN);
        evaluate(room);
    }

    private void forceOpen(Room room) {
        cancelTask(room);
        removeShell(room);
        room.setState(Room.State.OPEN);
    }

    private void cancelTask(Room room) {
        if (room.getTask() != null) {
            room.getTask().cancel();
            room.setTask(null);
        }
    }

    private List<Player> playersIn(Room room) {
        List<Player> list = new ArrayList<>();
        for (UUID id : room.getPlayers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) list.add(p);
        }
        return list;
    }

    // ------------------------------------------------------------------
    // Door blocks (the outer shell of the selected area)
    // ------------------------------------------------------------------

    private void forEachShellBlock(Room room, Consumer<Block> consumer) {
        World world = Bukkit.getWorld(room.getWorldName());
        if (world == null) return;
        for (int x = room.getMinX(); x <= room.getMaxX(); x++) {
            for (int y = room.getMinY(); y <= room.getMaxY(); y++) {
                boolean edge = x == room.getMinX() || x == room.getMaxX()
                        || y == room.getMinY() || y == room.getMaxY();
                if (edge) {
                    for (int z = room.getMinZ(); z <= room.getMaxZ(); z++) {
                        consumer.accept(world.getBlockAt(x, y, z));
                    }
                } else {
                    consumer.accept(world.getBlockAt(x, y, room.getMinZ()));
                    if (room.getMaxZ() != room.getMinZ()) {
                        consumer.accept(world.getBlockAt(x, y, room.getMaxZ()));
                    }
                }
            }
        }
    }

    private void placeShell(Room room) {
        removeShell(room);
        Material mat = room.getBlockMaterial();
        forEachShellBlock(room, block -> {
            if (block.getType().isAir()) {
                block.setType(mat, false);
                room.getPlacedBlocks().add(block);
            }
        });
    }

    private void removeShell(Room room) {
        Material mat = room.getBlockMaterial();
        for (Block b : room.getPlacedBlocks()) {
            if (b.getType() == mat) {
                b.setType(Material.AIR, false);
            }
        }
        room.getPlacedBlocks().clear();
    }

    /** Changes the door block of a room (and swaps the blocks if the door is currently closed). */
    public void setBlockMaterial(Room room, Material newMat) {
        Material old = room.getBlockMaterial();
        room.setBlockMaterial(newMat);
        for (Block b : room.getPlacedBlocks()) {
            if (b.getType() == old) {
                b.setType(newMat, false);
            }
        }
        save();
    }
                      }
