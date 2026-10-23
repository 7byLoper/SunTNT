package ru.loper.suntnt.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.ItemStack;
import ru.loper.suntnt.SunTNT;

@UtilityClass
public class Utils {

    private static final double DISPENSER_OFFSET = 0.7;
    private static final double CENTER_OFFSET = 0.5;

    public static List<File> getFiles(final File folder) {
        final List<File> files = new ArrayList<>();
        final File[] directoryFiles = folder.listFiles();
        if (directoryFiles == null) {
            return files;
        }

        for (final File file : directoryFiles) {
            if (file.isDirectory()) {
                files.addAll(getFiles(file));
                continue;
            }

            files.add(file);
        }

        return files;
    }

    public static ItemStack getCustomItem(final String path) {
        final ItemStack item = SunTNT.getInstance()
                .getConfigManager()
                .getCustomConfig("customItems")
                .getConfig()
                .getItemStack("items." + path);

        if (item != null) {
            return item.clone();
        }

        return null;
    }

    public static Location getDispenserLocation(final Block dispenser) {
        final Directional directional = (Directional) dispenser.getBlockData();
        final BlockFace face = directional.getFacing();

        return dispenser.getLocation().add(
                CENTER_OFFSET + face.getModX() * DISPENSER_OFFSET,
                CENTER_OFFSET + face.getModY() * DISPENSER_OFFSET,
                CENTER_OFFSET + face.getModZ() * DISPENSER_OFFSET);
    }
}
