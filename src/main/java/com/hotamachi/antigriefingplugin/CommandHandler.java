package com.hotamachi.antigriefingplugin;

import net.kyori.adventure.text.Component;
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
                sender.sendMessage(Component.text("設定ファイルが再読み込みされました。").color(net.kyori.adventure.text.format.TextColor.color(0x55FF55)));
                return true;
            case "world":
                return World(sender, args);
            case "antigrief":
                return AntiGrief(sender);
            default:
                return false;
        }
    }

    private boolean World(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("§c使用方法: /world <world>").color(net.kyori.adventure.text.format.TextColor.color(0xFFFF55)));
            return false;
        }

        String worldName = args[0];
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("worlds." + worldName)) {
            sender.sendMessage(Component.text("ワールド " + worldName + " は存在しません。").color(net.kyori.adventure.text.format.TextColor.color(0xFF5555)));
            return false;
        }

        boolean currentSetting = config.getBoolean("worlds." + worldName + ".prevent-block-placement");
        config.set("worlds." + worldName + ".prevent-block-placement", !currentSetting);
        plugin.saveConfig();
        sender.sendMessage(Component.text("ワールド " + worldName + " のブロック設置防止機能を " + (!currentSetting ? "有効" : "無効") + " に切り替えました。").color(net.kyori.adventure.text.format.TextColor.color(0x55FF55)));
        return true;
    }

    private boolean AntiGrief(CommandSender sender) {
        boolean pluginEnabled = plugin.getConfig().getBoolean("pluginEnabled", true);
        pluginEnabled = !pluginEnabled;
        plugin.getConfig().set("pluginEnabled", pluginEnabled);
        plugin.saveConfig();
        sender.sendMessage(Component.text("荒らし対策プラグインが " + (pluginEnabled ? "有効" : "無効") + " になりました。").color(net.kyori.adventure.text.format.TextColor.color(0x55FF55)));
        return true;
    }
}
