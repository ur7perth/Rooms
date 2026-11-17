package com.roomduel;

import com.roomduel.commands.BlockRoomCommand;
import com.roomduel.commands.DeleteRoomCommand;
import com.roomduel.commands.LockRoomCommand;
import com.roomduel.commands.RoomWandCommand;
import com.roomduel.commands.SetRoomCommand;
import com.roomduel.commands.UnlockRoomCommand;
import com.roomduel.listeners.RoomListener;
import com.roomduel.listeners.WandListener;
import com.roomduel.manager.RoomManager;
import com.roomduel.manager.SelectionManager;
import com.roomduel.util.Messages;
import org.bukkit.plugin.java.JavaPlugin;

public final class RoomDuelPlugin extends JavaPlugin {

    public static final String PERMISSION = "roomduel.admin";

    private Messages messages;
    private RoomManager roomManager;
    private SelectionManager selectionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messages = new Messages(this);
        this.roomManager = new RoomManager(this);
        this.selectionManager = new SelectionManager(this);
        this.roomManager.load();
        this.selectionManager.startParticleTask();

        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new RoomListener(this), this);

        getCommand("roomwand").setExecutor(new RoomWandCommand(this));
        getCommand("setroom").setExecutor(new SetRoomCommand(this));
        getCommand("blockroom").setExecutor(new BlockRoomCommand(this));
        getCommand("lockroom").setExecutor(new LockRoomCommand(this));
        getCommand("unlockroom").setExecutor(new UnlockRoomCommand(this));
        getCommand("deleteroom").setExecutor(new DeleteRoomCommand(this));

        getLogger().info("RoomDuel enabled with " + roomManager.getRooms().size() + " room(s).");
    }

    @Override
    public void onDisable() {
        if (roomManager != null) {
            roomManager.shutdown();
        }
    }

    public Messages getMessages() {
        return messages;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }
}
