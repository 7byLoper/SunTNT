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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.loper.suntnt.api.model.TNTGunProjectile;
import ru.loper.suntnt.command.TNTCommand;
import ru.loper.suntnt.config.TNTConfigManager;
import ru.loper.suntnt.handler.FlagHandler;
import ru.loper.suntnt.hook.GoldSpawnerHook;
import ru.loper.suntnt.listener.*;
import ru.loper.suntnt.listener.tnt.TNTGunListener;
import ru.loper.suntnt.listener.tnt.TNTPossibilityListener;
import ru.loper.suntnt.listener.tnt.TNTSpawnListener;
import ru.loper.suntnt.manager.TNTManager;
import ru.loper.suntnt.manager.TNTSpawnManager;
import ru.loper.suntnt.runnable.TnTGunRunnable;
import ru.loper.suntnt.utils.Utils;

@Getter
public final class SunTNT extends JavaPlugin {

    @Getter
    private static SunTNT instance;

    private TNTManager tntManager;
    private TNTConfigManager configManager;
    private TNTSpawnManager tntSpawnManager;
    private GoldSpawnerHook goldSpawnerHook;

    private boolean protectionStonesStatus = true;
    private boolean holyGoldSpawnerStatus = true;
    private boolean mysteriousEggsStatus = true;

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

        new TnTGunRunnable(block, blockFace, snowball).runTaskTimer(instance, 5L, 5L);
    }

    @Override
    public void onLoad() {
        FlagHandler.registerFlags();
    }

    @Override
    public void onEnable() {
        instance = this;
        initDepends();

        configManager = new TNTConfigManager(this);
        tntManager = new TNTManager(this);
        tntSpawnManager = new TNTSpawnManager();

        registerListeners();
        registerCommands();
    }

    private void registerCommands() {
        new TNTCommand(this).registerWrappers();
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new TNTPossibilityListener(this, configManager), this);
        Bukkit.getPluginManager().registerEvents(new TNTSpawnListener(this, configManager, tntSpawnManager), this);
        Bukkit.getPluginManager().registerEvents(new TNTGunListener(this, configManager), this);
        Bukkit.getPluginManager().registerEvents(new AnvilListener(configManager), this);
        Bukkit.getPluginManager().registerEvents(new SpawnerPlaceListener(), this);
    }

    private void initDepends() {
        Plugin protectionStones = Bukkit.getPluginManager().getPlugin("ProtectionStones");
        if (protectionStones == null
                || !protectionStones.getDescription().getVersion().contains("SUN-EDITION")) {
            protectionStonesStatus = false;
            getLogger()
                    .warning(
                            "ProtectionStones (SUN) отсутствует, плагин не будет работать с регионами. Приобрести плагина можно в нашей студии t.me/bySunDev");
        }

        if (Bukkit.getPluginManager().getPlugin("SunHolyGoldSpawner") != null) {
            goldSpawnerHook = new GoldSpawnerHook();
            goldSpawnerHook.hook();
        } else {
            holyGoldSpawnerStatus = false;
        }

        if (Bukkit.getPluginManager().getPlugin("SunMysteriousEggs") == null) {
            mysteriousEggsStatus = false;
        }
    }

    @Override
    public void onDisable() {
        if (tntManager != null) {
            tntManager.unregisterRecipes();
        }
    }
}
