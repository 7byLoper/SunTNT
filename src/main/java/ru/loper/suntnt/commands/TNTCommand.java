package ru.loper.suntnt.commands;

import org.bukkit.permissions.Permission;
import ru.loper.suncore.api.command.AdvancedSmartCommandExecutor;
import ru.loper.suntnt.SunTNT;
import ru.loper.suntnt.commands.arguments.CustomItemsArgument;
import ru.loper.suntnt.commands.arguments.GiveArgument;
import ru.loper.suntnt.utils.PluginConfigManager;

public class TNTCommand extends AdvancedSmartCommandExecutor {

    private final PluginConfigManager configManager;

    public TNTCommand(SunTNT plugin) {
        configManager = plugin.getConfigManager();
        addSubCommand(new CustomItemsArgument(configManager), new Permission("suntnt.customitems"), "customitems");
        addSubCommand(new GiveArgument(plugin.getTntManager()), new Permission("suntnt.give"), "give");
    }

    @Override
    public String getDontPermissionMessage() {
        return configManager.getNoPermissions();
    }
}
