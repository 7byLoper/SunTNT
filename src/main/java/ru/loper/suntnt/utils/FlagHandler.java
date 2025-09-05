package ru.loper.suntnt.utils;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class FlagHandler {
    public static StateFlag CANCEL_NEARBY_EXPLOSION;
    public static StateFlag TNT_BLOCK_EXPLOSION;

    public static void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        try {
            CANCEL_NEARBY_EXPLOSION = new StateFlag("cancel-nearby-explosion", false);
            TNT_BLOCK_EXPLOSION = new StateFlag("tnt-block-explosion", true);

            registry.register(CANCEL_NEARBY_EXPLOSION);
            registry.register(TNT_BLOCK_EXPLOSION);

            Bukkit.getLogger().info("Custom flags registered successfully");
        } catch (FlagConflictException | IllegalStateException e) {
            Bukkit.getLogger().warning("Flag conflict detected! Trying to use existing flags...");

            loadFlags();

            Bukkit.getLogger().warning("Using existing flags due to conflict");
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to register flags!", e);
        }
    }

    public static void loadFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        Flag<?> existing;
        if ((existing = registry.get("cancel-nearby-explosion")) instanceof StateFlag) {
            CANCEL_NEARBY_EXPLOSION = (StateFlag) existing;
        }
        if ((existing = registry.get("tnt-block-explosion")) instanceof StateFlag) {
            TNT_BLOCK_EXPLOSION = (StateFlag) existing;
        }
    }
}
