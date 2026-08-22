package ru.loper.suntnt.manager;

import java.io.File;
import java.util.*;
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
public class TNTManager {
    private final SunTNT plugin;
    private final Map<String, CustomTNT> customTNTs;
    private final Set<NamespacedKey> registeredRecipeKeys;

    private final NamespacedKey tntTypeKey;

    public TNTManager(SunTNT plugin) {
        this.plugin = plugin;

        this.tntTypeKey = new NamespacedKey(plugin, "TNTType");
        this.customTNTs = new HashMap<>();
        this.registeredRecipeKeys = new HashSet<>();

        reload();
    }

    public void reload() {
        unregisterRecipes();
        customTNTs.clear();
        File directory = new File(plugin.getDataFolder(), "/tnts");
        if (!directory.exists()) {
            plugin.saveResource("tnts/EXAMPLE_TNT.yml", true);
        }

        for (File file : Utils.getFiles(directory)) {
            if (!file.getName().endsWith(".yml")) continue;

            CustomConfig config = new CustomConfig(file);
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

    private int getRecipePriority(CustomTNT customTNT) {
        ConfigurationSection recipeSection = customTNT.getRecipeSection();
        if (recipeSection == null) {
            return 1;
        }

        return Math.max(0, recipeSection.getInt("priority", 1));
    }

    private void registerRecipe(CustomTNT customTNT) {
        ConfigurationSection recipeSection = customTNT.getRecipeSection();
        if (recipeSection == null) return;

        String[] shape = recipeSection.getString("shape", "").split(":");
        if (shape.length != 3) {
            plugin.getLogger().severe("Ошибка при загрузке крафта для " + customTNT.getName());
            return;
        }

        ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            plugin.getLogger()
                    .severe("Ошибка при загрузке крафта для " + customTNT.getName() + ", отсутствуют ингредиенты");
            return;
        }

        NamespacedKey recipeKey = getRecipeKey(customTNT.getName());
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, customTNT.getItemStack());
        recipe.shape(shape[0], shape[1], shape[2]);

        for (String key : ingredientsSection.getKeys(false)) {
            String ingredient = ingredientsSection.getString(key);
            if (ingredient == null || key.isEmpty()) continue;

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

    private boolean applyIngredient(ShapedRecipe recipe, char key, String ingredient) {
        String lowerIngredient = ingredient.toLowerCase(Locale.ROOT);

        if (lowerIngredient.startsWith("customitem:")) {
            ItemStack item = Utils.getCustomItem(ingredient.substring("customitem:".length()));
            if (item == null) {
                plugin.getLogger().severe("Неизвестный кастомный предмет в крафте - " + ingredient);
                return false;
            }
            recipe.setIngredient(key, new RecipeChoice.ExactChoice(item));
            return true;
        }

        if (lowerIngredient.startsWith("tnt:")) {
            CustomTNT customTNT = getCustomTNT(ingredient.substring("tnt:".length()));
            if (customTNT == null) {
                plugin.getLogger().severe("Неизвестный динамит в крафте - " + ingredient);
                return false;
            }
            recipe.setIngredient(key, new RecipeChoice.ExactChoice(customTNT.getItemStack()));
            return true;
        }

        try {
            Material material = Material.valueOf(ingredient.toUpperCase(Locale.ROOT));
            recipe.setIngredient(key, material);
            return true;
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().severe("Неизвестный материал - " + ingredient);
            return false;
        }
    }

    private NamespacedKey getRecipeKey(String name) {
        return new NamespacedKey(plugin, name.toLowerCase(Locale.ROOT));
    }

    public CustomTNT getCustomTNT(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }

        String tntType =
                itemStack.getItemMeta().getPersistentDataContainer().get(tntTypeKey, PersistentDataType.STRING);
        return customTNTs.get(tntType);
    }

    public CustomTNT getCustomTNT(String name) {
        return customTNTs.get(name);
    }

    public CustomTNT getCustomTNT(Entity entity) {
        if (!entity.hasMetadata("TNTType")) {
            return null;
        }
        String tntType = entity.getMetadata("TNTType").get(0).asString();
        return customTNTs.get(tntType);
    }

    public List<String> getCustomTNTsName() {
        return customTNTs.keySet().stream().toList();
    }
}
