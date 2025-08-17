package ru.loper.suntnt.api.modules;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.items.ItemBuilder;
import ru.loper.suncore.utils.Colorize;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.manager.TNTManager;
import ru.loper.suntnt.utils.Utils;

@Getter
public class CustomTNT {
    private final String name;

    private final int explosionRadius;
    private final int obsidianChance;
    private final int spawnerChance;
    private final int goldSpawnerChance;
    private final int blocksRadius;
    private final int liquidChance;
    private final int fuseTicks;
    private final int iceRadius;

    private final boolean spawnerAlwaysSaveMob;
    private final boolean breakPSRegion;
    private final boolean ice;

    private final long iceDelay;

    private final boolean customNameVisible;
    private final String customName;

    private final ItemBuilder tntBuilder;

    public CustomTNT(CustomConfig tntConfig, TNTManager tntManager) {
        FileConfiguration config = tntConfig.getConfig();

        name = config.getString("name", "default");
        obsidianChance = config.getInt("obsidian-chance", 0);
        liquidChance = config.getInt("liquid-chance", 0);
        spawnerChance = config.getInt("spawner-chance", 0);
        goldSpawnerChance = config.getInt("gold-spawner-chance", 0);
        spawnerAlwaysSaveMob = config.getBoolean("spawner-always-save-mob", false);
        explosionRadius = config.getInt("explosion-radius", 5);
        fuseTicks = config.getInt("fuse-ticks", 100);
        blocksRadius = config.getInt("blocks-radius", 4);
        ice = config.getBoolean("ice", false);
        iceRadius = config.getInt("ice-radius", 0);
        iceDelay = config.getInt("ice-delay", 0);
        breakPSRegion = config.getBoolean("break-ps-region", false);

        ConfigurationSection builderSection = config.getConfigurationSection("item");
        if (builderSection == null) {
            tntBuilder = new ItemBuilder(Material.TNT);
        } else {
            tntBuilder = ItemBuilder.fromConfig(builderSection);
        }

        ItemMeta meta = tntBuilder.meta();
        meta.getPersistentDataContainer().set(tntManager.getTntTypeKey(), PersistentDataType.STRING, name);
        tntBuilder.meta(meta);

        ConfigurationSection customNameSection = config.getConfigurationSection("custom_name");
        if (customNameSection != null) {
            customNameVisible = customNameSection.getBoolean("visible", false);
            customName = Colorize.parse(customNameSection.getString("name"));
        } else {
            customNameVisible = false;
            customName = "";
        }

        ConfigurationSection recipeSection = config.getConfigurationSection("recipe");
        if (recipeSection != null) {
            registerRecipe(recipeSection);
        }
    }

    private void registerRecipe(ConfigurationSection recipeSection) {
        String[] value = recipeSection.getString("shape", "").split(":");
        if (value.length != 3) {
            SunTNT.getInstance().getLogger().severe("Ошибка при загрузки крафта для " + name);
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(SunTNT.getInstance(), name), getItemStack());
        recipe.shape(value[0], value[1], value[2]);

        ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            SunTNT.getInstance().getLogger().severe("Ошибка при загрузки крафта для " + name + ", отсутствуют ингредиенты");
            return;
        }

        for (String key : ingredientsSection.getKeys(false)) {
            String shape = ingredientsSection.getString(key);
            if (shape == null) continue;

            if (shape.toLowerCase().startsWith("customitem:")) {
                ItemStack item = Utils.getCustomItem(shape.replace("customitem:", ""));
                if (item == null) continue;
                recipe.setIngredient(key.charAt(0), item);
                continue;
            }
            try {
                Material material = Material.valueOf(shape.toUpperCase());
                recipe.setIngredient(key.charAt(0), material);
            } catch (IllegalArgumentException e) {
                SunTNT.getInstance().getLogger().severe("Неизвестный материал - " + shape);
            }
        }
        try {
            SunTNT.getInstance().getServer().addRecipe(recipe);
        } catch (IllegalStateException e) {
            SunTNT.getInstance().getLogger().severe("Не удалось зарегистрировать крафт динамита " + name);
        }
    }

    public ItemStack getItemStack() {
        return tntBuilder.amount(1).build().clone();
    }
}
