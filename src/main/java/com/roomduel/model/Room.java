package com.roomduel.model;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Room {

    public enum State { OPEN, CLOSING, CLOSED, UNLOCKING }

    private final String name;
    private final String worldName;
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    private Material blockMaterial = Material.GLASS;
    private Double lockSeconds;   // null = use global value
    private Double unlockSeconds; // null = use global value

    // runtime data (not saved)
    private State state = State.OPEN;
    private final Set<UUID> players = new LinkedHashSet<>();
    private final Set<Block> placedBlocks = new LinkedHashSet<>();
    private BukkitTask task;

    public Room(String name, String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.name = name;
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public boolean contains(World world, int x, int y, int z) {
        return world != null
                && world.getName().equals(worldName)
                && x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public Material getBlockMaterial() { return blockMaterial; }
    public void setBlockMaterial(Material blockMaterial) { this.blockMaterial = blockMaterial; }

    public Double getLockSeconds() { return lockSeconds; }
    public void setLockSeconds(Double lockSeconds) { this.lockSeconds = lockSeconds; }

    public Double getUnlockSeconds() { return unlockSeconds; }
    public void setUnlockSeconds(Double unlockSeconds) { this.unlockSeconds = unlockSeconds; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public Set<UUID> getPlayers() { return players; }
    public Set<Block> getPlacedBlocks() { return placedBlocks; }

    public BukkitTask getTask() { return task; }
    public void setTask(BukkitTask task) { this.task = task; }
}
