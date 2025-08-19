package ru.loper.suntnt.config;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.ConfigManager;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.items.ItemBuilder;
import ru.loper.suncore.utils.Colorize;

import java.util.HashMap;
import java.util.Map;

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

    private Map<EntityType, String> entityTranslations;

    private NamespacedKey tntGunNamespacedKey;
    private ItemBuilder tntGunBuilder;
    private boolean tntGunRegionBreak;
    private double tntGunSpeed;

    public TNTConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        plugin.saveDefaultConfig();
        addCustomConfig(new CustomConfig("customItems", plugin));
        addCustomConfig(new CustomConfig("translation", plugin));
    }

    @Override
    public void loadValues() {
        entityTranslations = new HashMap<>();
        tntGunNamespacedKey = new NamespacedKey(plugin, "tnt-gun");

        ConfigurationSection entityTranslationsSection = getTranslationConfig().getConfig().getConfigurationSection("entities");
        if (entityTranslationsSection != null) {
            for (String key : entityTranslationsSection.getKeys(false)) {
                try {
                    EntityType entityType = EntityType.valueOf(key);
                    entityTranslations.put(entityType, entityTranslationsSection.getString(key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid entity type: " + key);
                }
            }
        }

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

        ConfigurationSection tntGunSection = plugin.getConfig().getConfigurationSection("tnt_gun");
        if (tntGunSection != null) {
            tntGunSpeed = tntGunSection.getDouble("speed");
            tntGunRegionBreak = tntGunSection.getBoolean("region_break");

            ConfigurationSection itemBuilderSection = tntGunSection.getConfigurationSection("item");
            tntGunBuilder = itemBuilderSection == null ? new ItemBuilder(Material.DISPENSER) : ItemBuilder.fromConfig(itemBuilderSection);
            tntGunBuilder.namespacedKey(tntGunNamespacedKey, PersistentDataType.STRING, "value");
        } else {
            tntGunSpeed = 1.0;
            tntGunRegionBreak = true;
            tntGunBuilder = new ItemBuilder(Material.DISPENSER);
            tntGunBuilder.namespacedKey(tntGunNamespacedKey, PersistentDataType.STRING, "value");
        }
    }

    public String getConfigMessage(String path) {
        return Colorize.parse(plugin.getConfig().getString(path, "unknown path " + path));
    }

    public CustomConfig getTranslationConfig() {
        return getCustomConfig("translation");
    }

    public String getEntityTranslation(EntityType entityType) {
        return entityTranslations.getOrDefault(entityType, entityType.name());
    }

    public boolean isTNTGunItem(ItemStack itemStack) {
        return itemStack != null &&
               itemStack.hasItemMeta() &&
               itemStack.getItemMeta().getPersistentDataContainer().has(tntGunNamespacedKey, PersistentDataType.STRING);
    }

}
