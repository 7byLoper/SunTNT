package ru.loper.suntnt.hook;

import java.util.UUID;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.persistence.PersistentDataType;
import ru.loper.sunholygoldspawner.SunHolyGoldSpawner;
import ru.loper.sunholygoldspawner.api.models.spawner.SpawnerData;
import ru.loper.sunholygoldspawner.config.SpawnerConfigManager;
import ru.loper.sunholygoldspawner.manager.DataManager;
import ru.loper.sunholygoldspawner.manager.SpawnerItemManager;

@Getter
public class GoldSpawnerHook {
    private SunHolyGoldSpawner holyGoldSpawner;
    private SpawnerConfigManager configManager;
    private DataManager dataManager;
    private SpawnerItemManager spawnerItemManager;

    public void hook() {
        holyGoldSpawner = SunHolyGoldSpawner.getPlugin(SunHolyGoldSpawner.class);
        configManager = holyGoldSpawner.getConfigManager();
        dataManager = holyGoldSpawner.getDataManager();
        spawnerItemManager = holyGoldSpawner.getItemManager();
    }

    public SpawnerData getGoldSpawnerData(Block block) {
        if (!(block.getState() instanceof CreatureSpawner spawner)) {
            return null;
        }

        String uuidStr =
                spawner.getPersistentDataContainer().get(configManager.getSpawnerItemKey(), PersistentDataType.STRING);
        if (uuidStr == null) {
            return null;
        }

        UUID spawnerUuid = UUID.fromString(uuidStr);
        return dataManager.get(spawnerUuid).orElse(null);
    }

    public void removeSpawnerData(Block block) {
        if (block == null || block.getType() != Material.SPAWNER) {
            return;
        }

        if (!(block.getState() instanceof CreatureSpawner spawner)) {
            return;
        }

        spawner.getPersistentDataContainer().remove(configManager.getSpawnerItemKey());
    }

    public boolean isGoldSpawner(Block block) {
        if (block == null || block.getType() != Material.SPAWNER) {
            return false;
        }

        if (!(block.getState() instanceof CreatureSpawner spawner)) {
            return false;
        }

        return spawner.getPersistentDataContainer().has(configManager.getSpawnerItemKey(), PersistentDataType.STRING);
    }
}
