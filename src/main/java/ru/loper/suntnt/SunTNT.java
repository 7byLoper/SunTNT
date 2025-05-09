package ru.loper.suntnt;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.loper.suntnt.commands.TNTCommand;
import ru.loper.suntnt.listeners.TNTListener;
import ru.loper.suntnt.tnt.TNTManager;
import ru.loper.suntnt.utils.PluginConfigManager;

import java.util.Optional;

@Getter
public final class SunTNT extends JavaPlugin {

    @Getter
    private static SunTNT instance;
    private TNTManager tntManager;
    private PluginConfigManager configManager;
    private boolean protectionStonesStatus = true;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("SunProtectionStones") == null) {
            protectionStonesStatus = false;
            getLogger().warning("SunProtectionStones отсутствует, некоторые функции плагина отключены. Приобрести плагина можно в нашей студии t.me/bySunDev");
        }

        configManager = new PluginConfigManager(this);
        tntManager = new TNTManager(this);

        Bukkit.getPluginManager().registerEvents(new TNTListener(this), this);
        Optional.ofNullable(getCommand("suntnt")).orElseThrow().setExecutor(new TNTCommand(this));
    }

}
