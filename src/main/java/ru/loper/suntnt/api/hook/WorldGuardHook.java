package ru.loper.suntnt.api.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Set;

@UtilityClass
public class WorldGuardHook {
    public static boolean hasRegionAtLocation(Location location) {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) return false;
        return !getRegionsAtLocation(location).isEmpty();
    }

    public static Set<ProtectedRegion> getRegionsAtLocation(Location location) {
        com.sk89q.worldedit.util.Location weLocation = BukkitAdapter.adapt(location);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet regions = query.getApplicableRegions(weLocation);
        return regions.getRegions();
    }
}