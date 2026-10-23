package ru.loper.suntnt.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import ru.loper.suncore.api.scheduler.SchedulerServices;
import ru.loper.suncore.api.sound.SoundPlayer;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.handler.FlagHandler;

@RequiredArgsConstructor
public class TNTIceService {

    private static final double SOUND_RADIUS = 15.0;

    private final SunTNT plugin;
    private final Cache<Location, BlockData> waterBlocks =
            CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.MINUTES).build();

    public void createIceSphere(final Location center, final int radius, long delay) {
        final World world = center.getWorld();
        if (world == null) {
            return;
        }

        final RegionQuery regionQuery = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        final int radiusSquared = radius * radius;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSquared) {
                        continue;
                    }

                    final Block block = world.getBlockAt(center.clone().add(x, y, z));
                    if (!isIceReplaceable(block)) {
                        continue;
                    }

                    if (regionQuery.getApplicableRegions(BukkitAdapter.adapt(block.getLocation()))
                                    .queryState(null, FlagHandler.TNT_BLOCK_EXPLOSION)
                            == StateFlag.State.DENY) {
                        continue;
                    }

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

    private boolean isIceReplaceable(final Block block) {
        final Material type = block.getType();
        return type.isAir() || type == Material.WATER || type == Material.BUBBLE_COLUMN;
    }

    private void scheduleIceRemoval(final Location location, final long delay) {
        SchedulerServices.clientScheduler().runTaskLater(plugin, () -> {
            final Block block = location.getBlock();
            if (block.getType() != Material.ICE) {
                return;
            }

            final BlockData previousBlockData = waterBlocks.getIfPresent(block.getLocation());
            if (previousBlockData != null) {
                block.setBlockData(previousBlockData, false);
                waterBlocks.invalidate(block.getLocation());
            } else {
                block.setType(Material.AIR);
            }

            location.getNearbyEntities(SOUND_RADIUS, SOUND_RADIUS, SOUND_RADIUS).forEach(entity -> {
                if (entity instanceof Player nearbyPlayer) {
                    SoundPlayer.play(nearbyPlayer, location, Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
                }
            });
        }, delay);
    }
}
