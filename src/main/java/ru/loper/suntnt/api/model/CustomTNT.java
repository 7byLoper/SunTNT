package ru.loper.suntnt.api.model;

import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.suncore.api.colorize.StringColorize;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suntnt.manager.TNTManager;

@Getter
public class CustomTNT {

    private static final int DEFAULT_FUSE_TICKS = 80;
    private static final int DEFAULT_EXPLOSION_POWER = 4;
    private static final int TICKS_PER_SECOND = 20;

    private final String name;
    private final int spawnerChance;
    private final int spawnerMobSaveChance;
    private final int goldSpawnerChance;
    private final int mysteriousSpawnerChance;
    private final int fuseTicks;
    private final int iceRadius;
    private final int waterChance;
    private final boolean ignoreWater;
    private final boolean preserveUnbreakableBlocks;
    private final boolean dropBlocks;
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

    public CustomTNT(final CustomConfig tntConfig, final TNTManager tntManager) {
        final FileConfiguration config = tntConfig.getConfig();

        this.name = config.getString("name", "default");
        final ConfigurationSection options = config.getConfigurationSection("options");
        final ConfigurationSection spawnerOptions = options == null ? null : options.getConfigurationSection("spawner");

        this.spawnerChance = getSectionInt(spawnerOptions, "chance", 0);
        this.spawnerMobSaveChance = getSectionInt(spawnerOptions, "mob-save-chance", 0);
        this.goldSpawnerChance = getSectionInt(spawnerOptions, "gold-chance", 0);
        this.mysteriousSpawnerChance = getSectionInt(spawnerOptions, "mysterious-chance", 0);
        this.explosiveMode = ExplosiveMode.from(options == null ? null : options.getString("explosive-mode"));
        this.explosivePower = options != null && options.contains("explosive-power")
                ? Math.max(0, options.getInt("explosive-power"))
                : null;
        this.cutDamage = options == null ? 0 : Math.max(0, options.getInt("cut-damage", 0));
        this.autoIgnite = options != null && options.getBoolean("auto-ignite", false);
        this.fuseTicks = options != null && options.contains("explosive-interval")
                ? Math.max(0, options.getInt("explosive-interval")) * TICKS_PER_SECOND
                : DEFAULT_FUSE_TICKS;

        final ConfigurationSection iceOptions = options == null ? null : options.getConfigurationSection("ice");
        this.ice = getSectionBoolean(iceOptions, "enabled", false);
        this.iceRadius = getSectionInt(iceOptions, "radius", 0);
        this.iceDelay = getSectionInt(iceOptions, "delay", 0);
        this.waterChance = getSectionInt(options, "water-chance", 0);
        this.ignoreWater = getSectionBoolean(options, "ignore-water", false);
        this.preserveUnbreakableBlocks = getSectionBoolean(options, "preserve-unbreakable-blocks", false);
        this.dropBlocks = getSectionBoolean(options, "drop-blocks", true);
        this.breakPSRegion = getSectionBoolean(options, "break-ps-region", false);

        final int effectiveRadius = this.explosivePower == null ? DEFAULT_EXPLOSION_POWER : this.explosivePower;
        this.breakableBlocks = readBreakableBlocks(config.getConfigurationSection("breakable-blocks"), effectiveRadius);
        this.transformableBlocks = readTransformableBlocks(config.getConfigurationSection("transformable-blocks"), effectiveRadius);

        final ConfigurationSection builderSection = config.getConfigurationSection("item");
        this.tntBuilder = builderSection == null
                ? new ItemBuilder(Material.TNT)
                : ItemBuilder.fromConfig(builderSection);
        this.tntBuilder.namespacedKey(tntManager.getTntTypeKey(), PersistentDataType.STRING, this.name);

        final ConfigurationSection spawnerBuilderSection = config.getConfigurationSection("spawner_form");
        this.spawnerForm = spawnerBuilderSection == null
                ? new ItemBuilder(Material.SPAWNER).name("&f{mob}")
                : ItemBuilder.fromConfig(spawnerBuilderSection);

        final ConfigurationSection customNameSection = config.getConfigurationSection("custom_name");
        if (customNameSection != null) {
            this.customNameVisible = customNameSection.getBoolean("visible", false);
            this.customName = StringColorize.parse(customNameSection.getString("name"));
        } else {
            this.customNameVisible = false;
            this.customName = "";
        }

        this.recipeSection = config.getConfigurationSection("recipe");
    }

    public ItemStack getItemStack() {
        return tntBuilder.amount(1).build().clone();
    }

    public int getExplosionPower() {
        return explosivePower == null ? DEFAULT_EXPLOSION_POWER : explosivePower;
    }

    public boolean hasExplosivePower() {
        return explosivePower != null;
    }

    private static int getSectionInt(final ConfigurationSection section, final String path, final int defaultValue) {
        return section == null ? defaultValue : section.getInt(path, defaultValue);
    }

    private static boolean getSectionBoolean(final ConfigurationSection section, final String path, final boolean defaultValue) {
        return section != null && section.getBoolean(path, defaultValue);
    }

    private static Map<Material, BreakableBlockSettings> readBreakableBlocks(
            final ConfigurationSection section, final int defaultRadius) {
        final Map<Material, BreakableBlockSettings> result = new EnumMap<>(Material.class);
        if (section == null) {
            return result;
        }

        for (final String key : section.getKeys(false)) {
            final Material material = Material.matchMaterial(key);
            final ConfigurationSection settings = section.getConfigurationSection(key);
            if (material == null || !material.isBlock() || settings == null) {
                continue;
            }

            final int radius = settings.contains("radius") ? Math.max(0, settings.getInt("radius")) : defaultRadius;
            result.put(
                    material,
                    new BreakableBlockSettings(
                            Math.min(radius, defaultRadius),
                            clampPercent(settings.getInt("chance", 100)),
                            clampPercent(settings.getInt("drop-chance", 100))));
        }

        return result;
    }

    private static Map<Material, TransformableBlockSettings> readTransformableBlocks(
            final ConfigurationSection section, final int defaultRadius) {
        final Map<Material, TransformableBlockSettings> result = new EnumMap<>(Material.class);
        if (section == null) {
            return result;
        }

        for (final String key : section.getKeys(false)) {
            final Material material = Material.matchMaterial(key);
            final ConfigurationSection settings = section.getConfigurationSection(key);
            final Material target = settings == null ? null : Material.matchMaterial(settings.getString("to", "AIR"));
            if (material == null || !material.isBlock() || target == null || !target.isBlock()) {
                continue;
            }

            final int radius = settings.contains("radius") ? Math.max(0, settings.getInt("radius")) : defaultRadius;
            result.put(
                    material,
                    new TransformableBlockSettings(
                            Math.min(radius, defaultRadius),
                            target,
                            clampPercent(settings.getInt("chance", 100))));
        }

        return result;
    }

    private static int clampPercent(final int value) {
        return Math.max(0, Math.min(100, value));
    }

    public record BreakableBlockSettings(int radius, int chance, int dropChance) {
        public int getRadius() {
            return radius;
        }

        public int getChance() {
            return chance;
        }

        public int getDropChance() {
            return dropChance;
        }
    }

    public record TransformableBlockSettings(int radius, Material target, int chance) {
        public int getRadius() {
            return radius;
        }

        public Material getTarget() {
            return target;
        }

        public int getChance() {
            return chance;
        }
    }

    public enum ExplosiveMode {
        VANILLA,
        BALL,
        CUBE;

        public static ExplosiveMode from(final String value) {
            if (value == null) {
                return VANILLA;
            }

            try {
                return valueOf(value.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return VANILLA;
            }
        }
    }
}
