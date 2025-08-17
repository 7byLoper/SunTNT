package ru.loper.suntnt.listeners;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.modules.CustomTNT;
import ru.loper.suntnt.config.TNTConfigManager;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class TNTSpawnListener implements Listener {
    private final SunTNT plugin;
    private final TNTConfigManager configManager;

    private final Cache<Block, CustomTNT> cachedTNTs = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();

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

        PersistentDataContainer data = itemStack.getItemMeta().getPersistentDataContainer();
        String tntType = data.get(plugin.getTntManager().getTntTypeKey(), PersistentDataType.STRING);
        if (tntType == null) {
            return;
        }

        CustomTNT customTNT = plugin.getTntManager().getCustomTNT(tntType);
        if (customTNT == null) {
            return;
        }

        Location dropLocation = getDispenserLocation(block);
        cachedTNTs.put(dropLocation.getBlock(), customTNT);
    }

    @EventHandler
    public void onTNTSpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof TNTPrimed tntPrimed)) {
            return;
        }

        Block block = event.getLocation().getBlock();
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

    private static void setTntCustomName(TNTPrimed tntPrimed, String customTnt) {
        tntPrimed.setCustomName(customTnt);
        tntPrimed.setCustomNameVisible(true);
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
    public void onBreak(BlockBreakEvent event) {
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

    private Location getDispenserLocation(Block dispenser) {
        Directional directional = (Directional) dispenser.getBlockData();
        BlockFace face = directional.getFacing();
        return dispenser.getLocation().add(0.5 + face.getModX() * 0.7,
                0.5 + face.getModY() * 0.7,
                0.5 + face.getModZ() * 0.7);
    }

    public Inventory getInventoryFromDispencer(Location location) {
        Block block = location.getBlock();
        if (block.getState() instanceof Dispenser dispenser) {
            return dispenser.getInventory();
        }

        return null;
    }
}
