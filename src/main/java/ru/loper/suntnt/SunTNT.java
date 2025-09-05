package ru.loper.suntnt;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.loper.suntnt.api.modules.TNTGunProjectile;
import ru.loper.suntnt.commands.TNTCommand;
import ru.loper.suntnt.config.TNTConfigManager;
import ru.loper.suntnt.listeners.TNTGunListener;
import ru.loper.suntnt.listeners.TNTPossibilityListener;
import ru.loper.suntnt.listeners.TNTSpawnListener;
import ru.loper.suntnt.manager.TNTManager;
import ru.loper.suntnt.utils.FlagHandler;
import ru.loper.suntnt.utils.Utils;

import java.util.Optional;

@Getter
public final class SunTNT extends JavaPlugin {

    @Getter
    private static SunTNT instance;

    private TNTManager tntManager;
    private TNTConfigManager configManager;

    private boolean protectionStonesStatus = true;
    private boolean holyLiteUtilsStatus = true;

    @Override
    public void onLoad() {
        FlagHandler.registerFlags();
    }

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("SunProtectionStones") == null) {
            protectionStonesStatus = false;
            getLogger().warning("SunProtectionStones отсутствует, некоторые функции плагина отключены. Приобрести плагина можно в нашей студии t.me/bySunDev");
        }

        if (Bukkit.getPluginManager().getPlugin("HolyLiteUtils") == null) {
            holyLiteUtilsStatus = false;
        }

        configManager = new TNTConfigManager(this);
        tntManager = new TNTManager(this);

        Bukkit.getPluginManager().registerEvents(new TNTPossibilityListener(this, configManager), this);
        Bukkit.getPluginManager().registerEvents(new TNTSpawnListener(this, configManager), this);
        Bukkit.getPluginManager().registerEvents(new TNTGunListener(this, configManager), this);

        Optional.ofNullable(getCommand("suntnt"))
                .orElseThrow(() -> new IllegalStateException("Command 'suntnt' not found!"))
                .setExecutor(new TNTCommand(this));
    }

    public static void handleTNTGun(TNTGunProjectile tntGunProjectile) {
        TNTPrimed tntPrimed = tntGunProjectile.getTntPrimed();
        if (tntPrimed == null) return;

        Block block = tntGunProjectile.getBlock();
        Directional directional = (Directional) block.getBlockData();
        BlockFace blockFace = directional.getFacing();

        Location spawnLocation = Utils.getDispenserLocation(block).subtract(0, 0.5, 0);
        Snowball snowball = block.getWorld().spawn(spawnLocation, Snowball.class);

        snowball.addPassenger(tntGunProjectile.getTntPrimed());
        tntPrimed.setFuseTicks(100);
        snowball.addPassenger(tntPrimed);
        snowball.setGravity(false);
        snowball.setMetadata("TNTGunProjectile", new FixedMetadataValue(instance, "TNTGun"));

        new BukkitRunnable() {
            private final Vector velocity = block.getRelative(blockFace).getLocation().toVector().subtract(block.getLocation().toVector());

            public void run() {
                if (snowball.isDead() || !snowball.isValid()) {
                    cancel();
                    return;
                }

                if (snowball.getPassengers().isEmpty()) {
                    snowball.remove();
                    cancel();
                    return;
                }

                snowball.setVelocity(velocity);
            }
        }.runTaskTimerAsynchronously(instance, 5L, 5L);
    }
}
