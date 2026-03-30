package ru.loper.suntnt.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.modules.CustomTNT;
import ru.loper.suntnt.api.modules.Rune;
import ru.loper.suntnt.api.modules.TNTGunProjectile;
import ru.loper.suntnt.config.TNTConfigManager;
import ru.loper.suntnt.utils.Utils;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class TNTSpawnListener implements Listener {
    private static final String META_TNT_TYPE = "TNTType";
    private static final String META_TNT_RUNES = "TNTRunes";

    private final SunTNT plugin;
    private final TNTConfigManager configManager;

    private final Cache<Block, CustomTNT> cachedTNTs = CacheBuilder.newBuilder()
            .expireAfterWrite(10L, TimeUnit.SECONDS)
            .build();

    private final Cache<Block, TNTGunProjectile> cachedGunProjectiles = CacheBuilder.newBuilder()
            .expireAfterWrite(10L, TimeUnit.SECONDS)
            .build();

    private final Cache<Block, String> cachedRunes = CacheBuilder.newBuilder()
            .expireAfterWrite(10L, TimeUnit.SECONDS)
            .build();

    private static void setTntCustomName(TNTPrimed tntPrimed, String customTnt) {
        tntPrimed.setCustomName(customTnt);
        tntPrimed.setCustomNameVisible(true);
    }

    private String getRunesDataFromItem(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer data = itemStack.getItemMeta().getPersistentDataContainer();
        return data.get(Rune.getTntRuneKey(), PersistentDataType.STRING);
    }

    private Set<Rune> getRunesFromData(String runesData) {
        return configManager.getRunesFromData(runesData);
    }

    private void applyRunesToItem(ItemStack itemStack, Set<Rune> runes) {
        if (itemStack == null || runes == null || runes.isEmpty()) {
            return;
        }
        for (Rune rune : runes) {
            rune.addRuneToTNT(itemStack);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTNTPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();
        if (itemStack.getType() != Material.TNT || !itemStack.hasItemMeta()) {
            return;
        }

        Block block = event.getBlockPlaced();

        PersistentDataContainer data = itemStack.getItemMeta().getPersistentDataContainer();
        String tntType = data.get(plugin.getTntManager().getTntTypeKey(), PersistentDataType.STRING);
        if (tntType != null) {
            block.setMetadata(META_TNT_TYPE, new FixedMetadataValue(plugin, tntType));
        }

        String runesData = getRunesDataFromItem(itemStack);
        if (runesData != null && !runesData.isEmpty()) {
            block.setMetadata(META_TNT_RUNES, new FixedMetadataValue(plugin, runesData));
            for (Rune rune : getRunesFromData(runesData)) {
                rune.handlePlace(event);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void omTNTDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DISPENSER) return;

        ItemStack itemStack = event.getItem();
        if (itemStack.getType() != Material.TNT || !itemStack.hasItemMeta()) {
            return;
        }

        Location dropLocation = Utils.getDispenserLocation(block);
        Block dropBlock = dropLocation.getBlock();

        if (block.hasMetadata("TNTGun")) {
            cachedGunProjectiles.put(dropBlock, new TNTGunProjectile(block));
        }

        PersistentDataContainer data = itemStack.getItemMeta().getPersistentDataContainer();

        String tntType = data.get(plugin.getTntManager().getTntTypeKey(), PersistentDataType.STRING);
        if (tntType != null) {
            CustomTNT customTNT = plugin.getTntManager().getCustomTNT(tntType);
            if (customTNT != null) {
                cachedTNTs.put(dropBlock, customTNT);
            }
        }

        String runesData = getRunesDataFromItem(itemStack);
        if (runesData != null && !runesData.isEmpty()) {
            cachedRunes.put(dropBlock, runesData);
        }
    }

    @EventHandler
    public void onTNTSpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof TNTPrimed tntPrimed)) {
            return;
        }

        Block searchBlock = entity.getLocation().getBlock();
        handleTNTPossibility(tntPrimed, searchBlock, entity);
        handleRunesPossibility(event, searchBlock, entity);

        TNTGunProjectile tntGunProjectile = cachedGunProjectiles.getIfPresent(searchBlock);
        if (tntGunProjectile != null) {
            tntGunProjectile.setTntPrimed(tntPrimed);
            cachedGunProjectiles.invalidate(searchBlock);
            SunTNT.handleTNTGun(tntGunProjectile);
        }
    }

    private void handleTNTPossibility(TNTPrimed tntPrimed, Block block, Entity entity) {
        CustomTNT customTnt = cachedTNTs.getIfPresent(block);

        if (customTnt == null && block.hasMetadata(META_TNT_TYPE)) {
            String tntType = block.getMetadata(META_TNT_TYPE).get(0).asString();
            customTnt = plugin.getTntManager().getCustomTNT(tntType);
            block.removeMetadata(META_TNT_TYPE, plugin);
            cachedTNTs.invalidate(block);
        }

        if (customTnt == null && entity.hasMetadata(META_TNT_TYPE)) {
            String tntType = entity.getMetadata(META_TNT_TYPE).get(0).asString();
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

    private void handleRunesPossibility(EntitySpawnEvent event, Block block, Entity entity) {
        String runesData = cachedRunes.getIfPresent(block);

        if ((runesData == null || runesData.isEmpty()) && block.hasMetadata(META_TNT_RUNES)) {
            runesData = block.getMetadata(META_TNT_RUNES).get(0).asString();
            block.removeMetadata(META_TNT_RUNES, plugin);
            cachedRunes.invalidate(block);
        }

        if (runesData == null || runesData.isEmpty()) {
            return;
        }

        entity.setMetadata(META_TNT_RUNES, new FixedMetadataValue(plugin, runesData));

        for (Rune rune : getRunesFromData(runesData)) {
            rune.handleSpawn(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTNTExplosion(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof TNTPrimed tntPrimed)) {
            return;
        }

        if (entity.hasMetadata(META_TNT_RUNES)) {
            String runesData = entity.getMetadata(META_TNT_RUNES).get(0).asString();
            if (!runesData.isEmpty()) {
                for (Rune rune : getRunesFromData(runesData)) {
                    rune.handleExplosion(tntPrimed);
                }
            }
        }

        if (!entity.hasMetadata(META_TNT_TYPE)) {
            return;
        }

        String tntType = entity.getMetadata(META_TNT_TYPE).get(0).asString();
        CustomTNT customTnt = plugin.getTntManager().getCustomTNT(tntType);
        if (customTnt == null) {
            return;
        }

        event.setRadius(customTnt.getExplosionRadius());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTNTBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.TNT) {
            return;
        }

        if (!block.hasMetadata(META_TNT_TYPE)) {
            return;
        }

        String tntType = block.getMetadata(META_TNT_TYPE).get(0).asString();
        CustomTNT customTnt = plugin.getTntManager().getCustomTNT(tntType);

        ItemStack drop = customTnt.getItemStack().clone();

        if (block.hasMetadata(META_TNT_RUNES)) {
            String runesData = block.getMetadata(META_TNT_RUNES).get(0).asString();
            applyRunesToItem(drop, getRunesFromData(runesData));
            block.removeMetadata(META_TNT_RUNES, plugin);
        }

        block.getLocation().getWorld().dropItemNaturally(block.getLocation(), drop);
        block.removeMetadata(META_TNT_TYPE, plugin);

        event.setDropItems(false);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!configManager.isDisableItemExplosion()) {
            return;
        }

        Entity entity = event.getEntity();
        if (!entity.getType().name().contains("ITEM")) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        updateBlockMeta(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) {
            return;
        }

        updateBlockMeta(event.getBlocks(), event.getDirection());
    }

    private void updateBlockMeta(List<Block> blocks2, BlockFace direction) {
        ListIterator<Block> blockIterator = blocks2.listIterator(blocks2.size());
        while (blockIterator.hasPrevious()) {
            Block block = blockIterator.previous();
            if (block.getType() != Material.TNT) continue;

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

            Block nextBlock = block.getRelative(direction, 1);
            if (tntType != null) {
                nextBlock.setMetadata(META_TNT_TYPE, new FixedMetadataValue(plugin, tntType));
            }
            if (runesData != null && !runesData.isEmpty()) {
                nextBlock.setMetadata(META_TNT_RUNES, new FixedMetadataValue(plugin, runesData));
            }
        }
    }
}