package ru.loper.suntnt.listener.tnt;

import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suncore.api.scheduler.SchedulerServices;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.config.TNTConfigManager;

@RequiredArgsConstructor
public class TNTGunListener implements Listener {

    private static final String META_TNT_GUN_PROJECTILE = "TNTGunProjectile";
    private static final String META_TNT_GUN = "TNTGun";

    private final SunTNT plugin;
    private final TNTConfigManager configManager;

    @EventHandler
    public void onHit(final ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }

        if (!snowball.hasMetadata(META_TNT_GUN_PROJECTILE)) {
            return;
        }

        if (event.getHitBlock() == null) {
            event.setCancelled(true);
            return;
        }

        for (final Entity passenger : snowball.getPassengers()) {
            if (passenger instanceof TNTPrimed tntPrimed) {
                tntPrimed.setFuseTicks(0);
            }
        }

        snowball.remove();
    }

    @EventHandler
    public void onDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }

        if (!snowball.hasMetadata(META_TNT_GUN_PROJECTILE)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGunPlace(final BlockPlaceEvent event) {
        final ItemStack itemStack = event.getItemInHand();
        if (!configManager.isTNTGunItem(itemStack)) {
            return;
        }

        final Block block = event.getBlockPlaced();
        block.setMetadata(META_TNT_GUN, new FixedMetadataValue(plugin, META_TNT_GUN));

        if (block.getState() instanceof Dispenser dispenser) {
            final Inventory inventory = dispenser.getInventory();
            final ItemStack blockedItem = configManager.getTntGunBlockedItem().build();
            blockedItem.setAmount(1);

            configManager.getTntGunBlockedSlots().forEach(slot -> inventory.setItem(slot, blockedItem.clone()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFade(final BlockFadeEvent event) {
        final Block block = event.getBlock();
        if (!block.hasMetadata(META_TNT_GUN)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(final InventoryMoveItemEvent event) {
        if (!isBlockedItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispense(final BlockDispenseEvent event) {
        final Block block = event.getBlock();
        if (!block.hasMetadata(META_TNT_GUN)) {
            return;
        }

        if (!isBlockedItem(event.getItem())) {
            return;
        }

        if (!(block.getState() instanceof Dispenser dispenser)) {
            return;
        }

        final Inventory inv = dispenser.getInventory();
        ItemStack ammo = null;

        for (final ItemStack item : inv.getContents()) {
            if (item != null && !item.getType().isAir() && !isBlockedItem(item)) {
                ammo = item;
                break;
            }
        }

        if (ammo == null) {
            event.setCancelled(true);
            return;
        }

        final ItemStack shotItem = ammo.clone();
        shotItem.setAmount(1);
        event.setItem(shotItem);

        SchedulerServices.clientScheduler().runTask(plugin, () -> {
            inv.removeItem(shotItem);

            final ItemStack blocked = configManager.getTntGunBlockedItem().build();
            blocked.setAmount(1);
            inv.addItem(blocked);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperPickup(final InventoryPickupItemEvent event) {
        if (!isBlockedItem(event.getItem().getItemStack())) {
            return;
        }

        event.setCancelled(true);
    }

    private boolean isBlockedItem(final ItemStack itemStack) {
        return itemStack != null
                && itemStack.hasItemMeta()
                && itemStack.getItemMeta().getPersistentDataContainer().has(configManager.getBlockedItemKey(), PersistentDataType.BYTE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(final InventoryClickEvent event) {
        final ItemStack itemStack = event.getCurrentItem();
        if (!isBlockedItem(itemStack)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFromTo(final BlockFromToEvent event) {
        final Block block = event.getBlock();
        if (!block.hasMetadata(META_TNT_GUN)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        cleanExplosionBlocks(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        cleanExplosionBlocks(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        if (!event.isSticky()) {
            return;
        }

        for (final Block block : event.getBlocks()) {
            if (block.hasMetadata(META_TNT_GUN)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void cleanExplosionBlocks(final List<Block> blocks) {
        final Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            final Block block = iterator.next();
            if (!block.hasMetadata(META_TNT_GUN)) {
                continue;
            }

            handleBreakGun(block);
            iterator.remove();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        final Block block = event.getBlock();
        if (!block.hasMetadata(META_TNT_GUN)) {
            return;
        }

        handleBreakGun(block);
        event.setCancelled(true);
    }

    private void handleBreakGun(final Block block) {
        if (block.getState() instanceof Dispenser dispenser) {
            final Inventory inv = dispenser.getInventory();
            final ItemStack[] contents = inv.getContents();

            for (int i = 0; i < contents.length; i++) {
                final ItemStack itemStack = contents[i];
                if (isBlockedItem(itemStack)) {
                    inv.setItem(i, null);
                    continue;
                }

                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    block.getLocation().getWorld().dropItemNaturally(block.getLocation(), itemStack);
                }
            }

            inv.clear();
        }

        final ItemBuilder gunBuilder = configManager.getTntGunBuilder();

        block.getLocation().getWorld().dropItemNaturally(block.getLocation(), gunBuilder.amount(1).build());
        block.removeMetadata(META_TNT_GUN, plugin);
        block.setType(Material.AIR);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTNTBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (!block.hasMetadata(META_TNT_GUN)) {
            return;
        }

        if (!configManager.isTntGunRegionBreak() && event.isCancelled()) {
            return;
        }

        handleBreakGun(block);
        event.setDropItems(false);
    }
}
