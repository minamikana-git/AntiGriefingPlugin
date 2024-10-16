package com.hotamachi.antigriefingplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandHandler implements CommandExecutor {
    private final JavaPlugin plugin;

    public CommandHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "設定ファイルが再読み込みされました。");
                return true;
            case "toggleworld":
                return handleToggleWorldCommand(sender, args);
            case "toggleantigrief":
                return handleToggleAntiGriefCommand(sender);
            default:
                return false;
        }
    }

    private boolean handleToggleWorldCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "§c使用方法: /toggleworld <world>");
            return false;
        }

        String worldName = args[0];
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("worlds." + worldName)) {
            sender.sendMessage(ChatColor.RED + "ワールド " + worldName + " は存在しません。");
            return false;
        }

        boolean currentSetting = config.getBoolean("worlds." + worldName + ".prevent-block-placement");
        config.set("worlds." + worldName + ".prevent-block-placement", !currentSetting);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "ワールド " + worldName + " のブロック設置防止機能を " + (!currentSetting ? "有効" : "無効") + " に切り替えました。");
        return true;
    }

    private boolean handleToggleAntiGriefCommand(CommandSender sender) {
        boolean pluginEnabled = plugin.getConfig().getBoolean("pluginEnabled", true);
        pluginEnabled = !pluginEnabled;
        plugin.getConfig().set("pluginEnabled", pluginEnabled);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "荒らし対策プラグインが " + (pluginEnabled ? "有効" : "無効") + " になりました。");
        return true;
    }
}
