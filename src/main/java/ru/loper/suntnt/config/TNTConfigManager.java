package ru.loper.suntnt.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.ConfigManager;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.utils.Colorize;

@Getter
public class TNTConfigManager extends ConfigManager {
    private String giveSenderMessage;
    private String givePlayerMessage;
    private String addCustomItemMessage;
    private String removeCustomItemMessage;
    private String errorCommandMessage;
    private String errorItemMessage;
    private String offlineTargetMessage;
    private String invalidTntMessage;
    private String noPermissionsMessage;

    private boolean disableItemExplosion;
    private boolean disableSpawnerExplosion;

    private boolean defaultCustomNameVisible;
    private String defaultCustomName;

    public TNTConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        plugin.saveDefaultConfig();
        addCustomConfig(new CustomConfig("customItems", plugin));
    }

    @Override
    public void loadValues() {
        giveSenderMessage = getConfigMessage("messages.give-sender");
        givePlayerMessage = getConfigMessage("messages.give-player");
        addCustomItemMessage = getConfigMessage("messages.add-custom-item");
        removeCustomItemMessage = getConfigMessage("messages.remove-custom-item");
        errorCommandMessage = getConfigMessage("messages.error-command");
        errorItemMessage = getConfigMessage("messages.error-item");
        offlineTargetMessage = getConfigMessage("messages.offline-target");
        invalidTntMessage = getConfigMessage("messages.invalid-tnt");
        noPermissionsMessage = getConfigMessage("messages.no-permissions");

        ConfigurationSection settingsSection = plugin.getConfig().getConfigurationSection("settings");
        if (settingsSection != null) {
            disableItemExplosion = settingsSection.getBoolean("disable_item_explosion", false);
            disableSpawnerExplosion = settingsSection.getBoolean("disable_spawner_explosion", false);
            defaultCustomNameVisible = settingsSection.getBoolean("default_custom_name.visible", false);
            defaultCustomName = Colorize.parse(settingsSection.getString("default_custom_name.name", ""));
        } else {
            disableItemExplosion = false;
            defaultCustomNameVisible = false;
            defaultCustomName = "";
        }
    }

    public String getConfigMessage(String path) {
        return Colorize.parse(plugin.getConfig().getString(path, "unknown path " + path));
    }

}
