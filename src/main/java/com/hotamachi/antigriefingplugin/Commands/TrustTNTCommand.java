package com.hotamachi.antigriefingplugin.Commands;

import com.hotamachi.antigriefingplugin.AntiGriefingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class TrustTNTCommand implements CommandExecutor, TabCompleter {

    private final AntiGriefingPlugin plugin;

    public TrustTNTCommand(AntiGriefingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("trusttnt")) {
            if (args.length != 2) {
                sender.sendMessage("使用方法: /trusttnt <add|remove> <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("プレイヤーが見つかりません");
                return true;
            }
            if (args[0].equalsIgnoreCase("add")) {
                plugin.getTrustedPlayers().add(target.getUniqueId());
                sender.sendMessage(target.getName() + " はTNTを使用できるようになりました。");
            } else if (args[0].equalsIgnoreCase("remove")) {
                plugin.getTrustedPlayers().remove(target.getUniqueId());
                sender.sendMessage(target.getName() + " はTNTを使用できません。");
            } else {
                sender.sendMessage("使用方法: /trusttnt <add|remove> <player>");
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("trusttnt")) {
            if (args.length == 1) {
                return Arrays.asList("add", "remove");
            } else if (args.length == 2) {
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                return playerNames;
            }
        }
        return null;
    }
}
