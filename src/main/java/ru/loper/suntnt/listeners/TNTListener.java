package ru.loper.suntnt.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
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
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.tnt.TNT;

public class TNTListener implements Listener {
    private final SunTNT plugin;
    private final Cache<Block, TNT> cachedTNTs = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();
    private final Cache<Location, Integer> waterBlocks = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.MINUTES).build();

    public TNTListener(SunTNT plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (!entity.hasMetadata("TNTType")) {
            return;
        }
        String tntType = (entity.getMetadata("TNTType").get(0)).asString();
        TNT tnt = SunTNT.getTntManager().getTNT(tntType);
        if (tnt == null) {
            event.blockList().removeIf(block -> block.getType().equals(Material.SPAWNER));
            return;
        }
        if (tnt.getSpawnerChance() > 0) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (random.nextInt(0, 101) <= tnt.getSpawnerChance() || !tnt.isSpawnerAlwaysSaveMob()) {
                for (Block block2 : event.blockList()) {
                    if (tnt.getSpawnerChance() <= 0 || !block2.getType().equals(Material.SPAWNER)) continue;
                    ItemStack mobSpawner = new ItemStack(Material.SPAWNER, 1);
                    if (random.nextInt(0, 101) <= tnt.getSpawnerChance() || tnt.isSpawnerAlwaysSaveMob()) {
                        CreatureSpawner creatureSpawner = (CreatureSpawner)block2.getState();
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
        if (tnt.getObsidianChance() > 0) {
            event.blockList().addAll(getNearbyBlocks(entity.getLocation(), 2).stream().filter(block -> block.getType().equals(Material.OBSIDIAN) || block.getType().equals(Material.CRYING_OBSIDIAN) || block.getType().equals(Material.ANCIENT_DEBRIS) || block.getType().equals(Material.NETHERITE_BLOCK) || block.getType().equals(Material.ENDER_CHEST) || block.getType().equals(Material.ENCHANTING_TABLE) || block.getType().equals(Material.ANVIL)).toList());
        }
        if (tnt.getLiquidChance() > 0) {
            event.blockList().addAll(getNearbyBlocks(entity.getLocation(), 2).stream().filter(block -> !block.getType().isAir() && !block.getType().equals(Material.BEDROCK) && !block.getType().equals(Material.BARRIER) && !block.getType().equals(Material.COMMAND_BLOCK) && !block.getType().equals(Material.END_PORTAL_FRAME) && !block.getType().equals(Material.END_PORTAL) && !block.getType().equals(Material.ANCIENT_DEBRIS) && !block.getType().equals(Material.NETHERITE_BLOCK) && !block.getType().equals(Material.OBSIDIAN) && !block.getType().equals(Material.CRYING_OBSIDIAN)).toList());
        } else {
            event.blockList().removeIf(Block::isLiquid);
        }
        if (tnt.getBlocksRadius() > 0) {
            event.blockList().addAll(getNearbyBlocks(entity.getLocation(), tnt.getBlocksRadius()).stream().filter(block -> !block.getType().isAir() && !block.getType().equals(Material.BEDROCK) && !block.getType().equals(Material.BARRIER) && !block.getType().equals(Material.COMMAND_BLOCK) && !block.getType().equals(Material.END_PORTAL_FRAME) && !block.getType().equals(Material.END_PORTAL) && !block.getType().equals(Material.ANCIENT_DEBRIS) && !block.getType().equals(Material.NETHERITE_BLOCK) && !block.getType().equals(Material.OBSIDIAN) && !block.getType().equals(Material.CRYING_OBSIDIAN)).toList());
        }
        if (tnt.isIce()) {
            createIceSphere(entity.getLocation(), tnt.getIceRadius());
        }
    }

    public ItemStack createSpawnerItemStack(EntityType entityType) {
        ItemStack spawnerItem = new ItemStack(Material.SPAWNER);
        BlockStateMeta blockStateMeta = (BlockStateMeta)spawnerItem.getItemMeta();
        if (blockStateMeta == null) {
            return null;
        }
        CreatureSpawner spawner = (CreatureSpawner)blockStateMeta.getBlockState();
        spawner.setSpawnedType(entityType);
        blockStateMeta.setBlockState(spawner);
        spawnerItem.setItemMeta(blockStateMeta);
        return spawnerItem;
    }

    private void createIceSphere(Location center, int radius) {
        World world = center.getWorld();
        if (world != null) {
            long delay = 105L;
            for (int y = -radius; y <= radius; ++y) {
                for (int x = -radius; x <= radius; ++x) {
                    for (int z = -radius; z <= radius; ++z) {
                        BlockData blockData;
                        Block block;
                        if (!(Math.sqrt(x * x + y * y + z * z) <= (double)radius) || !(block = world.getBlockAt(center.clone().add(x, y, z))).getType().isAir() && !block.isLiquid()) continue;
                        if (block.getType().equals(Material.WATER) && (blockData = block.getBlockData()) instanceof Levelled) {
                            Levelled levelled = (Levelled)(blockData);
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

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onTNTPrime(TNTPrimeEvent event) {
        Block block = event.getBlock();
        if (!block.hasMetadata("TNTType")) {
            return;
        }
        String tntType = (block.getMetadata("TNTType").get(0)).asString();
        TNT tnt = SunTNT.getTntManager().getTNT(tntType);
        if (tnt == null) {
            return;
        }
        cachedTNTs.put(block, tnt);
        block.removeMetadata("TNTType", plugin);
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onTNTPrime(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof TNTPrimed)) {
            return;
        }
        if (!entity.hasMetadata("TNTType")) {
            return;
        }
        String tntType = (entity.getMetadata("TNTType").get(0)).asString();
        TNT tnt = SunTNT.getTntManager().getTNT(tntType);
        if (tnt == null) {
            return;
        }
        event.setRadius(tnt.getExplosionRadius());
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof TNTPrimed tntPrimed)) {
            return;
        }
        TNT tnt = cachedTNTs.getIfPresent(event.getLocation().getBlock());
        if (tnt == null) {
            return;
        }
        tntPrimed.setFuseTicks(tnt.getFuseTicks());
        entity.setMetadata("TNTType", new FixedMetadataValue(plugin, tnt.getName()));
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();
        if (!itemStack.getType().equals(Material.TNT)) {
            return;
        }
        PersistentDataContainer data = itemStack.getItemMeta().getPersistentDataContainer();
        String tntType = data.get(new NamespacedKey(plugin, "TNTType"), PersistentDataType.STRING);
        if (tntType == null) {
            return;
        }
        Block block = event.getBlockPlaced();
        block.setMetadata("TNTType", new FixedMetadataValue(plugin, tntType));
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        List<Block> blocks = event.getBlocks();
        ListIterator<Block> blockIterator = blocks.listIterator(blocks.size());
        while (blockIterator.hasPrevious()) {
            Block block = blockIterator.previous();
            if (!block.getType().equals(Material.TNT) || !block.hasMetadata("TNTType")) continue;
            String tntType = (block.getMetadata("TNTType").get(0)).asString();
            block.removeMetadata("TNTType", plugin);
            Block nextBlock = block.getRelative(event.getDirection(), 1);
            nextBlock.setMetadata("TNTType", new FixedMetadataValue(plugin, tntType));
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) {
            return;
        }
        List<Block> blocks = event.getBlocks();
        ListIterator<Block> blockIterator = blocks.listIterator(blocks.size());
        while (blockIterator.hasPrevious()) {
            Block block = blockIterator.previous();
            if (!block.getType().equals(Material.TNT) || !block.hasMetadata("TNTType")) continue;
            String tntType = (block.getMetadata("TNTType").get(0)).asString();
            block.removeMetadata("TNTType", plugin);
            Block nextBlock = block.getRelative(event.getDirection(), 1);
            nextBlock.setMetadata("TNTType", new FixedMetadataValue(plugin, tntType));
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!block.getType().equals(Material.TNT)) {
            return;
        }
        if (!block.hasMetadata("TNTType")) {
            return;
        }
        String tntType = (block.getMetadata("TNTType").get(0)).asString();
        TNT tnt = SunTNT.getTntManager().getTNT(tntType);
        event.setDropItems(false);
        block.getLocation().getWorld().dropItemNaturally(block.getLocation(), tnt.getItemStack());
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
