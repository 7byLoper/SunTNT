package ru.loper.suntnt.command;

import ru.loper.suncore.api.command.executor.BaseCommandExecutor;
import ru.loper.suncore.api.command.register.CommandRegister;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.command.subcommand.*;

@CommandRegister(name = "suntnt", permission = "suntnt.command.use")
public class TNTCommand extends BaseCommandExecutor {

    private final SunTNT plugin;

    public TNTCommand(SunTNT plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public String getNoPermissionMessage() {
        return plugin.getConfigManager().getNoPermissionsMessage();
    }

    @Override
    public void registerWrappers() {
        addSubCommand(new CustomItemsArgument(plugin.getConfigManager()));
        addSubCommand(new GiveArgument(plugin.getTntManager(), plugin.getConfigManager()));
        addSubCommand(new TNTGunGiveArgument(plugin.getConfigManager()));
        addSubCommand(new RuneGiveArgument(plugin.getConfigManager()));
        addSubCommand(new ReloadArgument(plugin.getConfigManager(), plugin.getTntManager()));
    }
}
