package ru.loper.suntnt.command.subcommand;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.colorize.TextFormatter;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suntnt.api.model.Rune;
import ru.loper.suntnt.config.TNTConfigManager;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suntnt.command.runegive", aliases = "runegive")
public class RuneGiveArgument implements BuildableCommand {
    private final TNTConfigManager configManager;

    @Override
    public void handle(@NotNull CommandSender sender, String[] args) {
        if (args.length < 3) {
            TextFormatter.send(sender, configManager.getRuneGiveUsageMessage());
            return;
        }

        String runeName = args[1];
        Rune rune = configManager.getRune(runeName);

        if (rune == null) {
            TextFormatter.send(sender, configManager.getRuneNotFoundMessage().replace("{rune}", runeName));
            return;
        }

        Player player = resolveTargetPlayer(sender, args);
        if (player == null) return;

        int amount = resolveAmount(args);
        if (amount <= 0) {
            TextFormatter.send(sender, configManager.getInvalidAmountMessage());
            return;
        }

        ItemBuilder runeBuilder = rune.getItemBuilder();
        ItemStack runeItem = runeBuilder.build();
        runeItem.setAmount(amount);

        player.getInventory().addItem(runeItem);

        String message = configManager
                .getRuneGiveSenderMessage()
                .replace("{rune}", runeBuilder.name())
                .replace("{amount}", String.valueOf(amount))
                .replace("{player}", player.getName());
        TextFormatter.send(sender, message);

        if (!sender.equals(player)) {
            String playerMessage = configManager
                    .getRuneGivePlayerMessage()
                    .replace("{rune}", runeBuilder.name())
                    .replace("{amount}", String.valueOf(amount));
            TextFormatter.send(player, playerMessage);
        }
    }

    private Player resolveTargetPlayer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            if (!(sender instanceof Player)) {
                TextFormatter.send(sender, configManager.getPlayerOnlyCommandMessage());
                return null;
            }
            return (Player) sender;
        }

        Player player = Bukkit.getPlayer(args[2]);
        if (player == null) {
            TextFormatter.send(sender, configManager.getOfflineTargetMessage());
            return null;
        }
        return player;
    }

    private int resolveAmount(String[] args) {
        if (args.length < 4) return 1;

        try {
            return Math.max(1, Integer.parseInt(args[3]));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            return configManager.getTntRunes().keySet().stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 4) {
            return Stream.of("1", "8", "16", "32", "64")
                    .filter(s -> s.startsWith(args[3]))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
