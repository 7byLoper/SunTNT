package ru.loper.suntnt.tnt;

import java.io.File;
import java.util.HashMap;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.utils.Utils;

public class TNTManager {
    private final SunTNT plugin;
    private final HashMap<String, TNT> tntTypes = new HashMap<>();

    public TNTManager(SunTNT plugin) {
        this.plugin = plugin;
        this.init();
    }

    public void init() {
        tntTypes.clear();
        File file = new File(plugin.getDataFolder(),"/tnts");
        if(!file.exists()){
            file.mkdir();
            plugin.saveResource("tnts/TNTA.yml", true);
            plugin.saveResource("tnts/TNTICE.yml", true);
            plugin.saveResource("tnts/TNTAQUA.yml", true);
            plugin.saveResource("tnts/TNTSPAWNER.yml", true);
        }
        for(File file1 : Utils.getFiles(file)){
            if(!file1.getName().endsWith(".yml")) continue;
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.load(file1);
                tntTypes.put(config.getString("name"),new TNT(config));
            } catch (Exception e){
                throw new RuntimeException("Error yml load "+ file1.getName(), e);
            }
        }
    }

    public TNT getTNT(String name) {
        return this.tntTypes.get(name);
    }

    public HashMap<String, TNT> getTntTypes() {
        return this.tntTypes;
    }
}
