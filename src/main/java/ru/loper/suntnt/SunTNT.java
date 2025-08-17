package ru.loper.suntnt;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.loper.suntnt.commands.TNTCommand;
import ru.loper.suntnt.config.TNTConfigManager;
import ru.loper.suntnt.listeners.TNTPossibilityListener;
import ru.loper.suntnt.listeners.TNTSpawnListener;
import ru.loper.suntnt.manager.TNTManager;

import java.util.Optional;

@Getter
public final class SunTNT extends JavaPlugin {

    @Getter
    private static SunTNT instance;
    private TNTManager tntManager;
    private TNTConfigManager configManager;
    private boolean protectionStonesStatus = true;
    private boolean holyLiteUtilsStatus = true;

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("SunProtectionStones") == null) {
            protectionStonesStatus = false;
            getLogger().warning("SunProtectionStones отсутствует, некоторые функции плагина отключены. Приобрести плагина можно в нашей студии t.me/bySunDev");
        }

        if (Bukkit.getPluginManager().getPlugin("HolyLiteUtils") == null) {
            holyLiteUtilsStatus = false;
        }

        configManager = new TNTConfigManager(this);
        tntManager = new TNTManager(this);

        Bukkit.getPluginManager().registerEvents(new TNTPossibilityListener(this, configManager), this);
        Bukkit.getPluginManager().registerEvents(new TNTSpawnListener(this, configManager), this);

        Optional.ofNullable(getCommand("suntnt"))
                .orElseThrow(() -> new IllegalStateException("Command 'suntnt' not found!"))
                .setExecutor(new TNTCommand(this));
    }

}
