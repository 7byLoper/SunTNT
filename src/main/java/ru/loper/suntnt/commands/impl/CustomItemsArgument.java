package ru.loper.suntnt.commands.impl;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.colorize.StringColorize;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suntnt.config.TNTConfigManager;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suntnt.command.customitems", aliases = "customitems")
public class CustomItemsArgument implements BuildableCommand {
    private final TNTConfigManager configManager;

    @Override
    public void handle(@NotNull CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(StringColorize.parse(configManager.getCustomItemsUsageMessage()));
            return;
        }
        String operation = args[1].toLowerCase();

        switch (operation) {
            case "give" -> handleGiveCustomItem(sender, args);
            case "add" -> handleAddCustomItem(sender, args);
            case "remove" -> handleRemoveCustomItem(sender, args);
        }
    }

    private void handleAddCustomItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(StringColorize.parse(configManager.getCustomItemsPlayerOnlyMessage()));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(StringColorize.parse(configManager.getCustomItemsNameRequiredMessage()));
            return;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType().equals(Material.AIR)) {
            sender.sendMessage(StringColorize.parse(configManager.getCustomItemsHoldItemMessage()));
            return;
        }

        CustomConfig itemsConfig = configManager.getCustomConfig("customItems");
        itemsConfig.getConfig().set("items." + args[2], itemStack);
        itemsConfig.saveConfig();

        String displayName = itemStack.getItemMeta().hasDisplayName() ?
                itemStack.getItemMeta().getDisplayName() : itemStack.getType().name();

        sender.sendMessage(StringColorize.parse(configManager.getCustomItemsAddSuccessMessage()
                .replace("{name}", displayName)));
    }

    private void handleRemoveCustomItem(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(StringColorize.parse(configManager.getCustomItemsNameRequiredMessage()));
            return;
        }

        ConfigurationSection itemsSection = getItemsSection();
        if (itemsSection == null || !itemsSection.contains(args[2])) {
            sender.sendMessage(StringColorize.parse(configManager.getCustomItemsNotExistsMessage()
                    .replace("{item}", args[2])));
            return;
        }

        CustomConfig itemsConfig = configManager.getCustomConfig("customItems");
        itemsConfig.getConfig().set("items." + args[2], null);
        itemsConfig.saveConfig();

        sender.sendMessage(StringColorize.parse(configManager.getCustomItemsRemoveSuccessMessage()
                .replace("{item}", args[2])));
    }

    private void handleGiveCustomItem(CommandSender sender, String[] args) {
        ConfigurationSection itemsSection = getItemsSection();
        if (itemsSection == null) {
            sender.sendMessage(StringColorize.parse(configManager.getCustomItemsConfigErrorMessage()));
            return;
        }

        ItemStack itemStack = itemsSection.getItemStack(args[2]);
        if (itemStack == null) {
            sender.sendMessage(StringColorize.parse(configManager.getCustomItemsNotExistsMessage()
                    .replace("{item}", args[2])));
            return;
        }

        Player player = resolveTargetPlayer(sender, args);
        if (player == null) return;

        player.getInventory().addItem(itemStack.clone());

        String displayName = itemStack.getItemMeta().hasDisplayName() ?
                itemStack.getItemMeta().getDisplayName() : itemStack.getType().name();

        sender.sendMessage(StringColorize.parse(configManager.getCustomItemsGiveSuccessMessage()
                .replace("{item}", displayName)
                .replace("{player}", player.getName())));
    }

    private Player resolveTargetPlayer(CommandSender sender, String[] args) {
        if (args.length < 4) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(StringColorize.parse(configManager.getPlayerOnlyCommandMessage()));
                return null;
            }
            return (Player) sender;
        }

        Player player = Bukkit.getPlayer(args[3]);
        if (player == null) {
            sender.sendMessage(StringColorize.parse(configManager.getOfflineTargetMessage()));
            return null;
        }
        return player;
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