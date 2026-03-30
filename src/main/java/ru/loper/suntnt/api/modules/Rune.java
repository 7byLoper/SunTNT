package ru.loper.suntnt.api.modules;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import lombok.Data;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.colorize.StringColorize;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.enums.RuneType;

import java.util.*;
import java.util.stream.Collectors;

@Data
public abstract class Rune {
    @Getter
    private static final NamespacedKey runeKey = new NamespacedKey(SunTNT.getInstance(), "rune");
    @Getter
    private static final NamespacedKey tntRuneKey = new NamespacedKey(SunTNT.getInstance(), "tnt-runes");

    protected String name;
    protected String displayName;
    protected int cost;

    protected RuneType type;
    protected ItemBuilder itemBuilder;

    public void loadValues(@NotNull ConfigurationSection section) {
        this.name = section.getName();
        this.displayName = StringColorize.parse(section.getString("display_name"));
        this.cost = section.getInt("cost");

        this.type = RuneType.getByName(section.getString("rune_type", ""));

        this.itemBuilder = ItemBuilder.fromConfig(section.getConfigurationSection("item"));
        this.itemBuilder.namespacedKey(runeKey, PersistentDataType.STRING, this.name);

        onLoad(section);
    }

    public ItemStack addRuneToTNT(@NotNull ItemStack itemStack) {
        ItemMeta resultMeta = itemStack.hasItemMeta() ?
                itemStack.getItemMeta() :
                SunTNT.getInstance().getServer().getItemFactory().getItemMeta(itemStack.getType());

        PersistentDataContainer container = resultMeta.getPersistentDataContainer();

        Set<String> runes = new HashSet<>();
        if (container.has(tntRuneKey, PersistentDataType.STRING)) {
            String existing = container.get(tntRuneKey, PersistentDataType.STRING);
            if (existing != null) {
                runes.addAll(Arrays.asList(existing.split(",")));
            }
        }

        runes.add(this.name);
        container.set(tntRuneKey, PersistentDataType.STRING, String.join(",", runes));

        List<String> lore = resultMeta.hasLore() ?
                new ArrayList<>(resultMeta.getLore()) :
                new ArrayList<>();

        String settingsLore = SunTNT.getInstance().getConfigManager().getRunesInfoLore();
        if (settingsLore != null && !lore.contains(settingsLore)) {
            lore.add("");
            lore.add(settingsLore);
        }

        lore.add(displayName);

        resultMeta.setLore(lore.stream().distinct().collect(Collectors.toList()));
        itemStack.setItemMeta(resultMeta);

        return itemStack;
    }

    public abstract void onLoad(ConfigurationSection section);

    public void handlePlace(BlockPlaceEvent event) {
    }

    public void handleExplosion(TNTPrimed tntPrimed) {
    }

    public void handlePrime(TNTPrimeEvent event) {
    }

    public void handleSpawn(EntitySpawnEvent event) {
    }
}