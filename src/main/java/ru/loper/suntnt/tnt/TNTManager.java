package ru.loper.suntnt.tnt;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.utils.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class TNTManager {
    private final SunTNT plugin;
    private final HashMap<String, CustomTNT> customTNTS = new HashMap<>();
    @Getter
    private final NamespacedKey tntTypeKey;

    public TNTManager(SunTNT plugin) {
        tntTypeKey = new NamespacedKey(plugin, "TNTType");
        this.plugin = plugin;
        init();
    }

    public void init() {
        customTNTS.clear();
        File directory = new File(plugin.getDataFolder(), "/tnts");
        if (!directory.exists()) {
            plugin.saveResource("tnts/TNTA.yml", true);
            plugin.saveResource("tnts/TNTICE.yml", true);
            plugin.saveResource("tnts/TNTAQUA.yml", true);
            plugin.saveResource("tnts/TNTSPAWNER.yml", true);
        }

        for (File file : Utils.getFiles(directory)) {
            if (!file.getName().endsWith(".yml")) continue;

            CustomConfig config = new CustomConfig(file);
            customTNTS.put(config.getConfig().getString("name", "default"), new CustomTNT(config, this));
        }
    }

    public CustomTNT getCustomTNT(String name) {
        return customTNTS.get(name);
    }

    public List<String> getCustomTNTsName() {
        return customTNTS.keySet().stream().toList();
    }
}
