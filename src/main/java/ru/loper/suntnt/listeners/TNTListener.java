package ru.loper.suntnt.listeners;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.sunprotectionstones.PSRegion;
import ru.loper.sunprotectionstones.SunProtectionStones;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.tnt.CustomTNT;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class TNTListener implements Listener {
    private final SunTNT plugin;
    private final Cache<Block, CustomTNT> cachedTNTs = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();
    private final Cache<Location, Integer> waterBlocks = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.MINUTES).build();

    public TNTListener(SunTNT plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (!entity.hasMetadata("TNTType")) {
            return;
        }
        String tntType = (entity.getMetadata("TNTType").get(0)).asString();
        CustomTNT customTnt = plugin.getTntManager().getCustomTNT(tntType);
        if (customTnt == null) {
            event.blockList().removeIf(block -> block.getType().equals(Material.SPAWNER));
            return;
        }
        if (customTnt.getSpawnerChance() > 0) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (random.nextInt(0, 101) <= customTnt.getSpawnerChance() || !customTnt.isSpawnerAlwaysSaveMob()) {
                for (Block block2 : event.blockList()) {
                    if (customTnt.getSpawnerChance() <= 0 || !block2.getType().equals(Material.SPAWNER)) continue;
                    ItemStack mobSpawner = new ItemStack(Material.SPAWNER, 1);
                    if (random.nextInt(0, 101) <= customTnt.getSpawnerChance() || customTnt.isSpawnerAlwaysSaveMob()) {
                        CreatureSpawner creatureSpawner = (CreatureSpawner) block2.getState();
                        EntityType entityType = creatureSpawner.getSpawnedType();
                        mobSpawner = createSpawnerItemStack(entityType);
                    }
                    block2.getWorld().dropItemNaturally(block2.getLocation(), mobSpawner);

                    block2.setType(Material.AIR);
                    break;
                }
            }
            event.blockList().clear();
        } else {
            event.blockList().removeIf(block -> block.getType().equals(Material.SPAWNER));
        }
        if (customTnt.getObsidianChance() > 0) {
            event.blockList().addAll(getNearbyBlocks(entity.getLocation(), 2).stream().filter(block -> block.getType().equals(Material.OBSIDIAN) || block.getType().equals(Material.CRYING_OBSIDIAN) || block.getType().equals(Material.ANCIENT_DEBRIS) || block.getType().equals(Material.NETHERITE_BLOCK) || block.getType().equals(Material.ENDER_CHEST) || block.getType().equals(Material.ENCHANTING_TABLE) || block.getType().equals(Material.ANVIL)).toList());
        }
        if (customTnt.getLiquidChance() > 0) {
            event.blockList().addAll(getNearbyBlocks(entity.getLocation(), 2).stream().filter(block -> !block.getType().isAir() && !block.getType().equals(Material.BEDROCK) && !block.getType().equals(Material.BARRIER) && !block.getType().equals(Material.COMMAND_BLOCK) && !block.getType().equals(Material.END_PORTAL_FRAME) && !block.getType().equals(Material.END_PORTAL) && !block.getType().equals(Material.ANCIENT_DEBRIS) && !block.getType().equals(Material.NETHERITE_BLOCK) && !block.getType().equals(Material.OBSIDIAN) && !block.getType().equals(Material.CRYING_OBSIDIAN)).toList());
        } else {
            event.blockList().removeIf(Block::isLiquid);
        }
        if (customTnt.getBlocksRadius() > 0) {
            event.blockList().addAll(getNearbyBlocks(entity.getLocation(), customTnt.getBlocksRadius()).stream().filter(block -> !block.getType().isAir() && !block.getType().equals(Material.BEDROCK) && !block.getType().equals(Material.BARRIER) && !block.getType().equals(Material.COMMAND_BLOCK) && !block.getType().equals(Material.END_PORTAL_FRAME) && !block.getType().equals(Material.END_PORTAL) && !block.getType().equals(Material.ANCIENT_DEBRIS) && !block.getType().equals(Material.NETHERITE_BLOCK) && !block.getType().equals(Material.OBSIDIAN) && !block.getType().equals(Material.CRYING_OBSIDIAN)).toList());
        }
        if (customTnt.isIce()) {
            createIceSphere(entity.getLocation(), customTnt.getIceRadius(), customTnt.getIceDelay());
        }
        if (customTnt.isBreakPSRegion() && SunTNT.getInstance().isProtectionStonesStatus()) {
            for (Block block : event.blockList()) {
                if (!SunProtectionStones.isProtectBlock(block)) continue;
                PSRegion rg = PSRegion.fromLocation(block.getLocation());
                if (rg == null) continue;
                event.blockList().remove(block);
                rg.removeStrength(1);
            }
        }
    }

    public ItemStack createSpawnerItemStack(EntityType entityType) {
        ItemStack spawnerItem = new ItemStack(Material.SPAWNER);
        BlockStateMeta blockStateMeta = (BlockStateMeta) spawnerItem.getItemMeta();
        if (blockStateMeta == null) {
            return null;
        }
        CreatureSpawner spawner = (CreatureSpawner) blockStateMeta.getBlockState();
        spawner.setSpawnedType(entityType);
        blockStateMeta.setBlockState(spawner);
        spawnerItem.setItemMeta(blockStateMeta);
        return spawnerItem;
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
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
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

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTNTPrime(ExplosionPrimeEvent event) {
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

    @EventHandler
    public void onSpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof TNTPrimed tntPrimed)) {
            return;
        }
        CustomTNT customTnt = cachedTNTs.getIfPresent(event.getLocation().getBlock());
        if (customTnt == null) {
            return;
        }
        tntPrimed.setFuseTicks(customTnt.getFuseTicks());
        entity.setMetadata("TNTType", new FixedMetadataValue(plugin, customTnt.getName()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
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
        event.setDropItems(false);
        block.getLocation().getWorld().dropItemNaturally(block.getLocation(), customTnt.getItemStack());
        block.removeMetadata("TNTType", plugin);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (!entity.getType().equals(EntityType.DROPPED_ITEM)) {
            return;
        }
        if (cause.equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) || cause.equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
            event.setCancelled(true);
        }
    }

    private List<Block> getNearbyBlocks(Location location, int radius) {
        ArrayList<Block> blocks = new ArrayList<>();
        for (int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; ++x) {
            for (int y = location.getBlockY() - radius; y <= location.getBlockY() + radius; ++y) {
                for (int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; ++z) {
                    blocks.add(new Location(location.getWorld(), x, y, z).getBlock());
                }
            }
        }
        return blocks;
    }
}
