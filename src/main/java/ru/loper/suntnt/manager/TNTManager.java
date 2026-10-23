package ru.loper.suntnt.manager;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.model.CustomTNT;
import ru.loper.suntnt.utils.Utils;

@Getter
public final class TNTManager {

    private static final Pattern COLON_PATTERN = Pattern.compile(":");
    private static final String CUSTOM_ITEM_PREFIX = "customitem:";
    private static final String TNT_PREFIX = "tnt:";
    private static final String META_TNT_TYPE = "TNTType";
    private static final String YAML_EXTENSION = ".yml";

    private final SunTNT plugin;
    private final Map<String, CustomTNT> customTNTs;
    private final Set<NamespacedKey> registeredRecipeKeys;
    private final NamespacedKey tntTypeKey;

    public TNTManager(final SunTNT plugin) {
        this.plugin = plugin;
        this.tntTypeKey = new NamespacedKey(plugin, META_TNT_TYPE);
        this.customTNTs = new HashMap<>();
        this.registeredRecipeKeys = new HashSet<>();

        reload();
    }

    public void reload() {
        unregisterRecipes();
        customTNTs.clear();

        final File directory = new File(plugin.getDataFolder(), "/tnts");
        if (!directory.exists()) {
            plugin.saveResource("tnts/EXAMPLE_TNT.yml", true);
        }

        for (final File file : Utils.getFiles(directory)) {
            if (!file.getName().endsWith(YAML_EXTENSION)) {
                continue;
            }

            final CustomConfig config = new CustomConfig(file);
            customTNTs.put(config.getConfig().getString("name", "default"), new CustomTNT(config, this));
        }

        registerRecipes();
    }

    public void unregisterRecipes() {
        registeredRecipeKeys.forEach(plugin.getServer()::removeRecipe);
        registeredRecipeKeys.clear();
    }

    private void registerRecipes() {
        customTNTs.values().stream()
                .sorted(Comparator.comparingInt(this::getRecipePriority))
                .forEach(this::registerRecipe);
    }

    private int getRecipePriority(final CustomTNT customTNT) {
        final ConfigurationSection recipeSection = customTNT.getRecipeSection();
        if (recipeSection == null) {
            return 1;
        }

        return Math.max(0, recipeSection.getInt("priority", 1));
    }

    private void registerRecipe(final CustomTNT customTNT) {
        final ConfigurationSection recipeSection = customTNT.getRecipeSection();
        if (recipeSection == null) {
            return;
        }

        final String[] shape = COLON_PATTERN.split(recipeSection.getString("shape", ""));
        if (shape.length != 3) {
            plugin.getLogger().severe("Ошибка при загрузке крафта для " + customTNT.getName());
            return;
        }

        final ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            plugin.getLogger().severe("Ошибка при загрузке крафта для " + customTNT.getName() + ", отсутствуют ингредиенты");
            return;
        }

        final NamespacedKey recipeKey = getRecipeKey(customTNT.getName());
        final ShapedRecipe recipe = new ShapedRecipe(recipeKey, customTNT.getItemStack());
        recipe.shape(shape[0], shape[1], shape[2]);

        for (final String key : ingredientsSection.getKeys(false)) {
            final String ingredient = ingredientsSection.getString(key);
            if (ingredient == null || key.isEmpty()) {
                continue;
            }

            if (!applyIngredient(recipe, key.charAt(0), ingredient)) {
                return;
            }
        }

        try {
            plugin.getServer().addRecipe(recipe);
            registeredRecipeKeys.add(recipeKey);
            plugin.getLogger().info("Крафт динамита '%s' успешно зарегистрирован".formatted(customTNT.getName()));
        } catch (IllegalStateException exception) {
            plugin.getLogger().severe("Не удалось зарегистрировать крафт динамита " + customTNT.getName());
        }
    }

    private boolean applyIngredient(final ShapedRecipe recipe, final char key, final String ingredient) {
        final String lowerIngredient = ingredient.toLowerCase(Locale.ROOT);

        if (lowerIngredient.startsWith(CUSTOM_ITEM_PREFIX)) {
            final ItemStack item = Utils.getCustomItem(ingredient.substring(CUSTOM_ITEM_PREFIX.length()));
            if (item == null) {
                plugin.getLogger().severe("Неизвестный кастомный предмет в крафте - " + ingredient);
                return false;
            }
            recipe.setIngredient(key, new RecipeChoice.ExactChoice(item));
            return true;
        }

        if (lowerIngredient.startsWith(TNT_PREFIX)) {
            final CustomTNT customTNT = getCustomTNT(ingredient.substring(TNT_PREFIX.length()));
            if (customTNT == null) {
                plugin.getLogger().severe("Неизвестный динамит в крафте - " + ingredient);
                return false;
            }
            recipe.setIngredient(key, new RecipeChoice.ExactChoice(customTNT.getItemStack()));
            return true;
        }

        final Material material = Material.matchMaterial(ingredient);
        if (material != null) {
            recipe.setIngredient(key, material);
            return true;
        }

        plugin.getLogger().severe("Неизвестный материал - " + ingredient);
        return false;
    }

    private NamespacedKey getRecipeKey(final String name) {
        return new NamespacedKey(plugin, name.toLowerCase(Locale.ROOT));
    }

    public CustomTNT getCustomTNT(final ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }

        final String tntType =
                itemStack.getItemMeta().getPersistentDataContainer().get(tntTypeKey, PersistentDataType.STRING);
        return customTNTs.get(tntType);
    }

    public CustomTNT getCustomTNT(final String name) {
        return customTNTs.get(name);
    }

    public CustomTNT getCustomTNT(final Entity entity) {
        if (!entity.hasMetadata(META_TNT_TYPE)) {
            return null;
        }

        final String tntType = entity.getMetadata(META_TNT_TYPE).get(0).asString();
        return customTNTs.get(tntType);
    }

    public List<String> getCustomTNTsName() {
        return customTNTs.keySet().stream().toList();
    }
}
