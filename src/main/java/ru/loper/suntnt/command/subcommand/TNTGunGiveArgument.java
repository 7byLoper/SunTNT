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
import ru.loper.suntnt.config.TNTConfigManager;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suntnt.command.givegun", aliases = "givegun")
public class TNTGunGiveArgument implements BuildableCommand {

    private final TNTConfigManager configManager;

    @Override
    public void handle(final @NotNull CommandSender sender, final String[] args) {
        if (args.length < 2) {
            TextFormatter.send(sender, configManager.getGiveGunUsageMessage());
            return;
        }

        final Player player = resolveTargetPlayer(sender, args);
        if (player == null) {
            return;
        }

        final int amount = resolveAmount(args);
        if (amount <= 0) {
            TextFormatter.send(sender, configManager.getGiveGunInvalidAmountMessage());
            return;
        }

        final ItemBuilder itemBuilder = configManager.getTntGunBuilder();
        final ItemStack item = itemBuilder.build();
        item.setAmount(amount);

        player.getInventory().addItem(item);

        final String message = configManager
                .getGiveGunSuccessMessage()
                .replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(amount));
        TextFormatter.send(sender, message);
    }

    private Player resolveTargetPlayer(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                TextFormatter.send(sender, configManager.getPlayerOnlyCommandMessage());
                return null;
            }
            return player;
        }

        final Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            TextFormatter.send(sender, configManager.getOfflineTargetMessage());
            return null;
        }

        return player;
    }

    private int resolveAmount(final String[] args) {
        if (args.length < 3) {
            return 1;
        }

        try {
            return Math.max(1, Integer.parseInt(args[2]));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> tabComplete(final @NotNull CommandSender sender, final String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 3) {
            return Stream.of("1", "8", "16", "32", "64")
                    .filter(s -> s.startsWith(args[2]))
                    .toList();
        }

        return List.of();
    }
}
