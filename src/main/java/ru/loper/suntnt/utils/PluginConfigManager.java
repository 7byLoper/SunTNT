package ru.loper.suntnt.utils;

import lombok.Getter;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.ConfigManager;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.utils.Colorize;

@Getter
public class PluginConfigManager extends ConfigManager {
    private String giveSender;
    private String givePlayer;
    private String addCustomItem;
    private String removeCustomItem;
    private String errorCommand;
    private String errorItem;
    private String offlineTarget;
    private String invalidTnt;
    private String noPermissions;

    public PluginConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        addCustomConfig(new CustomConfig("customItems", plugin));
    }

    @Override
    public void loadValues() {
        giveSender = getConfigMessage("messages.give-sender");
        givePlayer = getConfigMessage("messages.give-player");
        addCustomItem = getConfigMessage("messages.add-custom-item");
        removeCustomItem = getConfigMessage("messages.remove-custom-item");
        errorCommand = getConfigMessage("messages.error-command");
        errorItem = getConfigMessage("messages.error-item");
        offlineTarget = getConfigMessage("messages.offline-target");
        invalidTnt = getConfigMessage("messages.invalid-tnt");
        noPermissions = getConfigMessage("messages.no-permissions");
    }

    public String getConfigMessage(String path) {
        return Colorize.parse(plugin.getConfig().getString(path, "unknown path " + path));
    }

}
