package dev.sharpennd.guestpass;

import dev.sharpennd.guestpass.commands.GuestPassCommand;
import dev.sharpennd.guestpass.listeners.GuestListener;
import dev.sharpennd.guestpass.managers.GuestManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuestPass extends JavaPlugin {

    private GuestManager guestManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        guestManager = new GuestManager(this);

        getServer().getPluginManager().registerEvents(new GuestListener(this), this);

        GuestPassCommand cmd = new GuestPassCommand(this);
        getCommand("guestpass").setExecutor(cmd);
        getCommand("guestpass").setTabCompleter(cmd);

        getLogger().info("GuestPass enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("GuestPass disabled.");
    }

    public GuestManager getGuestManager() {
        return guestManager;
    }
}
