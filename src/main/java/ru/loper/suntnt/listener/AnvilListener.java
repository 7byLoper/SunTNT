package ru.loper.suntnt.listener;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.suntnt.api.model.Rune;
import ru.loper.suntnt.config.TNTConfigManager;

@RequiredArgsConstructor
public class AnvilListener implements Listener {

    private final TNTConfigManager configManager;

    @EventHandler
    public void onPrepareAnvil(final PrepareAnvilEvent event) {
        final AnvilInventory inventory = event.getInventory();
        final ItemStack firstItem = inventory.getFirstItem();
        final ItemStack secondItem = inventory.getSecondItem();

        if (firstItem == null || secondItem == null) {
            return;
        }

        if (firstItem.getType() != Material.TNT) {
            return;
        }

        if (!isRuneMeta(secondItem) || secondItem.getAmount() != 1) {
            return;
        }

        final String name = getRuneType(secondItem);
        final Rune rune = configManager.getRune(name);
        if (rune == null) {
            return;
        }

        final ItemStack result = firstItem.clone();
        final Set<Rune> existingRunes = configManager.getRunesFromTNT(result);
        if (existingRunes.stream().anyMatch(r -> r.getName().equals(rune.getName()))) {
            return;
        }

        event.setResult(rune.addRuneToTNT(result));
        inventory.setRepairCost(rune.getCost());
    }

    private boolean isRuneMeta(final ItemStack item) {
        return item != null
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(Rune.getRuneKey(), PersistentDataType.STRING);
    }

    private String getRuneType(final ItemStack item) {
        return item.getItemMeta()
                .getPersistentDataContainer()
                .getOrDefault(Rune.getRuneKey(), PersistentDataType.STRING, "");
    }
}
