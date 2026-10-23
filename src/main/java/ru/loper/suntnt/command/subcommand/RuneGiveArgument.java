package ru.loper.suntnt.command.subcommand;

import java.util.List;
import java.util.Locale;
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
    public void handle(final @NotNull CommandSender sender, final String[] args) {
        if (args.length < 2) {
            TextFormatter.send(sender, configManager.getRuneGiveUsageMessage());
            return;
        }

        final String runeName = args[1];
        final Rune rune = configManager.getRune(runeName);
        if (rune == null) {
            TextFormatter.send(sender, configManager.getRuneNotFoundMessage().replace("{rune}", runeName));
            return;
        }

        final Player player = resolveTargetPlayer(sender, args);
        if (player == null) {
            return;
        }

        final int amount = resolveAmount(args);
        if (amount <= 0) {
            TextFormatter.send(sender, configManager.getInvalidAmountMessage());
            return;
        }

        final ItemBuilder runeBuilder = rune.getItemBuilder();
        final ItemStack runeItem = runeBuilder.build();
        runeItem.setAmount(amount);

        player.getInventory().addItem(runeItem);

        final String message = configManager
                .getRuneGiveSenderMessage()
                .replace("{rune}", runeBuilder.name())
                .replace("{amount}", String.valueOf(amount))
                .replace("{player}", player.getName());
        TextFormatter.send(sender, message);

        if (!sender.equals(player)) {
            final String playerMessage = configManager
                    .getRuneGivePlayerMessage()
                    .replace("{rune}", runeBuilder.name())
                    .replace("{amount}", String.valueOf(amount));
            TextFormatter.send(player, playerMessage);
        }
    }

    private Player resolveTargetPlayer(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            if (!(sender instanceof Player player)) {
                TextFormatter.send(sender, configManager.getPlayerOnlyCommandMessage());
                return null;
            }
            return player;
        }

        final Player player = Bukkit.getPlayer(args[2]);
        if (player == null) {
            TextFormatter.send(sender, configManager.getOfflineTargetMessage());
            return null;
        }

        return player;
    }

    private int resolveAmount(final String[] args) {
        if (args.length < 4) {
            return 1;
        }

        try {
            return Math.max(1, Integer.parseInt(args[3]));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> tabComplete(final @NotNull CommandSender sender, final String[] args) {
        if (args.length == 2) {
            return configManager.getTntRunes().keySet().stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 4) {
            return Stream.of("1", "8", "16", "32", "64")
                    .filter(s -> s.startsWith(args[3]))
                    .toList();
        }

        return List.of();
    }
}
