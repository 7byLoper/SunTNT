package ru.loper.suntnt.manager;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.modules.CustomTNT;
import ru.loper.suntnt.utils.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class TNTManager {
    private final SunTNT plugin;
    private final Map<String, CustomTNT> customTNTs;

    private final NamespacedKey tntTypeKey;

    public TNTManager(SunTNT plugin) {
        this.plugin = plugin;

        this.tntTypeKey = new NamespacedKey(plugin, "TNTType");
        this.customTNTs = new HashMap<>();

        reload();
    }

    public void reload() {
        customTNTs.clear();
        File directory = new File(plugin.getDataFolder(), "/tnts");
        if (!directory.exists()) {
            plugin.saveResource("tnts/EXAMPLE_TNT.yml", true);
        }

        for (File file : Utils.getFiles(directory)) {
            if (!file.getName().endsWith(".yml")) continue;

            CustomConfig config = new CustomConfig(file);
            customTNTs.put(config.getConfig().getString("name", "default"), new CustomTNT(config, this));
        }
    }

    public CustomTNT getCustomTNT(String name) {
        return customTNTs.get(name);
    }

    public CustomTNT getCustomTNT(Entity entity) {
        if (!entity.hasMetadata("TNTType")) {
            return null;
        }
        String tntType = entity.getMetadata("TNTType").get(0).asString();
        return customTNTs.get(tntType);
    }

    public List<String> getCustomTNTsName() {
        return customTNTs.keySet().stream().toList();
    }
}
