package ru.loper.suntnt.handler;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public class FlagHandler {

    public static StateFlag CANCEL_NEARBY_EXPLOSION;
    public static StateFlag TNT_BLOCK_EXPLOSION;

    public static void registerFlags() {
        final FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

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
        final FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        final Flag<?> cancelFlag = registry.get("cancel-nearby-explosion");
        if (cancelFlag instanceof StateFlag stateFlag) {
            CANCEL_NEARBY_EXPLOSION = stateFlag;
        }

        final Flag<?> tntFlag = registry.get("tnt-block-explosion");
        if (tntFlag instanceof StateFlag stateFlag) {
            TNT_BLOCK_EXPLOSION = stateFlag;
        }
    }
}
