package com.hotamachi.antigriefingplugin.Commands;

import com.hotamachi.antigriefingplugin.AntiGriefingPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final AntiGriefingPlugin plugin;

    public ReloadCommand(AntiGriefingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.reloadConfig();
        plugin.loadConfiguration();
        sender.sendMessage("§a設定ファイルが再読み込みされました。");
        plugin.getLogger().info("§a設定ファイルが再読み込みされました。");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
