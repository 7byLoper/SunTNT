package ru.loper.suntnt;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.loper.suntnt.commands.TNTCommand;
import ru.loper.suntnt.listeners.TNTListener;
import ru.loper.suntnt.tnt.TNTManager;

public final class SunTNT extends JavaPlugin {

    private static SunTNT instance;
    private static TNTManager tntManager;
    private static Config customItemsConfig;
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        customItemsConfig = new Config("customItems.yml",true,this);
        tntManager = new TNTManager(this);
        Bukkit.getPluginManager().registerEvents(new TNTListener(this),this);
        getCommand("tnt").setExecutor(new TNTCommand());
    }

    public static SunTNT getInstance() {
        return instance;
    }

    public static TNTManager getTntManager() {
        return tntManager;
    }

    public static Config getCustomItemsConfig() {
        return customItemsConfig;
    }
}
