package ru.loper.suntnt.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Colorize {
    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]){6}");
    public static void sendMessage(Player player, String message) {
        player.sendMessage(colorized(message));
    }

    public static void sendMessage(String player, String message) {
        Player p = Bukkit.getPlayerExact(player);
        if (p == null)
            return;
        p.sendMessage(colorized(message));
    }

    public static void sendMessage(CommandSender player, String message) {
        player.sendMessage(colorized(message));
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(colorized(title),
                colorized(subtitle), fadeIn, stay, fadeOut);
    }

    public static void sendTitle(String player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Player p = Bukkit.getPlayerExact(player);
        if (p == null)
            return;
        p.sendTitle(colorized(title),
                colorized(subtitle), fadeIn, stay, fadeOut);
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(colorized(message));
    }

    public static void sendActionBar(String player, String message) {
        Player p = Bukkit.getPlayerExact(player);
        if (p == null)
            return;
        p.sendActionBar(colorized(message));
    }

    public static String format(String message) {
        return colorized(message);
    }
    public static List<String> format(List<String> message) {
        message.replaceAll(Colorize::colorized);
        return message;
    }

    private static String colorized(String message) {
        if(message == null) return "";
        StringBuilder builder = new StringBuilder();
        Matcher matcher = HEX_PATTERN.matcher(message);
        while (matcher.find())
            matcher.appendReplacement(builder, net.md_5.bungee.api.ChatColor.of(matcher.group()).toString());
        matcher.appendTail(builder);
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', builder.toString());
    }
}
