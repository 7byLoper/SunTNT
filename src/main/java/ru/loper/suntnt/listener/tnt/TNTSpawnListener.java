package ru.loper.suntnt.listener.tnt;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.suncore.api.colorize.ComponentColorize;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.model.CustomTNT;
import ru.loper.suntnt.api.model.Rune;
import ru.loper.suntnt.api.model.TNTGunProjectile;
import ru.loper.suntnt.config.TNTConfigManager;
import ru.loper.suntnt.manager.TNTSpawnManager;
import ru.loper.suntnt.utils.Utils;

@RequiredArgsConstructor
public class TNTSpawnListener implements Listener {

    private static final String META_TNT_TYPE = "TNTType";
    private static final String META_TNT_RUNES = "TNTRunes";
    private static final String META_TNT_GUN = "TNTGun";

    private final SunTNT plugin;
    private final TNTConfigManager configManager;
    private final TNTSpawnManager tntSpawnManager;

    private static void setTntCustomName(final TNTPrimed tntPrimed, final String customTnt) {
        tntPrimed.customName(ComponentColorize.fromLegacyString(customTnt));
        tntPrimed.setCustomNameVisible(true);
    }

    private String getRunesDataFromItem(final ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }

        final PersistentDataContainer data = itemStack.getItemMeta().getPersistentDataContainer();
        return data.get(Rune.getTntRuneKey(), PersistentDataType.STRING);
    }

    private Set<Rune> getRunesFromData(final String runesData) {
        return configManager.getRunesFromData(runesData);
    }

    private void applyRunesToItem(final ItemStack itemStack, final Set<Rune> runes) {
        if (itemStack == null || runes == null || runes.isEmpty()) {
            return;
        }

        for (final Rune rune : runes) {
            rune.addRuneToTNT(itemStack);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTNTPlace(final BlockPlaceEvent event) {
        final ItemStack itemStack = event.getItemInHand();
        if (itemStack.getType() != Material.TNT || !itemStack.hasItemMeta()) {
            return;
        }

        final Block block = event.getBlockPlaced();
        final PersistentDataContainer data = itemStack.getItemMeta().getPersistentDataContainer();
        final String tntType = data.get(plugin.getTntManager().getTntTypeKey(), PersistentDataType.STRING);

        if (tntType != null) {
            block.setMetadata(META_TNT_TYPE, new FixedMetadataValue(plugin, tntType));
        }

        final String runesData = getRunesDataFromItem(itemStack);
        if (runesData != null && !runesData.isEmpty()) {
            block.setMetadata(META_TNT_RUNES, new FixedMetadataValue(plugin, runesData));
            for (final Rune rune : getRunesFromData(runesData)) {
                rune.handlePlace(event);
            }
        }

        if (tntType == null) {
            return;
        }

        final CustomTNT customTnt = plugin.getTntManager().getCustomTNT(tntType);
        if (customTnt == null || !customTnt.isAutoIgnite()) {
            return;
        }

        block.setType(Material.AIR, false);
        block.getWorld().spawn(block.getLocation().add(0.5, 0, 0.5), TNTPrimed.class, primed -> {
            primed.setMetadata(META_TNT_TYPE, new FixedMetadataValue(plugin, customTnt.getName()));
            if (runesData != null && !runesData.isEmpty()) {
                primed.setMetadata(META_TNT_RUNES, new FixedMetadataValue(plugin, runesData));
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onTNTDispense(final BlockDispenseEvent event) {
        final Block block = event.getBlock();
        if (block.getType() != Material.DISPENSER) {
            return;
        }

        final ItemStack itemStack = event.getItem();
        if (itemStack.getType() != Material.TNT) {
            return;
        }

        final Location dropLocation = Utils.getDispenserLocation(block);
        final Block dropBlock = dropLocation.getBlock();

        if (block.hasMetadata(META_TNT_GUN)) {
            tntSpawnManager.getCachedGunProjectiles().put(dropBlock, new TNTGunProjectile(block));
        }

        final PersistentDataContainer data = itemStack.getItemMeta().getPersistentDataContainer();
        final String tntType = data.get(plugin.getTntManager().getTntTypeKey(), PersistentDataType.STRING);
        if (tntType != null) {
            final CustomTNT customTNT = plugin.getTntManager().getCustomTNT(tntType);
            if (customTNT != null) {
                tntSpawnManager.getCachedTNTs().put(dropBlock, customTNT);
            }
        }

        final String runesData = getRunesDataFromItem(itemStack);
        if (runesData != null && !runesData.isEmpty()) {
            tntSpawnManager.getCachedRunes().put(dropBlock, runesData);
        }
    }

    @EventHandler
    public void onTNTSpawn(final EntitySpawnEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof TNTPrimed tntPrimed)) {
            return;
        }

        final Block searchBlock = entity.getLocation().getBlock();
        handleTNTPossibility(tntPrimed, searchBlock, entity);
        handleRunesPossibility(event, searchBlock, entity);

        final TNTGunProjectile tntGunProjectile =
                tntSpawnManager.getCachedGunProjectiles().getIfPresent(searchBlock);
        if (tntGunProjectile != null) {
            tntGunProjectile.setTntPrimed(tntPrimed);
            tntSpawnManager.getCachedGunProjectiles().invalidate(searchBlock);
            SunTNT.handleTNTGun(tntGunProjectile);
        }
    }

    private void handleTNTPossibility(final TNTPrimed tntPrimed, final Block block, final Entity entity) {
        CustomTNT customTnt = tntSpawnManager.getCachedTNTs().getIfPresent(block);

        if (customTnt == null && block.hasMetadata(META_TNT_TYPE)) {
            final String tntType = block.getMetadata(META_TNT_TYPE).get(0).asString();
            customTnt = plugin.getTntManager().getCustomTNT(tntType);
            block.removeMetadata(META_TNT_TYPE, plugin);
            tntSpawnManager.getCachedTNTs().invalidate(block);
        }

        if (customTnt == null && entity.hasMetadata(META_TNT_TYPE)) {
            final String tntType = entity.getMetadata(META_TNT_TYPE).get(0).asString();
            customTnt = plugin.getTntManager().getCustomTNT(tntType);
        }

        if (customTnt == null) {
            if (configManager.isDefaultCustomNameVisible()) {
                setTntCustomName(tntPrimed, configManager.getDefaultCustomName());
            }
            return;
        }

        tntPrimed.setFuseTicks(customTnt.getFuseTicks());
        if (customTnt.isCustomNameVisible()) {
            setTntCustomName(tntPrimed, customTnt.getCustomName());
        }

        entity.setMetadata(META_TNT_TYPE, new FixedMetadataValue(plugin, customTnt.getName()));
    }

    private void handleRunesPossibility(final EntitySpawnEvent event, final Block block, final Entity entity) {
        String runesData = tntSpawnManager.getCachedRunes().getIfPresent(block);

        if ((runesData == null || runesData.isEmpty()) && block.hasMetadata(META_TNT_RUNES)) {
            runesData = block.getMetadata(META_TNT_RUNES).get(0).asString();
            block.removeMetadata(META_TNT_RUNES, plugin);
            tntSpawnManager.getCachedRunes().invalidate(block);
        }

        if (runesData == null || runesData.isEmpty()) {
            return;
        }

        entity.setMetadata(META_TNT_RUNES, new FixedMetadataValue(plugin, runesData));

        for (final Rune rune : getRunesFromData(runesData)) {
            rune.handleSpawn(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTNTExplosion(final ExplosionPrimeEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof TNTPrimed tntPrimed)) {
            return;
        }

        if (entity.hasMetadata(META_TNT_RUNES)) {
            final String runesData = entity.getMetadata(META_TNT_RUNES).get(0).asString();
            if (!runesData.isEmpty()) {
                for (final Rune rune : getRunesFromData(runesData)) {
                    rune.handleExplosion(tntPrimed);
                }
            }
        }

        if (!entity.hasMetadata(META_TNT_TYPE)) {
            return;
        }

        final String tntType = entity.getMetadata(META_TNT_TYPE).get(0).asString();
        final CustomTNT customTnt = plugin.getTntManager().getCustomTNT(tntType);
        if (customTnt == null) {
            return;
        }

        if (customTnt.hasExplosivePower()) {
            event.setRadius(customTnt.getExplosionPower());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTNTBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (block.getType() != Material.TNT) {
            return;
        }

        if (!block.hasMetadata(META_TNT_TYPE)) {
            return;
        }

        final String tntType = block.getMetadata(META_TNT_TYPE).get(0).asString();
        final CustomTNT customTnt = plugin.getTntManager().getCustomTNT(tntType);

        final ItemStack drop = customTnt.getItemStack().clone();

        if (block.hasMetadata(META_TNT_RUNES)) {
            final String runesData = block.getMetadata(META_TNT_RUNES).get(0).asString();
            applyRunesToItem(drop, getRunesFromData(runesData));
            block.removeMetadata(META_TNT_RUNES, plugin);
        }

        block.getLocation().getWorld().dropItemNaturally(block.getLocation(), drop);
        block.removeMetadata(META_TNT_TYPE, plugin);

        event.setDropItems(false);
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        if (!configManager.isDisableItemExplosion()) {
            return;
        }

        final Entity entity = event.getEntity();
        if (!(entity instanceof org.bukkit.entity.Item)) {
            return;
        }

        final EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
        updateBlockMeta(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        if (!event.isSticky()) {
            return;
        }

        updateBlockMeta(event.getBlocks(), event.getDirection());
    }

    private void updateBlockMeta(final List<Block> blocks, final BlockFace direction) {
        final ListIterator<Block> blockIterator = blocks.listIterator(blocks.size());
        while (blockIterator.hasPrevious()) {
            final Block block = blockIterator.previous();
            if (block.getType() != Material.TNT) {
                continue;
            }

            String tntType = null;
            if (block.hasMetadata(META_TNT_TYPE)) {
                tntType = block.getMetadata(META_TNT_TYPE).get(0).asString();
                block.removeMetadata(META_TNT_TYPE, plugin);
            }

            String runesData = null;
            if (block.hasMetadata(META_TNT_RUNES)) {
                runesData = block.getMetadata(META_TNT_RUNES).get(0).asString();
                block.removeMetadata(META_TNT_RUNES, plugin);
            }

            if (tntType == null && (runesData == null || runesData.isEmpty())) {
                continue;
            }

            final Block nextBlock = block.getRelative(direction, 1);
            if (tntType != null) {
                nextBlock.setMetadata(META_TNT_TYPE, new FixedMetadataValue(plugin, tntType));
            }
            if (runesData != null && !runesData.isEmpty()) {
                nextBlock.setMetadata(META_TNT_RUNES, new FixedMetadataValue(plugin, runesData));
            }
        }
    }
}
