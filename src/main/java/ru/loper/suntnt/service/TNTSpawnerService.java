package ru.loper.suntnt.service;

import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.sunholygoldspawner.api.models.spawner.SpawnerData;
import ru.loper.sunmysteriouseggs.SunMysteriousEggs;
import ru.loper.sunmysteriouseggs.config.MysteriousEggConfig;
import ru.loper.sunmysteriouseggs.manager.MysteriousSpawnerManager;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.api.model.CustomTNT;
import ru.loper.suntnt.config.TNTConfigManager;

@RequiredArgsConstructor
public class TNTSpawnerService {

    private final SunTNT plugin;
    private final TNTConfigManager configManager;

    public void removeSpawnersExplosion(final EntityExplodeEvent event) {
        if (!configManager.isDisableSpawnerExplosion()) {
            return;
        }

        event.blockList().removeIf(block -> block.getType() == Material.SPAWNER);
    }

    public void handleSpawnerTnt(final EntityExplodeEvent event, final CustomTNT customTnt) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextInt(100) > customTnt.getSpawnerChance()) {
            return;
        }

        for (final Block block : event.blockList()) {
            if (block.getType() != Material.SPAWNER) {
                continue;
            }

            final ItemStack spawnerItem = getSpawner(customTnt, block, random);
            if (spawnerItem != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), spawnerItem);
            }

            block.setType(Material.AIR);
            break;
        }
    }

    public ItemStack getSpawner(final CustomTNT customTnt, final Block block, final ThreadLocalRandom random) {
        if (plugin.isHolyGoldSpawnerStatus() && plugin.getGoldSpawnerHook().isGoldSpawner(block)) {
            plugin.getGoldSpawnerHook().removeSpawnerData(block);

            if (random.nextInt(100) <= customTnt.getGoldSpawnerChance()) {
                final SpawnerData spawnerData = plugin.getGoldSpawnerHook().getGoldSpawnerData(block);
                return plugin.getGoldSpawnerHook().getSpawnerItemManager().createItem(spawnerData);
            }
        }

        if (plugin.isMysteriousEggsStatus()) {
            final MysteriousSpawnerManager spawnerManager = SunMysteriousEggs.getInstance().getSpawnerManager();
            final MysteriousEggConfig eggConfig = spawnerManager.getSpawner(block.getLocation());

            if (eggConfig != null) {
                spawnerManager.removeSpawner(block.getLocation());

                if (random.nextInt(100) <= customTnt.getMysteriousSpawnerChance()) {
                    final ItemBuilder dropBuilder = eggConfig.spawnerBuilder();
                    if (dropBuilder != null) {
                        return dropBuilder.build();
                    }
                }
            }
        }

        if (random.nextInt(100) <= customTnt.getSpawnerMobSaveChance()) {
            final CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();
            final EntityType entityType = creatureSpawner.getSpawnedType();
            return createSpawnerItemStack(entityType, customTnt);
        }

        return new ItemStack(Material.SPAWNER);
    }

    public ItemStack createSpawnerItemStack(final EntityType entityType, final CustomTNT customTNT) {
        final ItemBuilder itemBuilder = new ItemBuilder(customTNT.getSpawnerForm().build());
        itemBuilder.name(itemBuilder.name().replace("{mob}", configManager.getEntityTranslation(entityType)));

        final BlockStateMeta blockStateMeta = (BlockStateMeta) itemBuilder.meta();
        if (blockStateMeta == null) {
            return null;
        }

        final CreatureSpawner spawner = (CreatureSpawner) blockStateMeta.getBlockState();
        spawner.setSpawnedType(entityType);
        blockStateMeta.setBlockState(spawner);

        itemBuilder.meta(blockStateMeta);
        return itemBuilder.build();
    }
}
