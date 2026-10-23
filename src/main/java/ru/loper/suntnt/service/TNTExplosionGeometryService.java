package ru.loper.suntnt.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.entity.EntityExplodeEvent;
import ru.loper.suntnt.api.model.CustomTNT;
import ru.loper.suntnt.config.TNTConfigManager;

@RequiredArgsConstructor
public class TNTExplosionGeometryService {

    private final TNTConfigManager configManager;
    private final TNTProtectionService protectionService;

    public void applyExplosionShape(final EntityExplodeEvent event, final CustomTNT customTnt, final Location location) {
        if (customTnt.getExplosiveMode() == CustomTNT.ExplosiveMode.VANILLA) {
            return;
        }

        final List<Block> blocks = customTnt.getExplosiveMode() == CustomTNT.ExplosiveMode.BALL
                ? getNearbySphereBlocks(location, customTnt.getExplosionPower())
                : getNearbyBlocks(location, customTnt.getExplosionPower());

        final Set<Block> shapedBlocks = new HashSet<>(blocks);
        event.blockList().removeIf(block -> !shapedBlocks.contains(block));

        final Set<Block> listedBlocks = new HashSet<>(event.blockList());
        event.blockList().addAll(blocks.stream()
                .filter(block -> !block.getType().isAir())
                .filter(block -> !protectionService.isUnbreakableBlock(block.getType())
                        || configManager.getProtectionBlocks().containsKey(block.getType()))
                .filter(protectionService::isWorldGuardExplosionAllowed)
                .filter(listedBlocks::add)
                .toList());
    }

    public boolean containsWater(final Block block) {
        return block.getType() == Material.WATER
                || block.getType() == Material.BUBBLE_COLUMN
                || (block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged());
    }

    public void addBlocksBehindWater(final EntityExplodeEvent event, final Location center, final int power) {
        final World world = center.getWorld();
        if (world == null) {
            return;
        }

        final Set<Block> listed = new HashSet<>(event.blockList());
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    if (x != 0 && x != 15 && y != 0 && y != 15 && z != 0 && z != 15) {
                        continue;
                    }

                    final double dx = x / 15.0 * 2 - 1;
                    final double dy = y / 15.0 * 2 - 1;
                    final double dz = z / 15.0 * 2 - 1;
                    final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    double px = center.getX();
                    double py = center.getY();
                    double pz = center.getZ();
                    float strength = power * (0.7F + random.nextFloat() * 0.6F);
                    boolean passedWater = false;

                    while (strength > 0) {
                        final int blockY = (int) Math.floor(py);
                        if (blockY < world.getMinHeight() || blockY >= world.getMaxHeight()) {
                            break;
                        }

                        final Block block = world.getBlockAt((int) Math.floor(px), blockY, (int) Math.floor(pz));
                        if (containsWater(block)) {
                            passedWater = true;
                        }

                        if (!block.getType().isAir()
                                && block.getType() != Material.WATER
                                && block.getType() != Material.BUBBLE_COLUMN) {
                            strength -= (block.getType().getBlastResistance() + 0.3F) * 0.3F;
                            if (passedWater && strength > 0 && listed.add(block) && protectionService.isWorldGuardExplosionAllowed(block)) {
                                event.blockList().add(block);
                            }
                        }

                        px += dx / length * 0.3;
                        py += dy / length * 0.3;
                        pz += dz / length * 0.3;
                        strength -= 0.225F;
                    }
                }
            }
        }
    }

    public Set<Block> getBlastFootprint(final EntityExplodeEvent event, final Location location, final CustomTNT customTnt) {
        if (customTnt.getExplosiveMode() != CustomTNT.ExplosiveMode.VANILLA) {
            return Set.of();
        }

        final Set<Block> footprint = new HashSet<>(event.blockList());
        if (footprint.isEmpty()) {
            footprint.add(location.getBlock());
        }

        for (final Block block : new HashSet<>(footprint)) {
            footprint.add(block.getRelative(BlockFace.UP));
            footprint.add(block.getRelative(BlockFace.DOWN));
            footprint.add(block.getRelative(BlockFace.NORTH));
            footprint.add(block.getRelative(BlockFace.SOUTH));
            footprint.add(block.getRelative(BlockFace.EAST));
            footprint.add(block.getRelative(BlockFace.WEST));
        }

        return footprint;
    }

    public List<Block> getExplosionBlocks(final Location location, final int radius, final CustomTNT.ExplosiveMode mode) {
        return mode == CustomTNT.ExplosiveMode.CUBE
                ? getNearbyBlocks(location, radius)
                : getNearbySphereBlocks(location, radius);
    }

    public boolean isWithinRadius(final Location center, final Block block, final int radius, final CustomTNT.ExplosiveMode mode) {
        if (mode == CustomTNT.ExplosiveMode.CUBE) {
            return Math.abs(block.getX() - center.getBlockX()) <= radius
                    && Math.abs(block.getY() - center.getBlockY()) <= radius
                    && Math.abs(block.getZ() - center.getBlockZ()) <= radius;
        }

        final int x = block.getX() - center.getBlockX();
        final int y = block.getY() - center.getBlockY();
        final int z = block.getZ() - center.getBlockZ();
        return x * x + y * y + z * z <= radius * radius;
    }

    public List<Block> getNearbyBlocks(final Location location, final int radius) {
        final List<Block> blocks = new ArrayList<>();
        final World world = location.getWorld();
        if (world == null) {
            return blocks;
        }

        final int centerX = location.getBlockX();
        final int centerY = location.getBlockY();
        final int centerZ = location.getBlockZ();

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }

        return blocks;
    }

    public List<Block> getNearbySphereBlocks(final Location location, final int radius) {
        final List<Block> blocks = new ArrayList<>();
        final World world = location.getWorld();
        if (world == null) {
            return blocks;
        }

        final int centerX = location.getBlockX();
        final int centerY = location.getBlockY();
        final int centerZ = location.getBlockZ();
        final int radiusSquared = radius * radius;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    final int dx = x - centerX;
                    final int dy = y - centerY;
                    final int dz = z - centerZ;

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
