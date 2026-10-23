package ru.loper.suntnt.config;

import java.util.*;
import java.util.regex.Pattern;
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
import ru.loper.suntnt.api.model.Rune;

@Getter
public class TNTConfigManager extends ConfigManager {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private static final String CUSTOM_ITEMS_CONFIG = "customItems";
    private static final String TRANSLATION_CONFIG = "translation";
    private static final List<Integer> DEFAULT_BLOCKED_SLOTS = List.of(0, 1, 2, 3, 5, 6, 7, 8);

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
    private Map<Material, Material> protectionBlocks;

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

    public TNTConfigManager(final Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        plugin.saveDefaultConfig();

        addCustomConfig(new CustomConfig(CUSTOM_ITEMS_CONFIG, true, plugin));
        addCustomConfig(new CustomConfig(TRANSLATION_CONFIG, plugin));

        blockedItemKey = new NamespacedKey(plugin, "blocked-item");
        tntGunNamespacedKey = new NamespacedKey(plugin, "tnt-gun");
    }

    @Override
    public void loadValues() {
        entityTranslations = new EnumMap<>(EntityType.class);

        final ConfigurationSection entityTranslationsSection =
                getTranslationConfig().getConfig().getConfigurationSection("entities");
        if (entityTranslationsSection != null) {
            for (final String key : entityTranslationsSection.getKeys(false)) {
                try {
                    final EntityType entityType = EntityType.valueOf(key);
                    entityTranslations.put(entityType, entityTranslationsSection.getString(key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid entity type: " + key);
                }
            }
        }

        loadMessages();

        final ConfigurationSection settingsSection = plugin.getConfig().getConfigurationSection("settings");
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

        protectionBlocks = new EnumMap<>(Material.class);
        final ConfigurationSection protectionBlocksSection = plugin.getConfig().getConfigurationSection("protection_blocks");
        if (protectionBlocksSection != null) {
            for (final String key : protectionBlocksSection.getKeys(false)) {
                final Material from = Material.matchMaterial(key);
                final Material to = Material.matchMaterial(protectionBlocksSection.getString(key, ""));
                if (from != null && from.isBlock() && to != null && to.isBlock()) {
                    protectionBlocks.put(from, to);
                }
            }
        }

        final ConfigurationSection tntGunSection = plugin.getConfig().getConfigurationSection("tnt_gun");
        if (tntGunSection != null) {
            tntGunSpeed = tntGunSection.getDouble("speed");
            tntGunRegionBreak = tntGunSection.getBoolean("region_break");

            final ConfigurationSection itemBuilderSection = tntGunSection.getConfigurationSection("item");
            tntGunBuilder = itemBuilderSection == null
                    ? new ItemBuilder(Material.DISPENSER)
                    : ItemBuilder.fromConfig(itemBuilderSection);
            tntGunBuilder.namespacedKey(tntGunNamespacedKey, PersistentDataType.STRING, "value");

            final ConfigurationSection tntGunBlockedItemSection = tntGunSection.getConfigurationSection("blocked_item");
            tntGunBlockedItem = tntGunBlockedItemSection == null
                    ? new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    : ItemBuilder.fromConfig(tntGunBlockedItemSection);
            tntGunBlockedItem.namespacedKey(blockedItemKey, PersistentDataType.BYTE, (byte) 0);

            tntGunBlockedSlots = tntGunSection.getIntegerList("blocked_slots");
        } else {
            tntGunSpeed = 1.0;
            tntGunRegionBreak = true;
            tntGunBuilder = new ItemBuilder(Material.DISPENSER);
            tntGunBuilder.namespacedKey(tntGunNamespacedKey, PersistentDataType.STRING, "value");

            tntGunBlockedItem = new ItemBuilder(Material.RED_STAINED_GLASS_PANE);
            tntGunBlockedItem.namespacedKey(blockedItemKey, PersistentDataType.BYTE, (byte) 0);

            tntGunBlockedSlots = DEFAULT_BLOCKED_SLOTS;
        }

        runesInfoLore = StringColorize.parse(plugin.getConfig().getString("runes_info_lore"));

        tntRunes = new HashMap<>();
        final ConfigurationSection runesSection = plugin.getConfig().getConfigurationSection("runes");
        if (runesSection != null) {
            for (final String key : runesSection.getKeys(false)) {
                final ConfigurationSection runeSection = runesSection.getConfigurationSection(key);
                if (runeSection == null) {
                    continue;
                }

                final Rune rune = createRune(runeSection);
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

    public Rune createRune(final @NotNull ConfigurationSection section) {
        final RuneType runeType = RuneType.getByName(section.getString("rune_type", ""));
        if (runeType == null) {
            return null;
        }

        return runeType.createRune();
    }

    public Set<Rune> getRunesFromTNT(final ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Collections.emptySet();
        }

        final ItemMeta meta = item.getItemMeta();
        final PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(Rune.getTntRuneKey(), PersistentDataType.STRING)) {
            return Collections.emptySet();
        }

        final String runesData = container.get(Rune.getTntRuneKey(), PersistentDataType.STRING);
        return getRunesFromData(runesData);
    }

    public Set<Rune> getRunesFromData(final String runesData) {
        if (runesData == null || runesData.isEmpty()) {
            return Collections.emptySet();
        }

        final Set<Rune> runes = new HashSet<>();
        for (final String runeName : COMMA_PATTERN.split(runesData)) {
            final Rune rune = tntRunes.get(runeName);
            if (rune != null) {
                runes.add(rune);
            }
        }

        return Collections.unmodifiableSet(runes);
    }

    public Rune getRune(final String runeName) {
        return tntRunes.get(runeName);
    }

    public String getConfigMessage(final String path) {
        return StringColorize.parse(plugin.getConfig().getString(path, "unknown path " + path));
    }

    public CustomConfig getTranslationConfig() {
        return getCustomConfig(TRANSLATION_CONFIG);
    }

    public String getEntityTranslation(final EntityType entityType) {
        return entityTranslations.getOrDefault(entityType, entityType.name().toLowerCase(Locale.ROOT));
    }

    public boolean isTNTGunItem(final ItemStack itemStack) {
        return itemStack != null
                && itemStack.hasItemMeta()
                && itemStack.getItemMeta().getPersistentDataContainer().has(tntGunNamespacedKey, PersistentDataType.STRING);
    }
}
