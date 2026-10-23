package ru.loper.suntnt.api.model;

import java.util.*;
import java.util.regex.Pattern;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.colorize.ComponentColorize;
import ru.loper.suncore.api.colorize.StringColorize;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.enums.RuneType;

@Getter
public abstract class Rune {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    @Getter
    private static final NamespacedKey runeKey = new NamespacedKey(SunTNT.getInstance(), "rune");

    @Getter
    private static final NamespacedKey tntRuneKey = new NamespacedKey(SunTNT.getInstance(), "tnt-runes");

    protected String name;
    protected String displayName;
    protected int cost;
    protected RuneType type;
    protected ItemBuilder itemBuilder;

    public void loadValues(final @NotNull ConfigurationSection section) {
        this.name = section.getName();
        this.displayName = StringColorize.parse(section.getString("display_name"));
        this.cost = section.getInt("cost");
        this.type = RuneType.getByName(section.getString("rune_type", ""));

        this.itemBuilder = ItemBuilder.fromConfig(section.getConfigurationSection("item"));
        this.itemBuilder.namespacedKey(runeKey, PersistentDataType.STRING, this.name);

        onLoad(section);
    }

    public ItemStack addRuneToTNT(final @NotNull ItemStack itemStack) {
        final ItemMeta resultMeta = itemStack.hasItemMeta()
                ? itemStack.getItemMeta()
                : SunTNT.getInstance().getServer().getItemFactory().getItemMeta(itemStack.getType());

        final PersistentDataContainer container = resultMeta.getPersistentDataContainer();
        final Set<String> runes = new HashSet<>();

        if (container.has(tntRuneKey, PersistentDataType.STRING)) {
            final String existing = container.get(tntRuneKey, PersistentDataType.STRING);
            if (existing != null && !existing.isEmpty()) {
                Collections.addAll(runes, COMMA_PATTERN.split(existing));
            }
        }

        runes.add(this.name);
        container.set(tntRuneKey, PersistentDataType.STRING, String.join(",", runes));

        final List<Component> lore = resultMeta.hasLore() && resultMeta.lore() != null
                ? new ArrayList<>(Objects.requireNonNull(resultMeta.lore()))
                : new ArrayList<>();

        final String settingsLore = SunTNT.getInstance().getConfigManager().getRunesInfoLore();
        if (settingsLore != null && !settingsLore.isEmpty()) {
            final Component settingsComponent = ComponentColorize.fromLegacyString(settingsLore);
            if (!lore.contains(settingsComponent)) {
                lore.add(Component.empty());
                lore.add(settingsComponent);
            }
        }

        lore.add(ComponentColorize.fromLegacyString(displayName));
        resultMeta.lore(lore.stream().distinct().toList());
        itemStack.setItemMeta(resultMeta);

        return itemStack;
    }

    public abstract void onLoad(ConfigurationSection section);

    public void handlePlace(final BlockPlaceEvent event) {}

    public void handleExplosion(final TNTPrimed tntPrimed) {}

    public void handlePrime(final TNTPrimeEvent event) {}

    public void handleSpawn(final EntitySpawnEvent event) {}
}
