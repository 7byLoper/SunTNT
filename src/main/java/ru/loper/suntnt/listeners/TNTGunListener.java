package ru.loper.suntnt.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import ru.loper.suncore.api.items.ItemBuilder;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.config.TNTConfigManager;

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
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onTNTBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!block.hasMetadata("TNTGun")) {
            return;
        }

        if(!configManager.isTntGunRegionBreak() && event.isCancelled()){
            return;
        }

        ItemBuilder gunBuilder = configManager.getTntGunBuilder();

        block.getLocation().getWorld().dropItemNaturally(block.getLocation(), gunBuilder.amount(1).build());
        block.removeMetadata("TNTGun", plugin);
        block.setType(Material.AIR);

        event.setDropItems(false);
    }

}
