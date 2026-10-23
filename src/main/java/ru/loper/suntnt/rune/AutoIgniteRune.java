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

    private static final String META_TNT_TYPE = "TNTType";
    private static final String META_TNT_RUNES = "TNTRunes";

    @Override
    public void onLoad(final ConfigurationSection section) {}

    @Override
    public void handlePlace(final BlockPlaceEvent event) {
        final Block block = event.getBlockPlaced();
        if (block.getType() != Material.TNT) {
            return;
        }

        final String tntType = block.hasMetadata(META_TNT_TYPE)
                ? block.getMetadata(META_TNT_TYPE).get(0).asString()
                : null;
        final String runesData = block.hasMetadata(META_TNT_RUNES)
                ? block.getMetadata(META_TNT_RUNES).get(0).asString()
                : null;

        block.setType(Material.AIR);
        block.getWorld().spawn(block.getLocation().add(0.5, 0, 0.5), TNTPrimed.class, entity -> {
            if (tntType != null) {
                entity.setMetadata(META_TNT_TYPE, new FixedMetadataValue(SunTNT.getInstance(), tntType));
            }

            if (runesData != null && !runesData.isEmpty()) {
                entity.setMetadata(META_TNT_RUNES, new FixedMetadataValue(SunTNT.getInstance(), runesData));
            }
        });
    }
}
