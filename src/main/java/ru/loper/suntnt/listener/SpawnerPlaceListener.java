package ru.loper.suntnt.listener;

import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public class SpawnerPlaceListener implements Listener {

    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.SPAWNER) {
            return;
        }

        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) {
            return;
        }

        if (!(meta.getBlockState() instanceof CreatureSpawner itemSpawnerState)) {
            return;
        }

        if (event.getBlockPlaced().getState() instanceof CreatureSpawner worldSpawner) {
            worldSpawner.setSpawnedType(itemSpawnerState.getSpawnedType());
            worldSpawner.update();
        }
    }
}
