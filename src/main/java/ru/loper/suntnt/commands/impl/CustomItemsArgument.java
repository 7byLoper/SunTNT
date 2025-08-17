package ru.loper.suntnt.commands.impl;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.loper.suncore.api.command.SubCommand;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.utils.Colorize;
import ru.loper.suntnt.config.TNTConfigManager;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CustomItemsArgument implements SubCommand {
    private final TNTConfigManager configManager;

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Colorize.parse("&c ▶ &fИспользование: /tnt customitems [give/add/remove] [tnt] [player] [amount]"));
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
        if (args.length < 3) {
            sender.sendMessage(Colorize.parse("&c ▶ &fНапишите название предмета"));
            return;
        }
        ConfigurationSection itemsSection = getItemsSection();

        if (!itemsSection.contains(args[2])) {
            sender.sendMessage(Colorize.parse("&c ▶ &fДанного предмета не существует"));
            return;
        }

        CustomConfig itemsConfig = configManager.getCustomConfig("customItems");
        itemsConfig.getConfig().set("items." + args[2], null);
        itemsConfig.saveConfig();
        sender.sendMessage(Colorize.parse("&d ▶ &fВы успешно удалили кастомный предмет %s из конфига".formatted(args[2])));
    }

    private void handleRemoveCustomItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Colorize.parse("&cДанная команда доступна только игрокам"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Colorize.parse("&c ▶ &fНапишите название предмета"));
            return;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType().equals(Material.AIR)) {
            sender.sendMessage(Colorize.parse("&c ▶ &fВозьмите в руку предмет, который хотите добавить"));
            return;
        }

        CustomConfig itemsConfig = configManager.getCustomConfig("customItems");
        itemsConfig.getConfig().set("items." + args[2], itemStack);
        itemsConfig.saveConfig();
        sender.sendMessage(Colorize.parse("&d ▶ &fВы успешно добавили кастомный предмет %s в конфиг".formatted(itemStack.getItemMeta().getDisplayName())));
    }

    private void handleGiveCustomItem(CommandSender sender, String[] args) {
        ConfigurationSection itemsSection = getItemsSection();
        if (itemsSection == null) {
            sender.sendMessage(Colorize.parse("&c ▶ &fОшибка получения предметов из конфига"));
            return;
        }

        ItemStack itemStack = itemsSection.getItemStack(args[2]);
        if (itemStack == null) {
            sender.sendMessage(Colorize.parse("&c ▶ &fДанного предмета не существует"));
            return;
        }

        Player player = resolveTargetPlayer(sender, args);
        if (player == null) return;

        player.getInventory().addItem(itemStack.clone());
        sender.sendMessage(Colorize.parse(String.format(
                "&a ▶ &fВыдан предмет &e%s &fигроку &e%s",
                itemStack.getItemMeta().getDisplayName(), player.getName()
        )));

    }

    private Player resolveTargetPlayer(CommandSender sender, String[] args) {
        if (args.length < 4) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Colorize.parse("&cДанная команда доступна только игрокам"));
                return null;
            }
            return (Player) sender;
        }

        Player player = Bukkit.getPlayer(args[3]);
        if (player == null) {
            sender.sendMessage(Colorize.parse("&c ▶ &fУказанный игрок не найден или не в сети"));
            return null;
        }
        return player;
    }

    private ConfigurationSection getItemsSection() {
        return configManager.getCustomConfig("customItems").getConfig().getConfigurationSection("items");
    }

    @Override
    public List<String> onTabCompleter(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(List.of("give", "remove", "add"));
        } else if (args.length == 3) {
            switch (args[1].toLowerCase()) {
                case "give", "remove" -> {
                    ConfigurationSection itemsSection = getItemsSection();
                    if (itemsSection != null) {
                        completions.addAll(itemsSection.getKeys(false));
                    }
                }
                case "add" -> completions.add("name");
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName).toList());
        } else if (args.length == 5 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(List.of("1", "8", "16", "32", "64"));
        }

        String currentArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .toList();
    }
}
