package ru.loper.suntnt.api.model;

import java.util.EnumMap;
import java.util.Map;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.suncore.api.colorize.StringColorize;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suntnt.manager.TNTManager;

@Data
public class CustomTNT {
    private final String name;

    private final int spawnerChance;
    private final int spawnerMobSaveChance;
    private final int goldSpawnerChance;
    private final int mysteriousSpawnerChance;
    private final int fuseTicks;
    private final int iceRadius;
    private final int waterChance;
    private final ExplosiveMode explosiveMode;
    private final Integer explosivePower;
    private final int cutDamage;
    private final boolean autoIgnite;
    private final Map<Material, BreakableBlockSettings> breakableBlocks;
    private final Map<Material, TransformableBlockSettings> transformableBlocks;

    private final boolean breakPSRegion;
    private final boolean ice;

    private final long iceDelay;

    private final boolean customNameVisible;
    private final String customName;

    private final ItemBuilder tntBuilder;
    private final ItemBuilder spawnerForm;
    private final ConfigurationSection recipeSection;

    public CustomTNT(CustomConfig tntConfig, TNTManager tntManager) {
        FileConfiguration config = tntConfig.getConfig();

        name = config.getString("name", "default");
        ConfigurationSection options = config.getConfigurationSection("options");
        ConfigurationSection spawnerOptions = options == null ? null : options.getConfigurationSection("spawner");
        spawnerChance = getSectionInt(spawnerOptions, "chance", 0);
        spawnerMobSaveChance = getSectionInt(spawnerOptions, "mob-save-chance", 0);
        goldSpawnerChance = getSectionInt(spawnerOptions, "gold-chance", 0);
        mysteriousSpawnerChance = getSectionInt(spawnerOptions, "mysterious-chance", 0);
        explosiveMode = ExplosiveMode.from(options == null ? null : options.getString("explosive-mode"));
        explosivePower = options != null && options.contains("explosive-power")
                ? Math.max(0, options.getInt("explosive-power"))
                : null;
        cutDamage = options == null ? 0 : Math.max(0, options.getInt("cut-damage", 0));
        autoIgnite = options != null && options.getBoolean("auto-ignite", false);
        fuseTicks = options != null && options.contains("explosive-interval")
                ? Math.max(0, options.getInt("explosive-interval")) * 20
                : 80;
        ConfigurationSection iceOptions = options == null ? null : options.getConfigurationSection("ice");
        ice = getSectionBoolean(iceOptions, "enabled", false);
        iceRadius = getSectionInt(iceOptions, "radius", 0);
        iceDelay = getSectionInt(iceOptions, "delay", 0);
        waterChance = getSectionInt(options, "water-chance", 0);
        breakPSRegion = getSectionBoolean(options, "break-ps-region", false);
        breakableBlocks = readBreakableBlocks(config.getConfigurationSection("breakable-blocks"));
        transformableBlocks = readTransformableBlocks(config.getConfigurationSection("transformable-blocks"));

        ConfigurationSection builderSection = config.getConfigurationSection("item");
        if (builderSection == null) {
            tntBuilder = new ItemBuilder(Material.TNT);
        } else {
            tntBuilder = ItemBuilder.fromConfig(builderSection);
        }
        tntBuilder.namespacedKey(tntManager.getTntTypeKey(), PersistentDataType.STRING, name);

        ConfigurationSection spawnerBuilderSection = config.getConfigurationSection("spawner_form");
        if (spawnerBuilderSection == null) {
            spawnerForm = new ItemBuilder(Material.SPAWNER).name("&f{mob}");
        } else {
            spawnerForm = ItemBuilder.fromConfig(spawnerBuilderSection);
        }

        ConfigurationSection customNameSection = config.getConfigurationSection("custom_name");
        if (customNameSection != null) {
            customNameVisible = customNameSection.getBoolean("visible", false);
            customName = StringColorize.parse(customNameSection.getString("name"));
        } else {
            customNameVisible = false;
            customName = "";
        }

        recipeSection = config.getConfigurationSection("recipe");
    }

    public ItemStack getItemStack() {
        return tntBuilder.amount(1).build().clone();
    }

    public int getExplosionPower() {
        return explosivePower == null ? 4 : explosivePower;
    }

    public boolean hasExplosivePower() {
        return explosivePower != null;
    }

    private int getSectionInt(ConfigurationSection section, String path, int defaultValue) {
        return section == null ? defaultValue : section.getInt(path, defaultValue);
    }

    private boolean getSectionBoolean(ConfigurationSection section, String path, boolean defaultValue) {
        return section != null && section.getBoolean(path, defaultValue);
    }

    private Map<Material, BreakableBlockSettings> readBreakableBlocks(ConfigurationSection section) {
        Map<Material, BreakableBlockSettings> result = new EnumMap<>(Material.class);
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            ConfigurationSection settings = section.getConfigurationSection(key);
            if (material == null || !material.isBlock() || settings == null) continue;

            int radius = settings.contains("radius") ? Math.max(0, settings.getInt("radius")) : getExplosionPower();
            result.put(
                    material,
                    new BreakableBlockSettings(
                            Math.min(radius, getExplosionPower()),
                            clampPercent(settings.getInt("chance", 100)),
                            clampPercent(settings.getInt("drop-chance", 100))));
        }
        return result;
    }

    private Map<Material, TransformableBlockSettings> readTransformableBlocks(ConfigurationSection section) {
        Map<Material, TransformableBlockSettings> result = new EnumMap<>(Material.class);
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            ConfigurationSection settings = section.getConfigurationSection(key);
            Material target = settings == null ? null : Material.matchMaterial(settings.getString("to", "AIR"));
            if (material == null || !material.isBlock() || target == null || !target.isBlock()) continue;

            int radius = settings.contains("radius") ? Math.max(0, settings.getInt("radius")) : getExplosionPower();
            result.put(material, new TransformableBlockSettings(Math.min(radius, getExplosionPower()), target));
        }
        return result;
    }

    private int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    @Data
    public static class BreakableBlockSettings {
        private final int radius;
        private final int chance;
        private final int dropChance;
    }

    @Data
    public static class TransformableBlockSettings {
        private final int radius;
        private final Material target;
    }

    public enum ExplosiveMode {
        VANILLA,
        BALL,
        CUBE;

        private static ExplosiveMode from(String value) {
            if (value == null) return VANILLA;
            try {
                return valueOf(value.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return VANILLA;
            }
        }
    }
}
