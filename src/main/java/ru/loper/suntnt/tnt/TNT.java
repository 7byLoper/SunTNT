package ru.loper.suntnt.tnt;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.utils.Colorize;
import ru.loper.suntnt.utils.Utils;

public class TNT {
    private final String name;

    private final int obsidianChance;

    private final int liquidChance;

    private final int spawnerChance;

    private final boolean spawnerAlwaysSaveMob;

    private final int explosionRadius;

    private final int fuseTicks;

    private final int blocksRadius;

    private final int iceRadius;
    private final long iceDelay;

    private final boolean ice;

    private final String displayName;

    private final List<String> lore;

    public String getName() {
        return this.name;
    }

    public int getObsidianChance() {
        return this.obsidianChance;
    }

    public int getLiquidChance() {
        return this.liquidChance;
    }

    public int getSpawnerChance() {
        return this.spawnerChance;
    }

    public boolean isSpawnerAlwaysSaveMob() {
        return this.spawnerAlwaysSaveMob;
    }

    public int getExplosionRadius() {
        return this.explosionRadius;
    }

    public int getFuseTicks() {
        return this.fuseTicks;
    }

    public int getBlocksRadius() {
        return this.blocksRadius;
    }

    public boolean isIce() {
        return this.ice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TNT(FileConfiguration config){
        this.name = config.getString("name");
        this.obsidianChance = config.getInt("obsidian-chance");
        this.liquidChance = config.getInt("liquid-chance");
        this.spawnerChance = config.getInt("spawner-chance");
        this.spawnerAlwaysSaveMob = config.getBoolean("spawner-always-save-mob");
        this.explosionRadius = config.getInt("explosion-radius");
        this.fuseTicks = config.getInt("fuse-ticks");
        this.blocksRadius = config.getInt("blocks-radius");
        this.ice = config.getBoolean("ice");
        if(config.contains("ice-radius")) {
            iceRadius = config.getInt("ice-radius");
        }else{
            iceRadius = 0;
        }
        if(config.contains("ice-delay")) {
            iceDelay = config.getLong("ice-delay");
        }else{
            iceDelay = 105L;
        }
        this.displayName = Colorize.format(config.getString("display-name"));
        this.lore = Colorize.format(config.getStringList("lore"));
        recipe:
        if(config.contains("recipe")){
            String[] value = config.getString("recipe.shape").split(":");
            if(value.length != 3) break recipe;
            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(SunTNT.getInstance(), "tntA"), this.getItemStack());
            recipe.shape(value[0], value[1], value[2]);
            for(String tag : config.getConfigurationSection("recipe.ingredients").getKeys(false)){
                String shape = config.getString("recipe.ingredients."+tag);
                if(shape == null) continue;
                if(shape.toLowerCase().startsWith("customitem:")){
                    ItemStack item = Utils.getCustomItem(shape.replace("customitem:",""));
                    if(item == null) continue;
                    recipe.setIngredient(tag.charAt(0), item);
                    continue;
                }
                Material material;
                try{
                    material = Material.valueOf(shape.toUpperCase());
                } catch (Exception e){
                    SunTNT.getInstance().getLogger().severe("Material error "+shape);
                    material = Material.AIR;
                }
                recipe.setIngredient(tag.charAt(0), material);
            }
            try {
                SunTNT.getInstance().getServer().addRecipe(recipe);
            } catch (Exception e){
                //ignore
            }
        }
    }

    public ItemStack getItemStack() {
        ItemStack itemStack = new ItemStack(Material.TNT);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName);
        itemMeta.setLore(lore);
        PersistentDataContainer data = itemMeta.getPersistentDataContainer();
        data.set(new NamespacedKey(SunTNT.getInstance(), "TNTType"), PersistentDataType.STRING, this.name);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public int getIceRadius() {
        return iceRadius;
    }

    public long getIceDelay() {
        return iceDelay;
    }
}
