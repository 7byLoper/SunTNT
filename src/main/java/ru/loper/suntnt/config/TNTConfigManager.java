package ru.loper.suntnt.config;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.colorize.StringColorize;
import ru.loper.suncore.api.config.ConfigManager;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suntnt.api.enums.RuneType;
import ru.loper.suntnt.api.modules.Rune;

import java.util.*;
import java.util.stream.Collectors;

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

    private String customItemsUsageMessage;
    private String customItemsNameRequiredMessage;
    private String customItemsNotExistsMessage;
    private String customItemsRemoveSuccessMessage;
    private String customItemsPlayerOnlyMessage;
    private String customItemsHoldItemMessage;
    private String customItemsAddSuccessMessage;
    private String customItemsConfigErrorMessage;
    private String customItemsGiveSuccessMessage;

    private String giveUsageMessage;
    private String giveInvalidAmountMessage;

    private String giveGunUsageMessage;
    private String giveGunInvalidAmountMessage;
    private String giveGunSuccessMessage;

    private String runeGiveUsageMessage;
    private String runeNotFoundMessage;
    private String runeGiveSenderMessage;
    private String runeGivePlayerMessage;
    private String invalidAmountMessage;
    private String playerOnlyCommandMessage;

    private boolean disableItemExplosion;
    private boolean disableSpawnerExplosion;
    private boolean disableProtectionBlocksExplosion;

    private boolean defaultCustomNameVisible;
    private String defaultCustomName;

    private Map<EntityType, String> entityTranslations;

    private NamespacedKey tntGunNamespacedKey;
    private ItemBuilder tntGunBuilder;
    private boolean tntGunRegionBreak;
    private double tntGunSpeed;

    private NamespacedKey blockedItemKey;
    private List<Integer> tntGunBlockedSlots;
    private ItemBuilder tntGunBlockedItem;

    private Map<String, Rune> tntRunes;
    private String runesInfoLore;

    public TNTConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        plugin.saveDefaultConfig();

        addCustomConfig(new CustomConfig("customItems", true, plugin));
        addCustomConfig(new CustomConfig("translation", plugin));

        blockedItemKey = new NamespacedKey(plugin, "blocked-item");
        tntGunNamespacedKey = new NamespacedKey(plugin, "tnt-gun");
    }

    @Override
    public void loadValues() {
        entityTranslations = new HashMap<>();

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

        loadMessages();

        ConfigurationSection settingsSection = plugin.getConfig().getConfigurationSection("settings");
        if (settingsSection != null) {
            disableItemExplosion = settingsSection.getBoolean("disable_item_explosion", false);
            disableSpawnerExplosion = settingsSection.getBoolean("disable_spawner_explosion", false);
            disableProtectionBlocksExplosion = settingsSection.getBoolean("disable_protection_blocks_explosion", false);
            defaultCustomNameVisible = settingsSection.getBoolean("default_custom_name.visible", false);
            defaultCustomName = StringColorize.parse(settingsSection.getString("default_custom_name.name", ""));
        } else {
            disableItemExplosion = false;
            defaultCustomNameVisible = false;
            disableProtectionBlocksExplosion = false;
            defaultCustomName = "";
        }

        ConfigurationSection tntGunSection = plugin.getConfig().getConfigurationSection("tnt_gun");
        if (tntGunSection != null) {
            tntGunSpeed = tntGunSection.getDouble("speed");
            tntGunRegionBreak = tntGunSection.getBoolean("region_break");

            ConfigurationSection itemBuilderSection = tntGunSection.getConfigurationSection("item");
            tntGunBuilder = itemBuilderSection == null ? new ItemBuilder(Material.DISPENSER) : ItemBuilder.fromConfig(itemBuilderSection);
            tntGunBuilder.namespacedKey(tntGunNamespacedKey, PersistentDataType.STRING, "value");

            ConfigurationSection tntGunBlockedItemSection = tntGunSection.getConfigurationSection("blocked_item");
            tntGunBlockedItem = tntGunBlockedItemSection == null ? new ItemBuilder(Material.RED_STAINED_GLASS_PANE) : ItemBuilder.fromConfig(tntGunBlockedItemSection);
            tntGunBlockedItem.namespacedKey(blockedItemKey, PersistentDataType.BYTE, (byte) 0);

            tntGunBlockedSlots = tntGunSection.getIntegerList("blocked_slots");
        } else {
            tntGunSpeed = 1.0;
            tntGunRegionBreak = true;
            tntGunBuilder = new ItemBuilder(Material.DISPENSER);
            tntGunBuilder.namespacedKey(tntGunNamespacedKey, PersistentDataType.STRING, "value");

            tntGunBlockedItem = new ItemBuilder(Material.RED_STAINED_GLASS_PANE);
            tntGunBlockedItem.namespacedKey(blockedItemKey, PersistentDataType.BYTE, (byte) 0);

            tntGunBlockedSlots = List.of(0, 1, 2, 3, 5, 6, 7, 8);
        }

        runesInfoLore = StringColorize.parse(plugin.getConfig().getString("runes_info_lore"));

        tntRunes = new HashMap<>();
        ConfigurationSection runesSection = plugin.getConfig().getConfigurationSection("runes");
        if (runesSection != null) {
            for (String key : runesSection.getKeys(false)) {
                ConfigurationSection runeSection = runesSection.getConfigurationSection(key);
                if (runeSection == null) {
                    continue;
                }

                Rune rune = createRune(runeSection);
                if (rune == null) {
                    continue;
                }

                rune.loadValues(runeSection);
                tntRunes.put(rune.getName(), rune);
            }
        }
    }

    private void loadMessages() {
        giveSenderMessage = getConfigMessage("messages.give-sender");
        givePlayerMessage = getConfigMessage("messages.give-player");
        addCustomItemMessage = getConfigMessage("messages.add-custom-item");
        removeCustomItemMessage = getConfigMessage("messages.remove-custom-item");
        errorCommandMessage = getConfigMessage("messages.error-command");
        errorItemMessage = getConfigMessage("messages.error-item");
        offlineTargetMessage = getConfigMessage("messages.offline-target");
        invalidTntMessage = getConfigMessage("messages.invalid-tnt");
        noPermissionsMessage = getConfigMessage("messages.no-permissions");

        customItemsUsageMessage = getConfigMessage("messages.custom-items-usage");
        customItemsNameRequiredMessage = getConfigMessage("messages.custom-items-name-required");
        customItemsNotExistsMessage = getConfigMessage("messages.custom-items-not-exists");
        customItemsRemoveSuccessMessage = getConfigMessage("messages.custom-items-remove-success");
        customItemsPlayerOnlyMessage = getConfigMessage("messages.custom-items-player-only");
        customItemsHoldItemMessage = getConfigMessage("messages.custom-items-hold-item");
        customItemsAddSuccessMessage = getConfigMessage("messages.custom-items-add-success");
        customItemsConfigErrorMessage = getConfigMessage("messages.custom-items-config-error");
        customItemsGiveSuccessMessage = getConfigMessage("messages.custom-items-give-success");

        giveUsageMessage = getConfigMessage("messages.give-usage");
        giveInvalidAmountMessage = getConfigMessage("messages.give-invalid-amount");

        giveGunUsageMessage = getConfigMessage("messages.givegun-usage");
        giveGunInvalidAmountMessage = getConfigMessage("messages.givegun-invalid-amount");
        giveGunSuccessMessage = getConfigMessage("messages.givegun-success");

        runeGiveUsageMessage = getConfigMessage("messages.rune-give-usage");
        runeNotFoundMessage = getConfigMessage("messages.rune-not-found");
        runeGiveSenderMessage = getConfigMessage("messages.rune-give-sender");
        runeGivePlayerMessage = getConfigMessage("messages.rune-give-player");
        invalidAmountMessage = getConfigMessage("messages.invalid-amount");
        playerOnlyCommandMessage = getConfigMessage("messages.player-only-command");
    }

    public Rune createRune(@NotNull ConfigurationSection section) {
        RuneType runeType = RuneType.getByName(section.getString("rune_type", ""));
        if (runeType == null) {
            return null;
        }

        return runeType.createRune();
    }

    public Set<Rune> getRunesFromTNT(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return new HashSet<>();
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(Rune.getTntRuneKey(), PersistentDataType.STRING)) {
            return new HashSet<>();
        }

        String runesData = container.get(Rune.getTntRuneKey(), PersistentDataType.STRING);
        return getRunesFromData(runesData);
    }

    public Set<Rune> getRunesFromData(String runesData) {
        if (runesData == null || runesData.isEmpty()) {
            return Collections.emptySet();
        }

        return Arrays.stream(runesData.split(","))
                .map(tntRunes::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Rune getRune(String runeName) {
        return tntRunes.get(runeName);
    }

    public String getConfigMessage(String path) {
        return StringColorize.parse(plugin.getConfig().getString(path, "unknown path " + path));
    }

    public CustomConfig getTranslationConfig() {
        return getCustomConfig("translation");
    }

    public String getEntityTranslation(EntityType entityType) {
        return entityTranslations.getOrDefault(entityType, entityType.name().toLowerCase());
    }

    public boolean isTNTGunItem(ItemStack itemStack) {
        return itemStack != null &&
                itemStack.hasItemMeta() &&
                itemStack.getItemMeta().getPersistentDataContainer().has(tntGunNamespacedKey, PersistentDataType.STRING);
    }

}
