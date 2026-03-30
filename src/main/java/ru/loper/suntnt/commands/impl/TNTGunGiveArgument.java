package ru.loper.suntnt.commands.impl;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.colorize.StringColorize;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suntnt.config.TNTConfigManager;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suntnt.command.givegun", aliases = "givegun")
public class TNTGunGiveArgument implements BuildableCommand {
    private final TNTConfigManager configManager;

    @Override
    public void handle(@NotNull CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(StringColorize.parse(configManager.getGiveGunUsageMessage()));
            return;
        }

        Player player = resolveTargetPlayer(sender, args);
        if (player == null) return;

        int amount = resolveAmount(args);
        if (amount <= 0) {
            sender.sendMessage(StringColorize.parse(configManager.getGiveGunInvalidAmountMessage()));
            return;
        }

        ItemBuilder itemBuilder = configManager.getTntGunBuilder();
        ItemStack item = itemBuilder.build();
        item.setAmount(amount);

        player.getInventory().addItem(item);

        String message = configManager.getGiveGunSuccessMessage()
                .replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(amount));
        sender.sendMessage(StringColorize.parse(message));
    }

    private Player resolveTargetPlayer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(StringColorize.parse(configManager.getPlayerOnlyCommandMessage()));
                return null;
            }
            return (Player) sender;
        }

        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            sender.sendMessage(StringColorize.parse(configManager.getOfflineTargetMessage()));
            return null;
        }
        return player;
    }

    private int resolveAmount(String[] args) {
        if (args.length < 3) return 1;

        try {
            return Math.max(1, Integer.parseInt(args[2]));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            return Stream.of("1", "8", "16", "32", "64")
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}