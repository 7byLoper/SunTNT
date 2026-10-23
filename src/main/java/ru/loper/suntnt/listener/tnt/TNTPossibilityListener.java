package ru.loper.suntnt.listener.tnt;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.model.CustomTNT;
import ru.loper.suntnt.service.TNTExplosionService;

@RequiredArgsConstructor
public class TNTPossibilityListener implements Listener {

    private final SunTNT plugin;
    private final TNTExplosionService explosionService;

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        final Entity entity = event.getEntity();
        final CustomTNT customTnt = plugin.getTntManager().getCustomTNT(entity);

        if (explosionService.handleWorldGuardFlags(event)) {
            return;
        }

        if (customTnt == null) {
            explosionService.handleVanillaExplosion(event);
            return;
        }

        explosionService.handleCustomTNT(event, customTnt);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNoDropExplosion(final EntityExplodeEvent event) {
        final CustomTNT customTnt = plugin.getTntManager().getCustomTNT(event.getEntity());
        if (customTnt == null || customTnt.isDropBlocks()) {
            return;
        }

        event.setYield(0);
        explosionService.dropContainerContents(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCustomTntDamage(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof TNTPrimed tntPrimed)) {
            return;
        }

        final CustomTNT customTnt = plugin.getTntManager().getCustomTNT(tntPrimed);
        if (customTnt == null) {
            return;
        }

        final int cutDamage = customTnt.getCutDamage();
        if (cutDamage >= 100) {
            event.setCancelled(true);
            return;
        }

        if (cutDamage > 0) {
            event.setDamage(event.getDamage() * (100 - cutDamage) / 100.0D);
        }
    }
}
