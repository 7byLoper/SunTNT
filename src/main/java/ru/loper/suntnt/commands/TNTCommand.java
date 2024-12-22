package ru.loper.suntnt.commands;

import java.util.Arrays;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.tnt.TNT;
import ru.loper.suntnt.utils.Colorize;

public class TNTCommand implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("SunTNT.admin")) {
            return true;
        }
        if (args.length == 0) {
            Player player = (Player) sender;
            Block block = player.getLocation().getBlock();
            player.sendMessage(block.toString());
            block.setBlockData(Material.WATER.createBlockData(), false);
        } else if (args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayer(args[1]);
            TNT tnt = SunTNT.getTntManager().getTNT(args[2]);
            ItemStack itemStack = tnt.getItemStack();
            if (args.length == 4) {
                itemStack.setAmount(Integer.parseInt(args[3]));
            }
            giveOrDrop(target, itemStack);
            Colorize.sendMessage(sender,SunTNT.getInstance().getConfig().getString("messages.give-sender").replace("{name}",tnt.getDisplayName()).replace("{amount}",String.valueOf(itemStack.getAmount())).replace("{player}", target.getDisplayName()));
            Colorize.sendMessage(target,SunTNT.getInstance().getConfig().getString("messages.give-sender").replace("{name}",tnt.getDisplayName()).replace("{amount}",String.valueOf(itemStack.getAmount())).replace("{player}", sender.getName()));
        } else if (args[0].equalsIgnoreCase("customItems")) {
            if(args.length != 3){
                Colorize.sendMessage(sender,SunTNT.getInstance().getConfig().getString("messages.error-command"));
                return true;
            }
            if(args[1].equalsIgnoreCase("add")){
                if(sender instanceof Player player){
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.getType() == Material.AIR){
                        Colorize.sendMessage(player,SunTNT.getInstance().getConfig().getString("messages.error-item"));
                        return true;
                    }
                    SunTNT.getCustomItemsConfig().getConfig().set("items."+args[2],item);
                    SunTNT.getCustomItemsConfig().saveConfig();
                    Colorize.sendMessage(player,SunTNT.getInstance().getConfig().getString("messages.add-custom-item").replace("{name}",args[2]));
                }
            } else if (args[1].equalsIgnoreCase("remove")) {
                if(sender instanceof Player player){
                    SunTNT.getCustomItemsConfig().getConfig().set("items."+args[2],null);
                    SunTNT.getCustomItemsConfig().saveConfig();
                    Colorize.sendMessage(player,SunTNT.getInstance().getConfig().getString("messages.remove-custom-item").replace("{name}",args[2]));
                }
            }
        }
        return true;
    }

    public boolean isInvFull(Player player) {
        return Arrays.stream(player.getInventory().getStorageContents()).noneMatch(Objects::isNull);
    }

    public void giveOrDrop(Player player, ItemStack itemStack) {
        if (isInvFull(player)) {
            player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            return;
        }
        player.getInventory().addItem(itemStack);
    }
}

