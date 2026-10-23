package ru.loper.suntnt.service;

import dev.espi.protectionstones.ProtectionStones;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.model.CustomTNT;
import ru.loper.suntnt.config.TNTConfigManager;

@RequiredArgsConstructor
public class TNTExplosionService {

    private final SunTNT plugin;
    private final TNTConfigManager configManager;
    private final TNTProtectionService protectionService;
    private final TNTExplosionGeometryService geometryService;
    private final TNTSpawnerService spawnerService;
    private final TNTIceService iceService;

    public boolean handleWorldGuardFlags(final EntityExplodeEvent event) {
        return protectionService.handleWorldGuardFlags(event);
    }

    public void handleVanillaExplosion(final EntityExplodeEvent event) {
        if (plugin.isProtectionStonesStatus() && configManager.isDisableProtectionBlocksExplosion()) {
            event.blockList().removeIf(ProtectionStones::isProtectBlock);
        }

        spawnerService.removeSpawnersExplosion(event);
        transformProtectionBlocks(event);
    }

    public void handleCustomTNT(final EntityExplodeEvent event, final CustomTNT customTnt) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final Entity entity = event.getEntity();
        final Location location = entity.getLocation();

        if (customTnt.getSpawnerChance() > 0) {
            spawnerService.handleSpawnerTnt(event, customTnt);
            event.blockList().clear();
            return;
        }

        spawnerService.removeSpawnersExplosion(event);

        if (entity.isInWater() && random.nextInt(100) >= customTnt.getWaterChance()) {
            event.blockList().clear();
            return;
        }

        geometryService.applyExplosionShape(event, customTnt, location);

        if (customTnt.isIgnoreWater()) {
            geometryService.addBlocksBehindWater(event, location, customTnt.getExplosionPower());
            event.blockList().removeIf(geometryService::containsWater);
        }

        if (customTnt.isPreserveUnbreakableBlocks()) {
            event.blockList().removeIf(block -> protectionService.isUnbreakableBlock(block.getType()));
        }

        final Set<Block> blastFootprint = geometryService.getBlastFootprint(event, location, customTnt);
        addSpecialBlocks(event, customTnt, location, blastFootprint);

        final Set<Location> transformedBlocks = customTnt.isPreserveUnbreakableBlocks()
                ? Collections.emptySet()
                : transformProtectionBlocks(event);

        applyTransformableBlocks(event, customTnt, location, transformedBlocks);
        applyBreakableBlocks(event, customTnt, location, transformedBlocks, random);

        if (customTnt.isIce()) {
            iceService.createIceSphere(location, customTnt.getIceRadius(), customTnt.getIceDelay());
        }

        if (plugin.isProtectionStonesStatus()) {
            protectionService.handleProtectionStones(event, customTnt);
        }
    }

    public void dropContainerContents(final EntityExplodeEvent event) {
        for (final Block block : event.blockList()) {
            if (!(block.getState() instanceof Container container)) {
                continue;
            }

            final Inventory inventory = container instanceof Chest chest
                    ? chest.getBlockInventory()
                    : container.getInventory();

            for (final ItemStack item : inventory.getContents()) {
                if (item != null && !item.getType().isAir()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item.clone());
                }
            }

            inventory.clear();
        }
    }

    private void addSpecialBlocks(
            final EntityExplodeEvent event, final CustomTNT customTnt, final Location location, final Set<Block> blastFootprint) {
        final Set<Block> listed = new HashSet<>(event.blockList());
        final Set<Block> candidates = new HashSet<>(blastFootprint);

        if (customTnt.getExplosiveMode() != CustomTNT.ExplosiveMode.VANILLA) {
            candidates.addAll(geometryService.getExplosionBlocks(location, customTnt.getExplosionPower(), customTnt.getExplosiveMode()));
        } else {
            final int specialRadius = Math.max(
                    customTnt.getBreakableBlocks().values().stream()
                            .mapToInt(CustomTNT.BreakableBlockSettings::radius)
                            .max().orElse(0),
                    customTnt.getTransformableBlocks().values().stream()
                            .mapToInt(CustomTNT.TransformableBlockSettings::radius)
                            .max().orElse(0));
            candidates.addAll(geometryService.getExplosionBlocks(location, specialRadius, customTnt.getExplosiveMode()));
        }

        for (final Block block : candidates) {
            if (!geometryService.isWithinRadius(location, block, customTnt.getExplosionPower(), customTnt.getExplosiveMode())) {
                continue;
            }

            final boolean protectionBlock = plugin.isProtectionStonesStatus() && ProtectionStones.isProtectBlock(block);
            final boolean layerBlock = !protectionBlock
                    && !customTnt.isPreserveUnbreakableBlocks()
                    && customTnt.getExplosiveMode() == CustomTNT.ExplosiveMode.VANILLA
                    && configManager.getProtectionBlocks().containsKey(block.getType());

            if ((!protectionBlock || !customTnt.isBreakPSRegion()) && !layerBlock) {
                continue;
            }

            if (!listed.add(block) || !protectionService.isWorldGuardExplosionAllowed(block)) {
                continue;
            }

            event.blockList().add(block);
        }
    }

    private Set<Location> transformProtectionBlocks(final EntityExplodeEvent event) {
        final Set<Location> transformed = new HashSet<>();
        final Map<Material, Material> settings = configManager.getProtectionBlocks();
        if (settings.isEmpty()) {
            return transformed;
        }

        final Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            final Block block = iterator.next();
            final Material target = settings.get(block.getType());
            if (target == null || (plugin.isProtectionStonesStatus() && ProtectionStones.isProtectBlock(block))) {
                continue;
            }

            if (!protectionService.isWorldGuardExplosionAllowed(block)) {
                continue;
            }

            transformed.add(block.getLocation());
            block.setType(target, false);
            iterator.remove();
        }

        return transformed;
    }

    private void applyTransformableBlocks(
            final EntityExplodeEvent event,
            final CustomTNT customTnt,
            final Location location,
            final Set<Location> transformed) {
        final Map<Material, CustomTNT.TransformableBlockSettings> settings = customTnt.getTransformableBlocks();
        if (settings.isEmpty()) {
            return;
        }

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        event.blockList().removeIf(block -> settings.containsKey(block.getType())
                && (!plugin.isProtectionStonesStatus() || !ProtectionStones.isProtectBlock(block)));

        final int maxRadius = settings.values().stream()
                .mapToInt(CustomTNT.TransformableBlockSettings::radius)
                .max()
                .orElse(0);

        for (final Block block : geometryService.getExplosionBlocks(location, maxRadius, customTnt.getExplosiveMode())) {
            if (transformed.contains(block.getLocation())) {
                continue;
            }

            final CustomTNT.TransformableBlockSettings setting = settings.get(block.getType());
            if (setting == null
                    || !geometryService.isWithinRadius(location, block, setting.radius(), customTnt.getExplosiveMode())
                    || protectionService.isManualBlockModificationProtected(block)
                    || random.nextInt(100) >= setting.chance()) {
                continue;
            }

            transformed.add(block.getLocation());
            block.setType(setting.target(), false);
        }
    }

    private void applyBreakableBlocks(
            final EntityExplodeEvent event,
            final CustomTNT customTnt,
            final Location location,
            final Set<Location> transformedBlocks,
            final ThreadLocalRandom random) {
        final Map<Material, CustomTNT.BreakableBlockSettings> settings = customTnt.getBreakableBlocks();
        if (settings.isEmpty()) {
            return;
        }

        event.blockList().removeIf(block -> settings.containsKey(block.getType())
                && (!plugin.isProtectionStonesStatus() || !ProtectionStones.isProtectBlock(block)));

        final int maxRadius = settings.values().stream()
                .mapToInt(CustomTNT.BreakableBlockSettings::radius)
                .max()
                .orElse(0);

        for (final Block block : geometryService.getExplosionBlocks(location, maxRadius, customTnt.getExplosiveMode())) {
            if (transformedBlocks.contains(block.getLocation())) {
                continue;
            }

            final CustomTNT.BreakableBlockSettings setting = settings.get(block.getType());
            if (setting == null
                    || !geometryService.isWithinRadius(location, block, setting.radius(), customTnt.getExplosiveMode())
                    || protectionService.isManualBlockModificationProtected(block)
                    || random.nextInt(100) >= setting.chance()) {
                continue;
            }

            if (!block.isLiquid() && random.nextInt(100) < setting.dropChance()) {
                block.getDrops().forEach(drop -> block.getWorld().dropItemNaturally(block.getLocation(), drop));
            }

            block.setType(Material.AIR, false);
        }
    }
}
