package ru.loper.suntnt.api.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;

@UtilityClass
public class WorldGuardHook {

    private static final String WORLD_GUARD_PLUGIN = "WorldGuard";

    public static boolean hasRegionAtLocation(final Location location) {
        if (!Bukkit.getPluginManager().isPluginEnabled(WORLD_GUARD_PLUGIN)) {
            return false;
        }

        return !getRegionsAtLocation(location).isEmpty();
    }

    public static Set<ProtectedRegion> getRegionsAtLocation(final Location location) {
        final com.sk89q.worldedit.util.Location weLocation = BukkitAdapter.adapt(location);
        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionQuery query = container.createQuery();
        final ApplicableRegionSet regions = query.getApplicableRegions(weLocation);

        return regions.getRegions();
    }
}
