package ru.loper.suntnt.command.subcommand;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.colorize.TextFormatter;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suntnt.config.TNTConfigManager;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suntnt.command.customitems", aliases = "customitems")
public class CustomItemsArgument implements BuildableCommand {

    private static final int MAX_GIVE_AMOUNT = 2304;

    private final TNTConfigManager configManager;

    @Override
    public void handle(final @NotNull CommandSender sender, final String[] args) {
        if (args.length < 2) {
            TextFormatter.send(sender, configManager.getCustomItemsUsageMessage());
            return;
        }

        final String operation = args[1].toLowerCase(Locale.ROOT);
        switch (operation) {
            case "give" -> handleGiveCustomItem(sender, args);
            case "add" -> handleAddCustomItem(sender, args);
            case "remove" -> handleRemoveCustomItem(sender, args);
            default -> TextFormatter.send(sender, configManager.getCustomItemsUsageMessage());
        }
    }

    private void handleAddCustomItem(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            TextFormatter.send(sender, configManager.getCustomItemsPlayerOnlyMessage());
            return;
        }

        if (args.length < 3) {
            TextFormatter.send(sender, configManager.getCustomItemsNameRequiredMessage());
            return;
        }

        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
            TextFormatter.send(sender, configManager.getCustomItemsHoldItemMessage());
            return;
        }

        final CustomConfig itemsConfig = configManager.getCustomConfig("customItems");
        itemsConfig.getConfig().set("items." + args[2], itemStack);
        itemsConfig.saveConfig();

        final var meta = itemStack.getItemMeta();
        final String displayName = meta != null && meta.hasDisplayName() && meta.displayName() != null
                ? LegacyComponentSerializer.legacySection().serialize(meta.displayName())
                : itemStack.getType().name();

        TextFormatter.send(sender, configManager.getCustomItemsAddSuccessMessage().replace("{name}", displayName));
    }

    private void handleRemoveCustomItem(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            TextFormatter.send(sender, configManager.getCustomItemsNameRequiredMessage());
            return;
        }

        final ConfigurationSection itemsSection = getItemsSection();
        if (itemsSection == null || !itemsSection.contains(args[2])) {
            TextFormatter.send(sender, configManager.getCustomItemsNotExistsMessage().replace("{item}", args[2]));
            return;
        }

        final CustomConfig itemsConfig = configManager.getCustomConfig("customItems");
        itemsConfig.getConfig().set("items." + args[2], null);
        itemsConfig.saveConfig();

        TextFormatter.send(sender, configManager.getCustomItemsRemoveSuccessMessage().replace("{item}", args[2]));
    }

    private void handleGiveCustomItem(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            TextFormatter.send(sender, configManager.getCustomItemsUsageMessage());
            return;
        }

        final ConfigurationSection itemsSection = getItemsSection();
        if (itemsSection == null) {
            TextFormatter.send(sender, configManager.getCustomItemsConfigErrorMessage());
            return;
        }

        final ItemStack itemStack = itemsSection.getItemStack(args[2]);
        if (itemStack == null) {
            TextFormatter.send(sender, configManager.getCustomItemsNotExistsMessage().replace("{item}", args[2]));
            return;
        }

        final Player player = resolveTargetPlayer(sender, args);
        if (player == null) {
            return;
        }

        final int amount = resolveGiveAmount(args);
        if (amount <= 0) {
            TextFormatter.send(sender, configManager.getInvalidAmountMessage());
            return;
        }

        giveItem(player, itemStack, amount);

        final var meta = itemStack.getItemMeta();
        final String displayName = meta != null && meta.hasDisplayName() && meta.displayName() != null
                ? LegacyComponentSerializer.legacySection().serialize(meta.displayName())
                : itemStack.getType().name();

        TextFormatter.send(sender, configManager.getCustomItemsGiveSuccessMessage()
                .replace("{item}", displayName)
                .replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(amount)));
    }

    private Player resolveTargetPlayer(final CommandSender sender, final String[] args) {
        if (args.length < 4) {
            if (!(sender instanceof Player player)) {
                TextFormatter.send(sender, configManager.getPlayerOnlyCommandMessage());
                return null;
            }
            return player;
        }

        final Player player = Bukkit.getPlayer(args[3]);
        if (player == null) {
            TextFormatter.send(sender, configManager.getOfflineTargetMessage());
            return null;
        }

        return player;
    }

    private int resolveGiveAmount(final String[] args) {
        if (args.length < 5) {
            return 1;
        }

        try {
            final int amount = Integer.parseInt(args[4]);
            return amount > 0 && amount <= MAX_GIVE_AMOUNT ? amount : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private void giveItem(final Player player, final ItemStack source, final int amount) {
        int remaining = amount;
        final int stackSize = Math.max(1, source.getMaxStackSize());

        while (remaining > 0) {
            final ItemStack stack = source.clone();
            stack.setAmount(Math.min(stackSize, remaining));
            remaining -= stack.getAmount();

            final Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            overflow.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    private ConfigurationSection getItemsSection() {
        return configManager.getCustomConfig("customItems").getConfig().getConfigurationSection("items");
    }

    @Override
    public List<String> tabComplete(final @NotNull CommandSender sender, final String[] args) {
        if (args.length == 2) {
            return Stream.of("give", "remove", "add")
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 3) {
            final String operation = args[1].toLowerCase(Locale.ROOT);
            if (operation.equals("give") || operation.equals("remove")) {
                final ConfigurationSection itemsSection = getItemsSection();
                if (itemsSection != null) {
                    return itemsSection.getKeys(false).stream()
                            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                            .toList();
                }
            } else if (operation.equals("add")) {
                return Stream.of("name")
                        .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                        .toList();
            }
        }

        if (args.length == 4 && args[1].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[3].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 5 && args[1].equalsIgnoreCase("give")) {
            return Stream.of("1", "8", "16", "32", "64")
                    .filter(s -> s.startsWith(args[4]))
                    .toList();
        }

        return List.of();
    }
}
