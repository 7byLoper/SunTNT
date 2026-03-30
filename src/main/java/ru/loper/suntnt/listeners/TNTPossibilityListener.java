package ru.loper.suntnt.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import lombok.RequiredArgsConstructor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suncore.api.scheduler.SchedulerServices;
import ru.loper.sunholyitems.SunHolyItems;
import ru.loper.sunholyitems.api.modules.blocks.impl.GoldSpawner;
import ru.loper.sunholyitems.manager.GoldSpawnerManager;
import ru.loper.sunmysteriouseggs.SunMysteriousEggs;
import ru.loper.sunmysteriouseggs.config.MysteriousEggConfig;
import ru.loper.sunmysteriouseggs.manager.MysteriousSpawnerManager;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.hook.WorldGuardHook;
import ru.loper.suntnt.api.modules.CustomTNT;
import ru.loper.suntnt.config.TNTConfigManager;
import ru.loper.suntnt.utils.FlagHandler;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class TNTPossibilityListener implements Listener {
    private static final Set<Material> PROTECTED_BLOCKS = EnumSet.of(
            Material.BEDROCK, Material.BARRIER, Material.COMMAND_BLOCK,
            Material.END_PORTAL_FRAME, Material.END_PORTAL, Material.ANCIENT_DEBRIS,
            Material.NETHERITE_BLOCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN
    );
    private static final Set<Material> OBSIDIAN_TYPE_BLOCKS = EnumSet.of(
            Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.ANCIENT_DEBRIS,
            Material.NETHERITE_BLOCK, Material.ENDER_CHEST, Material.ENCHANTING_TABLE, Material.RESPAWN_ANCHOR
    );
    private final SunTNT plugin;
    private final TNTConfigManager configManager;
    private final Cache<Location, Integer> waterBlocks = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.MINUTES).build();

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
            ApplicableRegionSet blockRegions = WorldGuard.getInstance().getPlatform()
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
        } else {
            removeSpawnersExplosion(event);
        }

        if (entity.isInWater() && random.nextInt(0, 100) > customTnt.getWaterChance()) {
            event.blockList().clear();
            return;
        }

        if (customTnt.getObsidianChance() > 0) {
            event.blockList().addAll(
                    getNearbyBlocks(location, 2)
                            .stream()
                            .filter(block -> OBSIDIAN_TYPE_BLOCKS.contains(block.getType()))
                            .toList()
            );
        }

        if (customTnt.getLiquidChance() > 0) {
            event.blockList().addAll(
                    getNearbyBlocks(location, 2)
                            .stream()
                            .filter(block -> !block.getType().isAir() && !PROTECTED_BLOCKS.contains(block.getType()))
                            .toList()
            );
        } else {
            event.blockList().removeIf(Block::isLiquid);
        }

        if (customTnt.getBlocksRadius() > 0) {
            List<Block> nearbyBlocks = getNearbyBlocks(location, customTnt.getBlocksRadius());
            event.blockList().addAll(
                    nearbyBlocks.stream()
                            .filter(block -> !block.getType().isAir() &&
                                    !PROTECTED_BLOCKS.contains(block.getType()) &&
                                    !WorldGuardHook.hasRegionAtLocation(block.getLocation()))
                            .toList()
            );
        }

        if (customTnt.isIce()) {
            createIceSphere(location, customTnt.getIceRadius(), customTnt.getIceDelay());
        }

        if (plugin.isProtectionStonesStatus()) {
            handleProtectionStones(event, customTnt);
        }
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
        if (random.nextInt(0, 100) > customTnt.getSpawnerChance()) return;

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
        if (plugin.isHolyItemsStatus()) {
            GoldSpawnerManager goldSpawnerManager = SunHolyItems.getInstance().getGoldSpawnerManager();
            GoldSpawner goldSpawner = goldSpawnerManager.getSpawner(block.getLocation());

            if (goldSpawner != null) {
                goldSpawnerManager.removeSpawner(block.getLocation());

                if (random.nextInt(0, 100) <= customTnt.getGoldSpawnerChance()) {
                    ru.loper.suncore.api.items.ItemBuilder dropBuilder = goldSpawner.getItemBuilder();
                    if (dropBuilder != null) {
                        return dropBuilder.build();
                    }
                }
            }
        }

        if (plugin.isMysteriousEggsStatus()) {
            MysteriousSpawnerManager spawnerManager = SunMysteriousEggs.getInstance().getSpawnerManager();
            MysteriousEggConfig eggConfig = spawnerManager.getSpawner(block.getLocation());

            if (eggConfig != null) {
                spawnerManager.removeSpawner(block.getLocation());

                if (random.nextInt(0, 100) <= customTnt.getMysteriousSpawnerChance()) {
                    ru.loper.suncore.api.items.ItemBuilder dropBuilder = eggConfig.spawnerBuilder();
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

        itemBuilder.name(itemBuilder.name()
                .replace("{mob}", configManager.getEntityTranslation(entityType))
        );

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
        if (world != null) {
            for (int y = -radius; y <= radius; ++y) {
                for (int x = -radius; x <= radius; ++x) {
                    for (int z = -radius; z <= radius; ++z) {
                        BlockData blockData;
                        Block block;
                        if (!(Math.sqrt(x * x + y * y + z * z) <= (double) radius) || !(block = world.getBlockAt(center.clone().add(x, y, z))).getType().isAir() && !block.isLiquid())
                            continue;
                        if (block.getType().equals(Material.WATER) && (blockData = block.getBlockData()) instanceof Levelled) {
                            Levelled levelled = (Levelled) (blockData);
                            if (levelled.getLevel() == 0) {
                                waterBlocks.put(block.getLocation(), levelled.getLevel());
                            }
                        } else if (block.getType().equals(Material.BUBBLE_COLUMN)) {
                            waterBlocks.put(block.getLocation(), 0);
                        }
                        block.setType(Material.ICE);
                        scheduleIceRemoval(block.getLocation(), delay);
                    }
                }
                --delay;
            }
        }
    }

    private void scheduleIceRemoval(Location location, long delay) {
        SchedulerServices.clientScheduler().runTaskLater(plugin, () -> {
            Block block = location.getBlock();
            if (block.getType() == Material.ICE) {
                if (waterBlocks.getIfPresent(block.getLocation()) != null) {
                    block.setBlockData(Material.WATER.createBlockData(), false);
                } else {
                    block.setType(Material.AIR);
                }
                location.getNearbyEntities(15.0, 15.0, 15.0).forEach(entity -> {
                    if (entity instanceof Player nearbyPlayer) {
                        nearbyPlayer.playSound(location, Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
                    }
                });
            }
        }, delay);
    }

    private List<Block> getNearbyBlocks(Location location, int radius) {
        ArrayList<Block> blocks = new ArrayList<>();
        for (int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; ++x) {
            for (int y = location.getBlockY() - radius; y <= location.getBlockY() + radius; ++y) {
                for (int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; ++z) {
                    Location loc = new Location(location.getWorld(), x, y, z);
                    blocks.add(loc.getBlock());
                }
            }
        }
        return blocks;
    }

}
