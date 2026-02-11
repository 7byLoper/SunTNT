package ru.loper.suntnt.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
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
import ru.loper.suncore.api.items.ItemBuilder;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.config.TNTConfigManager;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class TNTGunListener implements Listener {
    private final SunTNT plugin;
    private final TNTConfigManager configManager;

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }

        if (!snowball.hasMetadata("TNTGunProjectile")) {
            return;
        }

        if (event.getHitBlock() == null) {
            event.setCancelled(true);
            return;
        }

        for (Entity passenger : snowball.getPassengers()) {
            if (!(passenger instanceof TNTPrimed tntPrimed)) continue;
            tntPrimed.setFuseTicks(0);
        }

        snowball.remove();
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }

        if (!snowball.hasMetadata("TNTGunProjectile")) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGunPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();
        if (!configManager.isTNTGunItem(itemStack)) {
            return;
        }

        Block block = event.getBlockPlaced();
        block.setMetadata("TNTGun", new FixedMetadataValue(plugin, "TNTGun"));
        if (block.getState() instanceof Dispenser dispenser) {
            Inventory inventory = dispenser.getInventory();
            ItemStack blockedItem = configManager.getTntGunBlockedItem().build();
            blockedItem.setAmount(1);

            configManager.getTntGunBlockedSlots().forEach(slot -> inventory.setItem(slot, blockedItem.clone()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFade(org.bukkit.event.block.BlockFadeEvent event) {
        Block block = event.getBlock();
        if (!block.hasMetadata("TNTGun")) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (!isBlockedItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (!block.hasMetadata("TNTGun")) {
            return;
        }

        if (!isBlockedItem(event.getItem())) {
            return;
        }
        if (!(block.getState() instanceof Dispenser dispenser)) {
            return;
        }

        Inventory inv = dispenser.getInventory();
        ItemStack ammo = null;

        for (ItemStack item : inv.getContents()) {
            if (item != null && !item.getType().isAir() && !isBlockedItem(item)) {
                ammo = item;
                break;
            }
        }

        if (ammo == null) {
            event.setCancelled(true);
            return;
        }

        ItemStack shotItem = ammo.clone();
        shotItem.setAmount(1);
        event.setItem(shotItem);

        Bukkit.getScheduler().runTask(plugin, () -> {
            inv.removeItem(shotItem);

            ItemStack blocked = configManager.getTntGunBlockedItem().build();
            blocked.setAmount(1);
            inv.addItem(blocked);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (!isBlockedItem(event.getItem().getItemStack())) {
            return;
        }

        event.setCancelled(true);
    }

    private boolean isBlockedItem(ItemStack itemStack) {
        return itemStack != null &&
                itemStack.hasItemMeta() &&
                itemStack.getItemMeta().getPersistentDataContainer().has(configManager.getBlockedItemKey(), PersistentDataType.BYTE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (!isBlockedItem(itemStack)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFromTo(org.bukkit.event.block.BlockFromToEvent event) {
        Block block = event.getBlock();
        if (!block.hasMetadata("TNTGun")) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        cleanExplosionBlocks(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        cleanExplosionBlocks(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) return;

        for (Block block : event.getBlocks()) {
            if (!block.hasMetadata("TNTGun")) {
                continue;
            }

            event.setCancelled(true);
            return;
        }
    }

    private void cleanExplosionBlocks(List<Block> blocks) {
        List<Block> blocksToRemove = new ArrayList<>();

        for (Block block : blocks) {
            if (!block.hasMetadata("TNTGun")) {
                continue;
            }

            handleBreakGun(block);
            blocksToRemove.add(block);
        }

        blocks.removeAll(blocksToRemove);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Block block = event.getBlock();
        if (!block.hasMetadata("TNTGun")) {
            return;
        }

        handleBreakGun(block);
        event.setCancelled(true);
    }

    private void handleBreakGun(Block block) {
        if (block.getState() instanceof Dispenser dispenser) {
            Inventory inv = dispenser.getInventory();

            ItemStack[] contents = inv.getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack itemStack = contents[i];
                if (isBlockedItem(contents[i])) {
                    inv.setItem(i, null);
                    continue;
                }

                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    block.getLocation().getWorld().dropItemNaturally(block.getLocation(), itemStack);
                }
            }

            inv.clear();
        }

        ItemBuilder gunBuilder = configManager.getTntGunBuilder();

        block.getLocation().getWorld().dropItemNaturally(block.getLocation(), gunBuilder.amount(1).build());
        block.removeMetadata("TNTGun", plugin);
        block.setType(Material.AIR);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTNTBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!block.hasMetadata("TNTGun")) {
            return;
        }

        if (!configManager.isTntGunRegionBreak() && event.isCancelled()) {
            return;
        }

        handleBreakGun(block);
        event.setDropItems(false);
    }

}
