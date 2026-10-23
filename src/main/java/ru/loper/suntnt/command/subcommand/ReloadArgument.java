package ru.loper.suntnt.command.subcommand;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.suntnt.config.TNTConfigManager;
import ru.loper.suntnt.manager.TNTManager;

@RequiredArgsConstructor
@SubCommandRegister(permission = "suntnt.command.reload", aliases = "reload")
public class ReloadArgument implements BuildableCommand {

    private final TNTConfigManager configManager;
    private final TNTManager tntManager;

    @Override
    public void handle(final @NotNull CommandSender commandSender, final @NotNull String[] strings) {
        configManager.reloadAll();
        tntManager.reload();
    }

    @Override
    public List<String> tabComplete(final @NotNull CommandSender commandSender, final @NotNull String[] strings) {
        return List.of();
    }
}
