package ru.loper.suntnt.commands.impl;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suntnt.config.TNTConfigManager;
import ru.loper.suntnt.manager.TNTManager;

import java.util.List;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suntnt.command.reload", aliases = "reload")
public class ReloadArgument implements BuildableCommand {
    private final TNTConfigManager configManager;
    private final TNTManager tntManager;

    @Override
    public void handle(@NotNull CommandSender commandSender, @NotNull String[] strings) {
        configManager.reloadAll();
        tntManager.reload();
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, @NotNull String[] strings) {
        return List.of();
    }
}
