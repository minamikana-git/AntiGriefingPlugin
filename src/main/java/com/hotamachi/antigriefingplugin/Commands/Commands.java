package com.hotamachi.antigriefingplugin.Commands;

import com.hotamachi.antigriefingplugin.AntiGriefingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Commands implements CommandExecutor, TabCompleter {

    private final AntiGriefingPlugin plugin;

    public Commands(AntiGriefingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "reloadconfig":
                return handleReloadCommand(sender);

            case "toggleworld":
                return handleToggleWorldCommand(sender, args);

            case "trusttnt":
                return handleTrustTNTCommand(sender, args);

            default:
                return false;
        }
    }

    private boolean handleReloadCommand(CommandSender sender) {
        plugin.reloadConfig();
        plugin.loadConfiguration();
        sender.sendMessage("§a設定ファイルが再読み込みされました。");
        plugin.getLogger().info("§a設定ファイルが再読み込みされました。");
        return true;
    }

    private boolean handleToggleWorldCommand(CommandSender sender, String[] args) {
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

    private boolean handleTrustTNTCommand(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("使用方法: /trusttnt <add|remove> <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("プレイヤーが見つかりません");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        if (args[0].equalsIgnoreCase("add")) {
            plugin.getTrustedPlayers().add(targetUUID);
            sender.sendMessage(target.getName() + " はTNTを使用できるようになりました。");
        } else if (args[0].equalsIgnoreCase("remove")) {
            plugin.getTrustedPlayers().remove(targetUUID);
            sender.sendMessage(target.getName() + " はTNTを使用できません。");
        } else {
            sender.sendMessage("使用方法: /trusttnt <add|remove> <player>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "trusttnt":
                return handleTrustTNTTabComplete(args);

            case "toggleworld":
                return handleToggleWorldTabComplete(args);

            case "reloadconfig":
                return handleReloadConfigTabComplete(args);

            default:
                return null;
        }
    }

    private List<String> handleReloadConfigTabComplete(String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reloadconfig");
        }
        return null;
    }

    private List<String> handleToggleWorldTabComplete(String[] args) {
        if (args.length == 1) {
            List<String> worldNames = new ArrayList<>();
            for (String worldName : plugin.getConfig().getConfigurationSection("worlds").getKeys(false)) {
                worldNames.add(worldName);
            }
            return worldNames;
        }
        return null;
    }

    private List<String> handleTrustTNTTabComplete(String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove");
        } else if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames;
        }
        return null;
    }
}
