package ru.loper.suntnt.rune;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.model.Rune;

public class AutoIgniteRune extends Rune {
    @Override
    public void onLoad(ConfigurationSection section) {}

    @Override
    public void handlePlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.TNT) {
            return;
        }

        String tntType = block.hasMetadata("TNTType")
                ? block.getMetadata("TNTType").get(0).asString()
                : null;
        String runesData = block.hasMetadata("TNTRunes")
                ? block.getMetadata("TNTRunes").get(0).asString()
                : null;

        block.setType(Material.AIR);

        block.getWorld().spawn(block.getLocation().add(0.5, 0, 0.5), TNTPrimed.class, entity -> {
            if (tntType != null) {
                entity.setMetadata("TNTType", new FixedMetadataValue(SunTNT.getInstance(), tntType));
            }

            if (runesData != null && !runesData.isEmpty()) {
                entity.setMetadata("TNTRunes", new FixedMetadataValue(SunTNT.getInstance(), runesData));
            }
        });
    }
}
