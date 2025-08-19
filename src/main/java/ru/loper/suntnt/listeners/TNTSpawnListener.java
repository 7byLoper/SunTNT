package ru.loper.suntnt.listeners;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import ru.loper.suntnt.api.modules.TNTGunProjectile;
import ru.loper.suntnt.config.TNTConfigManager;
import ru.loper.suntnt.utils.Utils;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class TNTSpawnListener implements Listener {
    private final SunTNT plugin;
    private final TNTConfigManager configManager;

    private final Cache<Block, CustomTNT> cachedTNTs = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();
    private final Cache<Block, TNTGunProjectile> cachedGunProjectiles = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();

    private static void setTntCustomName(TNTPrimed tntPrimed, String customTnt) {
        tntPrimed.setCustomName(customTnt);
        tntPrimed.setCustomNameVisible(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTNTPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();
        if (!itemStack.getType().equals(Material.TNT)) {
            return;
        }

        PersistentDataContainer data = itemStack.getItemMeta().getPersistentDataContainer();
        String tntType = data.get(plugin.getTntManager().getTntTypeKey(), PersistentDataType.STRING);
        if (tntType == null) {
            return;
        }

        Block block = event.getBlockPlaced();
        block.setMetadata("TNTType", new FixedMetadataValue(plugin, tntType));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTNTPrime(TNTPrimeEvent event) {
        Block block = event.getBlock();
        if (!block.hasMetadata("TNTType")) {
            return;
        }

        String tntType = (block.getMetadata("TNTType").get(0)).asString();
        CustomTNT customTnt = plugin.getTntManager().getCustomTNT(tntType);
        if (customTnt == null) {
            return;
        }

        cachedTNTs.put(block, customTnt);
        block.removeMetadata("TNTType", plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void omTNTDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DISPENSER) return;

        ItemStack itemStack = event.getItem();
        if (!itemStack.getType().equals(Material.TNT)) {
            return;
        }

        Location dropLocation = Utils.getDispenserLocation(block);
        if (block.hasMetadata("TNTGun")) {
            cachedGunProjectiles.put(dropLocation.getBlock(), new TNTGunProjectile(block));
        }

        PersistentDataContainer data = itemStack.getItemMeta().getPersistentDataContainer();
        String tntType = data.get(plugin.getTntManager().getTntTypeKey(), PersistentDataType.STRING);
        if (tntType == null) {
            return;
        }

        CustomTNT customTNT = plugin.getTntManager().getCustomTNT(tntType);
        if (customTNT == null) {
            return;
        }

        cachedTNTs.put(dropLocation.getBlock(), customTNT);
    }

    @EventHandler
    public void onTNTSpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof TNTPrimed tntPrimed)) {
            return;
        }

        Block block = event.getLocation().getBlock();
        handleTNTPossibility(tntPrimed, block, entity);

        TNTGunProjectile tntGunProjectile = cachedGunProjectiles.getIfPresent(block);
        if (tntGunProjectile != null) {
            tntGunProjectile.setTntPrimed(tntPrimed);
            cachedGunProjectiles.invalidate(block);
            SunTNT.handleTNTGun(tntGunProjectile);
        }
    }

    private void handleTNTPossibility(TNTPrimed tntPrimed, Block block, Entity entity) {
        CustomTNT customTnt = cachedTNTs.getIfPresent(block);

        if (customTnt == null) {
            if (configManager.isDefaultCustomNameVisible()) {
                setTntCustomName(tntPrimed, configManager.getDefaultCustomName());
            }
            return;
        }

        cachedTNTs.invalidate(block);

        tntPrimed.setFuseTicks(customTnt.getFuseTicks());
        if (customTnt.isCustomNameVisible()) {
            setTntCustomName(tntPrimed, customTnt.getCustomName());
        }

        entity.setMetadata("TNTType", new FixedMetadataValue(plugin, customTnt.getName()));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTNTExplosion(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof TNTPrimed)) {
            return;
        }

        if (!entity.hasMetadata("TNTType")) {
            return;
        }

        String tntType = (entity.getMetadata("TNTType").get(0)).asString();
        CustomTNT customTnt = plugin.getTntManager().getCustomTNT(tntType);
        if (customTnt == null) {
            return;
        }

        event.setRadius(customTnt.getExplosionRadius());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTNTBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!block.getType().equals(Material.TNT)) {
            return;
        }

        if (!block.hasMetadata("TNTType")) {
            return;
        }

        String tntType = (block.getMetadata("TNTType").get(0)).asString();
        CustomTNT customTnt = plugin.getTntManager().getCustomTNT(tntType);

        block.getLocation().getWorld().dropItemNaturally(block.getLocation(), customTnt.getItemStack());
        block.removeMetadata("TNTType", plugin);

        event.setDropItems(false);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (!configManager.isDisableItemExplosion()) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (!entity.getType().equals(EntityType.DROPPED_ITEM)) {
            return;
        }

        if (cause.equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) || cause.equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        updateBlockMeta(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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
            if (!block.getType().equals(Material.TNT) || !block.hasMetadata("TNTType")) continue;

            String tntType = (block.getMetadata("TNTType").get(0)).asString();
            block.removeMetadata("TNTType", plugin);

            Block nextBlock = block.getRelative(direction, 1);
            nextBlock.setMetadata("TNTType", new FixedMetadataValue(plugin, tntType));
        }
    }
}
