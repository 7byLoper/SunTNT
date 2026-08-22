package ru.loper.suntnt.listener.tnt;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suncore.api.scheduler.SchedulerServices;
import ru.loper.suncore.api.sound.SoundPlayer;
import ru.loper.sunholygoldspawner.api.models.spawner.SpawnerData;
import ru.loper.sunmysteriouseggs.SunMysteriousEggs;
import ru.loper.sunmysteriouseggs.config.MysteriousEggConfig;
import ru.loper.sunmysteriouseggs.manager.MysteriousSpawnerManager;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.hook.WorldGuardHook;
import ru.loper.suntnt.api.model.CustomTNT;
import ru.loper.suntnt.config.TNTConfigManager;
import ru.loper.suntnt.handler.FlagHandler;

@RequiredArgsConstructor
public class TNTPossibilityListener implements Listener {
    private static final Set<Material> PROTECTED_BLOCKS = EnumSet.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.END_PORTAL_FRAME,
            Material.END_PORTAL,
            Material.ANCIENT_DEBRIS,
            Material.NETHERITE_BLOCK,
            Material.OBSIDIAN,
            Material.CRYING_OBSIDIAN);
    private final SunTNT plugin;
    private final TNTConfigManager configManager;
    private final Cache<Location, BlockData> waterBlocks =
            CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.MINUTES).build();

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        CustomTNT customTnt = plugin.getTntManager().getCustomTNT(entity);

        if (handleWorldGuardFlags(event)) {
            return;
        }

        if (customTnt == null) {
            if (plugin.isProtectionStonesStatus()) {
                handleProtectionStonesDefault(event);
            }

            removeSpawnersExplosion(event);
            return;
        }

        handleCustomTNT(event, customTnt);
    }

    private boolean handleWorldGuardFlags(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();

            com.sk89q.worldedit.util.Location blockLocation = BukkitAdapter.adapt(block.getLocation());
            ApplicableRegionSet blockRegions = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .createQuery()
                    .getApplicableRegions(blockLocation);

            if (blockRegions.queryState(null, FlagHandler.CANCEL_NEARBY_EXPLOSION) == StateFlag.State.ALLOW) {
                event.setCancelled(true);
                return true;
            }

            if (blockRegions.queryState(null, FlagHandler.TNT_BLOCK_EXPLOSION) == StateFlag.State.DENY) {
                iterator.remove();
            }
        }

        return false;
    }

    private void handleCustomTNT(EntityExplodeEvent event, CustomTNT customTnt) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Entity entity = event.getEntity();
        Location location = entity.getLocation();

        if (customTnt.getSpawnerChance() > 0) {
            handleSpawnerTnt(event, customTnt);
            event.blockList().clear();
            return;
        }

        removeSpawnersExplosion(event);

        if (entity.isInWater() && random.nextInt(0, 100) > customTnt.getWaterChance()) {
            event.blockList().clear();
            return;
        }

        applyExplosionShape(event, customTnt, location);
        Set<Location> transformedBlocks = applyTransformableBlocks(event, customTnt, location);
        applyBreakableBlocks(event, customTnt, location, transformedBlocks, random);

        if (customTnt.isIce()) {
            createIceSphere(location, customTnt.getIceRadius(), customTnt.getIceDelay());
        }

        if (plugin.isProtectionStonesStatus()) {
            handleProtectionStones(event, customTnt);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCustomTntDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof org.bukkit.entity.TNTPrimed tntPrimed)) return;

        CustomTNT customTnt = plugin.getTntManager().getCustomTNT(tntPrimed);
        if (customTnt == null) return;

        int cutDamage = customTnt.getCutDamage();
        if (cutDamage >= 100) {
            event.setCancelled(true);
        } else if (cutDamage > 0) {
            event.setDamage(event.getDamage() * (100 - cutDamage) / 100.0D);
        }
    }

    private void applyExplosionShape(EntityExplodeEvent event, CustomTNT customTnt, Location location) {
        if (customTnt.getExplosiveMode() == CustomTNT.ExplosiveMode.VANILLA) return;

        List<Block> blocks = customTnt.getExplosiveMode() == CustomTNT.ExplosiveMode.BALL
                ? getNearbySphereBlocks(location, customTnt.getExplosionPower())
                : getNearbyBlocks(location, customTnt.getExplosionPower());
        Set<Block> listedBlocks = new HashSet<>(event.blockList());
        event.blockList()
                .addAll(blocks.stream()
                        .filter(block -> !block.getType().isAir())
                        .filter(block -> !PROTECTED_BLOCKS.contains(block.getType()))
                        .filter(block -> !WorldGuardHook.hasRegionAtLocation(block.getLocation()))
                        .filter(listedBlocks::add)
                        .toList());
    }

    private Set<Location> applyTransformableBlocks(EntityExplodeEvent event, CustomTNT customTnt, Location location) {
        Map<Material, CustomTNT.TransformableBlockSettings> settings = customTnt.getTransformableBlocks();
        if (settings.isEmpty()) return Collections.emptySet();

        event.blockList().removeIf(block -> settings.containsKey(block.getType()));
        Set<Location> transformed = new HashSet<>();
        int maxRadius = settings.values().stream()
                .mapToInt(CustomTNT.TransformableBlockSettings::getRadius)
                .max()
                .orElse(0);

        for (Block block : getExplosionBlocks(location, maxRadius, customTnt.getExplosiveMode())) {
            CustomTNT.TransformableBlockSettings setting = settings.get(block.getType());
            if (setting == null
                    || !isWithinRadius(location, block, setting.getRadius(), customTnt.getExplosiveMode())
                    || isManualBlockModificationProtected(block, customTnt)) continue;

            transformed.add(block.getLocation());
            block.setType(setting.getTarget(), false);
        }
        return transformed;
    }

    private void applyBreakableBlocks(
            EntityExplodeEvent event,
            CustomTNT customTnt,
            Location location,
            Set<Location> transformedBlocks,
            ThreadLocalRandom random) {
        Map<Material, CustomTNT.BreakableBlockSettings> settings = customTnt.getBreakableBlocks();
        if (settings.isEmpty()) return;

        event.blockList().removeIf(block -> settings.containsKey(block.getType()));
        int maxRadius = settings.values().stream()
                .mapToInt(CustomTNT.BreakableBlockSettings::getRadius)
                .max()
                .orElse(0);

        for (Block block : getExplosionBlocks(location, maxRadius, customTnt.getExplosiveMode())) {
            if (transformedBlocks.contains(block.getLocation())) continue;
            CustomTNT.BreakableBlockSettings setting = settings.get(block.getType());
            if (setting == null
                    || !isWithinRadius(location, block, setting.getRadius(), customTnt.getExplosiveMode())
                    || isManualBlockModificationProtected(block, customTnt)
                    || random.nextInt(100) >= setting.getChance()) continue;

            if (!block.isLiquid() && random.nextInt(100) < setting.getDropChance()) {
                block.getDrops().forEach(drop -> block.getWorld().dropItemNaturally(block.getLocation(), drop));
            }
            block.setType(Material.AIR, false);
        }
    }

    private List<Block> getExplosionBlocks(Location location, int radius, CustomTNT.ExplosiveMode mode) {
        return mode == CustomTNT.ExplosiveMode.CUBE
                ? getNearbyBlocks(location, radius)
                : getNearbySphereBlocks(location, radius);
    }

    private boolean isWithinRadius(Location center, Block block, int radius, CustomTNT.ExplosiveMode mode) {
        if (mode == CustomTNT.ExplosiveMode.CUBE) {
            return Math.abs(block.getX() - center.getBlockX()) <= radius
                    && Math.abs(block.getY() - center.getBlockY()) <= radius
                    && Math.abs(block.getZ() - center.getBlockZ()) <= radius;
        }
        int x = block.getX() - center.getBlockX();
        int y = block.getY() - center.getBlockY();
        int z = block.getZ() - center.getBlockZ();
        return x * x + y * y + z * z <= radius * radius;
    }

    private boolean isManualBlockModificationProtected(Block block, CustomTNT customTnt) {
        if (WorldGuardHook.hasRegionAtLocation(block.getLocation())) {
            return true;
        }
        if (!plugin.isProtectionStonesStatus() || !ProtectionStones.isProtectBlock(block)) {
            return false;
        }

        PSRegion region = PSRegion.fromLocation(block.getLocation());
        if (region == null) {
            return false;
        }
        if (customTnt.isBreakPSRegion()) {
            region.removeStrength(1);
        }
        return true;
    }

    private void handleProtectionStones(EntityExplodeEvent event, CustomTNT customTNT) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();

            if (!ProtectionStones.isProtectBlock(block)) {
                continue;
            }

            PSRegion region = PSRegion.fromLocation(block.getLocation());
            if (region == null) {
                continue;
            }

            if (customTNT.isBreakPSRegion()) {
                region.removeStrength(1);
            }

            iterator.remove();
        }
    }

    private void removeSpawnersExplosion(EntityExplodeEvent event) {
        if (!configManager.isDisableSpawnerExplosion()) return;
        event.blockList().removeIf(block -> block.getType().equals(Material.SPAWNER));
    }

    private void handleProtectionStonesDefault(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();

            if (!ProtectionStones.isProtectBlock(block)) {
                continue;
            }

            PSRegion region = PSRegion.fromLocation(block.getLocation());
            if (region == null) {
                continue;
            }

            if (!configManager.isDisableProtectionBlocksExplosion()) {
                region.removeStrength(1);
            }

            iterator.remove();
        }
    }

    private void handleSpawnerTnt(EntityExplodeEvent event, CustomTNT customTnt) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextInt(100) > customTnt.getSpawnerChance()) return;

        for (Block block : event.blockList()) {
            if (!block.getType().equals(Material.SPAWNER)) continue;

            ItemStack spawnerItem = getSpawner(customTnt, block, random);
            if (spawnerItem != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
            }
            block.setType(Material.AIR);
            break;
        }
    }

    private ItemStack getSpawner(CustomTNT customTnt, Block block, ThreadLocalRandom random) {
        if (plugin.isHolyGoldSpawnerStatus()) {
            if (plugin.getGoldSpawnerHook().isGoldSpawner(block)) {
                plugin.getGoldSpawnerHook().removeSpawnerData(block);

                if (random.nextInt(0, 100) <= customTnt.getGoldSpawnerChance()) {
                    SpawnerData spawnerData = plugin.getGoldSpawnerHook().getGoldSpawnerData(block);
                    return plugin.getGoldSpawnerHook().getSpawnerItemManager().createItem(spawnerData);
                }
            }
        }

        if (plugin.isMysteriousEggsStatus()) {
            MysteriousSpawnerManager spawnerManager =
                    SunMysteriousEggs.getInstance().getSpawnerManager();
            MysteriousEggConfig eggConfig = spawnerManager.getSpawner(block.getLocation());

            if (eggConfig != null) {
                spawnerManager.removeSpawner(block.getLocation());

                if (random.nextInt(0, 100) <= customTnt.getMysteriousSpawnerChance()) {
                    ItemBuilder dropBuilder = eggConfig.spawnerBuilder();
                    if (dropBuilder != null) {
                        return dropBuilder.build();
                    }
                }
            }
        }

        if (random.nextInt(0, 100) <= customTnt.getSpawnerMobSaveChance()) {
            CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();
            EntityType entityType = creatureSpawner.getSpawnedType();
            return createSpawnerItemStack(entityType, customTnt);
        }

        return new ItemStack(Material.SPAWNER);
    }

    public ItemStack createSpawnerItemStack(EntityType entityType, CustomTNT customTNT) {
        ItemBuilder itemBuilder = new ItemBuilder(customTNT.getSpawnerForm().build());

        itemBuilder.name(itemBuilder.name().replace("{mob}", configManager.getEntityTranslation(entityType)));

        BlockStateMeta blockStateMeta = (BlockStateMeta) itemBuilder.meta();
        if (blockStateMeta == null) {
            return null;
        }

        CreatureSpawner spawner = (CreatureSpawner) blockStateMeta.getBlockState();
        spawner.setSpawnedType(entityType);
        blockStateMeta.setBlockState(spawner);

        itemBuilder.meta(blockStateMeta);
        return itemBuilder.build();
    }

    private void createIceSphere(Location center, int radius, long delay) {
        World world = center.getWorld();
        if (world == null) return;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.sqrt(x * x + y * y + z * z) > radius) continue;

                    Block block = world.getBlockAt(center.clone().add(x, y, z));
                    if (!isIceReplaceable(block)) continue;

                    if (block.getType() == Material.WATER || block.getType() == Material.BUBBLE_COLUMN) {
                        waterBlocks.put(block.getLocation(), block.getBlockData());
                    }

                    block.setType(Material.ICE);
                    scheduleIceRemoval(block.getLocation(), delay);
                }
            }
            delay--;
        }
    }

    private boolean isIceReplaceable(Block block) {
        Material type = block.getType();
        return type.isAir() || type == Material.WATER || type == Material.BUBBLE_COLUMN;
    }

    private void scheduleIceRemoval(Location location, long delay) {
        SchedulerServices.clientScheduler()
                .runTaskLater(
                        plugin,
                        () -> {
                            Block block = location.getBlock();
                            if (block.getType() == Material.ICE) {
                                BlockData previousBlockData = waterBlocks.getIfPresent(block.getLocation());
                                if (previousBlockData != null) {
                                    block.setBlockData(previousBlockData, false);
                                    waterBlocks.invalidate(block.getLocation());
                                } else {
                                    block.setType(Material.AIR);
                                }
                                location.getNearbyEntities(15.0, 15.0, 15.0).forEach(entity -> {
                                    if (entity instanceof Player nearbyPlayer) {
                                        SoundPlayer.play(nearbyPlayer, location, Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
                                    }
                                });
                            }
                        },
                        delay);
    }

    private List<Block> getNearbyBlocks(Location location, int radius) {
        List<Block> blocks = new ArrayList<>();
        World world = location.getWorld();

        if (world == null) {
            return blocks;
        }

        int centerX = location.getBlockX();
        int centerY = location.getBlockY();
        int centerZ = location.getBlockZ();

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }

        return blocks;
    }

    private List<Block> getNearbySphereBlocks(Location location, int radius) {
        List<Block> blocks = new ArrayList<>();
        World world = location.getWorld();

        if (world == null) {
            return blocks;
        }

        int centerX = location.getBlockX();
        int centerY = location.getBlockY();
        int centerZ = location.getBlockZ();

        int radiusSquared = radius * radius;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    int dx = x - centerX;
                    int dy = y - centerY;
                    int dz = z - centerZ;

                    if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                        continue;
                    }

                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }

        return blocks;
    }
}
