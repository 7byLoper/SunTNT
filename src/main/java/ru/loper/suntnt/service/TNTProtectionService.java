package ru.loper.suntnt.service;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import dev.espi.protectionstones.PSRegion;
import dev.espi.protectionstones.ProtectionStones;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityExplodeEvent;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.model.CustomTNT;
import ru.loper.suntnt.handler.FlagHandler;

@RequiredArgsConstructor
public class TNTProtectionService {

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

    public boolean isUnbreakableBlock(final Material material) {
        return PROTECTED_BLOCKS.contains(material);
    }

    public boolean handleWorldGuardFlags(final EntityExplodeEvent event) {
        final Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            final Block block = iterator.next();
            final com.sk89q.worldedit.util.Location blockLocation = BukkitAdapter.adapt(block.getLocation());
            final ApplicableRegionSet blockRegions = WorldGuard.getInstance()
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

    public boolean isManualBlockModificationProtected(final Block block) {
        if (plugin.isProtectionStonesStatus() && ProtectionStones.isProtectBlock(block)) {
            return true;
        }

        return !isWorldGuardExplosionAllowed(block);
    }

    public boolean isWorldGuardExplosionAllowed(final Block block) {
        final ApplicableRegionSet regions = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .createQuery()
                .getApplicableRegions(BukkitAdapter.adapt(block.getLocation()));

        final StateFlag.State customFlag = regions.queryState(null, FlagHandler.TNT_BLOCK_EXPLOSION);
        if (customFlag == StateFlag.State.DENY) {
            return false;
        }

        return regions.queryState(null, Flags.TNT) != StateFlag.State.DENY;
    }

    public void handleProtectionStones(final EntityExplodeEvent event, final CustomTNT customTNT) {
        final Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            final Block block = iterator.next();
            if (!ProtectionStones.isProtectBlock(block)) {
                continue;
            }

            final PSRegion region = PSRegion.fromLocation(block.getLocation());
            if (region == null) {
                continue;
            }

            if (!customTNT.isBreakPSRegion()) {
                iterator.remove();
            } else if (region.getStrength() != null && region.getStrength() > 1) {
                region.removeStrength(1);
                iterator.remove();
            }
        }
    }
}
