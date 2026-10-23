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
import ru.loper.suntnt.listener.AnvilListener;
import ru.loper.suntnt.listener.SpawnerPlaceListener;
import ru.loper.suntnt.listener.tnt.TNTGunListener;
import ru.loper.suntnt.listener.tnt.TNTPossibilityListener;
import ru.loper.suntnt.listener.tnt.TNTSpawnListener;
import ru.loper.suntnt.manager.TNTManager;
import ru.loper.suntnt.manager.TNTSpawnManager;
import ru.loper.suntnt.runnable.TnTGunRunnable;
import ru.loper.suntnt.service.TNTExplosionGeometryService;
import ru.loper.suntnt.service.TNTExplosionService;
import ru.loper.suntnt.service.TNTIceService;
import ru.loper.suntnt.service.TNTProtectionService;
import ru.loper.suntnt.service.TNTSpawnerService;
import ru.loper.suntnt.utils.Utils;

@Getter
public final class SunTNT extends JavaPlugin {

    private static final String META_TNT_GUN_PROJECTILE = "TNTGunProjectile";
    private static final String META_TNT_GUN = "TNTGun";
    private static final int GUN_TNT_FUSE_TICKS = 100;
    private static final long GUN_RUNNABLE_PERIOD = 5L;

    @Getter
    private static SunTNT instance;

    private TNTManager tntManager;
    private TNTConfigManager configManager;
    private TNTSpawnManager tntSpawnManager;
    private TNTProtectionService protectionService;
    private TNTExplosionGeometryService geometryService;
    private TNTSpawnerService spawnerService;
    private TNTIceService iceService;
    private TNTExplosionService explosionService;
    private GoldSpawnerHook goldSpawnerHook;

    private boolean protectionStonesStatus = true;
    private boolean holyGoldSpawnerStatus = true;
    private boolean mysteriousEggsStatus = true;

    public static void handleTNTGun(final TNTGunProjectile tntGunProjectile) {
        final TNTPrimed tntPrimed = tntGunProjectile.getTntPrimed();
        if (tntPrimed == null) {
            return;
        }

        final Block block = tntGunProjectile.getBlock();
        final Directional directional = (Directional) block.getBlockData();
        final BlockFace blockFace = directional.getFacing();

        final Location spawnLocation = Utils.getDispenserLocation(block).subtract(0, 0.5, 0);
        final Snowball snowball = block.getWorld().spawn(spawnLocation, Snowball.class);

        tntPrimed.setFuseTicks(GUN_TNT_FUSE_TICKS);
        snowball.addPassenger(tntPrimed);
        snowball.setGravity(false);
        snowball.setMetadata(META_TNT_GUN_PROJECTILE, new FixedMetadataValue(instance, META_TNT_GUN));

        new TnTGunRunnable(block, blockFace, snowball).runTaskTimer(instance, GUN_RUNNABLE_PERIOD, GUN_RUNNABLE_PERIOD);
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

        protectionService = new TNTProtectionService(this);
        geometryService = new TNTExplosionGeometryService(configManager, protectionService);
        spawnerService = new TNTSpawnerService(this, configManager);
        iceService = new TNTIceService(this);
        explosionService = new TNTExplosionService(
                this, configManager, protectionService, geometryService, spawnerService, iceService);

        registerListeners();
        registerCommands();
    }

    private void registerCommands() {
        new TNTCommand(this).registerWrappers();
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new TNTPossibilityListener(this, explosionService), this);
        Bukkit.getPluginManager().registerEvents(new TNTSpawnListener(this, configManager, tntSpawnManager), this);
        Bukkit.getPluginManager().registerEvents(new TNTGunListener(this, configManager), this);
        Bukkit.getPluginManager().registerEvents(new AnvilListener(configManager), this);
        Bukkit.getPluginManager().registerEvents(new SpawnerPlaceListener(), this);
    }

    private void initDepends() {
        final Plugin protectionStones = Bukkit.getPluginManager().getPlugin("ProtectionStones");
        if (!hasCompatibleProtectionStonesApi(protectionStones)) {
            protectionStonesStatus = false;
            getLogger().warning("Совместимый ProtectionStones отсутствует, плагин не будет работать с регионами.");
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

    private boolean hasCompatibleProtectionStonesApi(final Plugin protectionStones) {
        if (protectionStones == null || !protectionStones.isEnabled()) {
            return false;
        }

        try {
            final ClassLoader loader = protectionStones.getClass().getClassLoader();
            final Class<?> region = loader.loadClass("dev.espi.protectionstones.PSRegion");
            final Class<?> api = loader.loadClass("dev.espi.protectionstones.ProtectionStones");

            region.getMethod("fromLocation", Location.class);
            region.getMethod("getStrength");
            region.getMethod("removeStrength", int.class);
            api.getMethod("isProtectBlock", Block.class);
            api.getMethod("isPSRegion", com.sk89q.worldguard.protection.regions.ProtectedRegion.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            getLogger().warning("ProtectionStones API несовместим: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void onDisable() {
        if (tntManager != null) {
            tntManager.unregisterRecipes();
        }
    }
}
