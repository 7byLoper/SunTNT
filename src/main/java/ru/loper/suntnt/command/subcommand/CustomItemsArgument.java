package ru.loper.suntnt.command.subcommand;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
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
    private final TNTConfigManager configManager;

    @Override
    public void handle(@NotNull CommandSender sender, String[] args) {
        if (args.length < 3) {
            TextFormatter.send(sender, configManager.getCustomItemsUsageMessage());
            return;
        }
        String operation = args[1].toLowerCase(Locale.ROOT);

        switch (operation) {
            case "give" -> handleGiveCustomItem(sender, args);
            case "add" -> handleAddCustomItem(sender, args);
            case "remove" -> handleRemoveCustomItem(sender, args);
            default -> TextFormatter.send(sender, configManager.getCustomItemsUsageMessage());
        }
    }

    private void handleAddCustomItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            TextFormatter.send(sender, configManager.getCustomItemsPlayerOnlyMessage());
            return;
        }

        if (args.length < 3) {
            TextFormatter.send(sender, configManager.getCustomItemsNameRequiredMessage());
            return;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType().equals(Material.AIR)) {
            TextFormatter.send(sender, configManager.getCustomItemsHoldItemMessage());
            return;
        }

        CustomConfig itemsConfig = configManager.getCustomConfig("customItems");
        itemsConfig.getConfig().set("items." + args[2], itemStack);
        itemsConfig.saveConfig();

        var meta = itemStack.getItemMeta();
        String displayName = meta != null && meta.hasDisplayName()
                ? meta.getDisplayName()
                : itemStack.getType().name();

        TextFormatter.send(
                sender, configManager.getCustomItemsAddSuccessMessage().replace("{name}", displayName));
    }

    private void handleRemoveCustomItem(CommandSender sender, String[] args) {
        if (args.length < 3) {
            TextFormatter.send(sender, configManager.getCustomItemsNameRequiredMessage());
            return;
        }

        ConfigurationSection itemsSection = getItemsSection();
        if (itemsSection == null || !itemsSection.contains(args[2])) {
            TextFormatter.send(
                    sender, configManager.getCustomItemsNotExistsMessage().replace("{item}", args[2]));
            return;
        }

        CustomConfig itemsConfig = configManager.getCustomConfig("customItems");
        itemsConfig.getConfig().set("items." + args[2], null);
        itemsConfig.saveConfig();

        TextFormatter.send(
                sender, configManager.getCustomItemsRemoveSuccessMessage().replace("{item}", args[2]));
    }

    private void handleGiveCustomItem(CommandSender sender, String[] args) {
        ConfigurationSection itemsSection = getItemsSection();
        if (itemsSection == null) {
            TextFormatter.send(sender, configManager.getCustomItemsConfigErrorMessage());
            return;
        }

        ItemStack itemStack = itemsSection.getItemStack(args[2]);
        if (itemStack == null) {
            TextFormatter.send(
                    sender, configManager.getCustomItemsNotExistsMessage().replace("{item}", args[2]));
            return;
        }

        Player player = resolveTargetPlayer(sender, args);
        if (player == null) return;

        int amount = resolveGiveAmount(args);
        if (amount <= 0) {
            TextFormatter.send(sender, configManager.getInvalidAmountMessage());
            return;
        }

        giveItem(player, itemStack, amount);

        var meta = itemStack.getItemMeta();
        String displayName = meta != null && meta.hasDisplayName()
                ? meta.getDisplayName()
                : itemStack.getType().name();

        TextFormatter.send(
                sender,
                configManager
                        .getCustomItemsGiveSuccessMessage()
                        .replace("{item}", displayName)
                        .replace("{player}", player.getName())
                        .replace("{amount}", String.valueOf(amount)));
    }

    private Player resolveTargetPlayer(CommandSender sender, String[] args) {
        if (args.length < 4) {
            if (!(sender instanceof Player)) {
                TextFormatter.send(sender, configManager.getPlayerOnlyCommandMessage());
                return null;
            }
            return (Player) sender;
        }

        Player player = Bukkit.getPlayer(args[3]);
        if (player == null) {
            TextFormatter.send(sender, configManager.getOfflineTargetMessage());
            return null;
        }
        return player;
    }

    private int resolveGiveAmount(String[] args) {
        if (args.length < 5) {
            return 1;
        }

        try {
            int amount = Integer.parseInt(args[4]);
            return amount > 0 && amount <= 2304 ? amount : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private void giveItem(Player player, ItemStack source, int amount) {
        int remaining = amount;
        int stackSize = Math.max(1, source.getMaxStackSize());

        while (remaining > 0) {
            ItemStack stack = source.clone();
            stack.setAmount(Math.min(stackSize, remaining));
            remaining -= stack.getAmount();

            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            overflow.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    private ConfigurationSection getItemsSection() {
        return configManager.getCustomConfig("customItems").getConfig().getConfigurationSection("items");
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Stream.of("give", "remove", "add")
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            String operation = args[1].toLowerCase();
            if (operation.equals("give") || operation.equals("remove")) {
                ConfigurationSection itemsSection = getItemsSection();
                if (itemsSection != null) {
                    return itemsSection.getKeys(false).stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            } else if (operation.equals("add")) {
                return Stream.of("name")
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 4 && args[1].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 5 && args[1].equalsIgnoreCase("give")) {
            return Stream.of("1", "8", "16", "32", "64")
                    .filter(s -> s.startsWith(args[4]))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
