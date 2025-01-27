package com.hotamachi.antigriefingplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;

public class CommandHandler implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Set<UUID> trustedPlayers;

    public CommandHandler(JavaPlugin plugin, Set<UUID> trustedPlayers) {
        this.plugin = plugin;
        this.trustedPlayers = trustedPlayers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.hasPermission("antigrief.admin")) {
            sender.sendMessage(Component.text("このコマンドを実行する権限がありません。")
                    .style(style -> style.color(TextColor.color(0xFF5555))));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(Component.text("設定ファイルが再読み込みされました。")
                        .style(style -> style.color(TextColor.color(0x55FF55))));
                return true;
            case "world":
                return World(sender, args);
            case "antigrief":
                if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage(Component.text("使い方: /antigrief - プラグイン全体の有効/無効を切り替えます。")
                            .style(style -> style.color(TextColor.color(0xFFFF55))));
                    return true;
                }
                return togglePluginEnabled(sender);
            case "baninfo":
                return BanInfo(sender, args);
            case "antinuker":
                if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage(Component.text("使い方: /antinuker - Nuker対策を有効/無効に切り替えます。")
                            .style(style -> style.color(TextColor.color(0xFFFF55))));
                    return true;
                }
                return toggleNukerProtection(sender);
            default:
                sender.sendMessage(Component.text("不明なコマンドです。")
                        .style(style -> style.color(TextColor.color(0xFF5555))));
                return false;
        }
    }


    private boolean World(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("§c使用方法: /world <world>").style(style -> style.color(net.kyori.adventure.text.format.TextColor.color(0xFFFF55))));
            return false;
        }

        String worldName = args[0];
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("worlds." + worldName)) {
            sender.sendMessage(Component.text("ワールド " + worldName + " は存在しません。").style(style -> style.color(net.kyori.adventure.text.format.TextColor.color(0xFF5555))));
            return false;
        }

        boolean currentSetting = config.getBoolean("worlds." + worldName + ".prevent-block-placement");
        config.set("worlds." + worldName + ".prevent-block-placement", !currentSetting);
        plugin.saveConfig();
        sender.sendMessage(Component.text("ワールド " + worldName + " のブロック設置防止機能を " + (!currentSetting ? "有効" : "無効") + " に切り替えました。").style(style -> style.color(net.kyori.adventure.text.format.TextColor.color(0x55FF55))));
        return true;
    }

    private boolean toggleNukerProtection(CommandSender sender) {
        boolean nukerProtection = plugin.getConfig().getBoolean("nuker-protection", true);
        nukerProtection = !nukerProtection;
        plugin.getConfig().set("nuker-protection", nukerProtection);
        plugin.saveConfig();
        sender.sendMessage(Component.text("Nuker対策が " + (nukerProtection ? "有効" : "無効") + " になりました。")
                .style(style -> style.color(TextColor.color(0x55FF55))));
        return true;
    }

    private boolean togglePluginEnabled(CommandSender sender) {
        boolean pluginEnabled = plugin.getConfig().getBoolean("pluginEnabled", true);
        pluginEnabled = !pluginEnabled;
        plugin.getConfig().set("pluginEnabled", pluginEnabled);
        plugin.saveConfig();
        sender.sendMessage(Component.text("荒らし対策プラグインが " + (pluginEnabled ? "有効" : "無効") + " になりました。")
                .style(style -> style.color(TextColor.color(0x55FF55))));
        return true;
    }



    private boolean BanInfo(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("§c使用方法: /baninfo <player>").style(style -> style.color(net.kyori.adventure.text.format.TextColor.color(0xFFFF55))));
            return false;
        }
        String playerName = args[0];
        BanEntry banEntry = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(playerName);
        if (banEntry == null) {
            sender.sendMessage(Component.text("プレイヤー" + playerName + "はBANされていません。").style(style -> style.color(net.kyori.adventure.text.format.TextColor.color(0x55FF55))));
        } else {
            sender.sendMessage(Component.text("プレイヤー" + playerName + "はBANされています。理由：" + banEntry.getReason()).style(style -> style.color(TextColor.color(0xFF5555))));
            return true;
        }

        return false;
    }
}


