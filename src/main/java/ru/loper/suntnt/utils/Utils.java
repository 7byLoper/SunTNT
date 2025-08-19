package ru.loper.suntnt.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.ItemStack;
import ru.loper.suntnt.SunTNT;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@UtilityClass
public class Utils {
    public static List<File> getFiles(File folder) {
        ArrayList<File> files = new ArrayList<>();
        if (folder.listFiles() == null) return files;
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isDirectory()) {
                files.addAll(getFiles(file));
                continue;
            }
            files.add(file);
        }
        return files;
    }

    public static ItemStack getCustomItem(String path) {
        ItemStack item = SunTNT.getInstance().getConfigManager().getCustomConfig("customitems").getConfig().getItemStack("items." + path);
        if (item != null) {
            return item.clone();
        }
        return null;
    }

    public static Location getDispenserLocation(Block dispenser) {
        Directional directional = (Directional) dispenser.getBlockData();
        BlockFace face = directional.getFacing();
        return dispenser.getLocation().add(0.5 + face.getModX() * 0.7,
                0.5 + face.getModY() * 0.7,
                0.5 + face.getModZ() * 0.7);
    }
}
