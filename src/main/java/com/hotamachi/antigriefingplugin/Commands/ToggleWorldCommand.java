package com.hotamachi.antigriefingplugin.Commands;

import com.hotamachi.antigriefingplugin.AntiGriefingPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ToggleWorldCommand implements CommandExecutor, TabCompleter {

    private final AntiGriefingPlugin plugin;

    public ToggleWorldCommand(AntiGriefingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§c使用方法: /toggleworld <world>");
            return false;
        }

        String worldName = args[0];
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("worlds." + worldName)) {
            sender.sendMessage("§cワールド " + worldName + " は存在しません。");
            return false;
        }

        boolean currentSetting = config.getBoolean("worlds." + worldName + ".prevent-block-placement");
        config.set("worlds." + worldName + ".prevent-block-placement", !currentSetting);
        plugin.saveConfig();

        sender.sendMessage("§aワールド " + worldName + " のブロック設置防止機能を " + (!currentSetting ? "有効" : "無効") + " に切り替えました。");
        plugin.getLogger().info("ワールド " + worldName + " のブロック設置防止機能が " + (!currentSetting ? "有効" : "無効") + " に切り替えられました。");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
