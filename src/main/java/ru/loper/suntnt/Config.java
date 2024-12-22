package ru.loper.suntnt;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import ru.loper.suntnt.utils.Colorize;

import java.io.File;
import java.io.IOException;

public class Config {
    private final FileConfiguration config;
    private final File file;
    private final String name;

    public Config(String name, boolean saveResources, Plugin plugin) {
        this.name = name;
        file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            if (saveResources) {
                plugin.saveResource(name, true);
            } else {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reloadConfig() {
        try {
            config.load(file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            Bukkit.getLogger().warning(name + " не найден!");
        }
    }

    public String configMessage(String path) {
        String message = config.getString(path);
        if (message == null) {
            SunTNT.getInstance().getLogger().warning("В конфиге " + name + " отсутствует строка " + path);
            return "ERROR 404";
        }
        message = Colorize.format(message);
        return message;
    }
}
