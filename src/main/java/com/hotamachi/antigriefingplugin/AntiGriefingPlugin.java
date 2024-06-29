package com.hotamachi.antigriefingplugin;



import org.bukkit.Bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.*;

public class AntiGriefingPlugin extends JavaPlugin implements Listener, TabExecutor {

    public Set<UUID> trustedPlayers = new HashSet<>();


    private String fireBanReason;
    private String lavaBanReason;

    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
        loadConfiguration();


        getLogger().info("[!]チート対策プラグインを有効化しています。");
        getServer().getPluginManager().registerEvents(this, this);

        getCommand("trusttnt").setExecutor(this);
        getCommand("trusttnt").setTabCompleter(this);
        getCommand("reload").setExecutor(this);
        getCommand("reload").setTabCompleter(this);
        getCommand("toggleworld").setExecutor(this);
        getCommand("toggleworld").setTabCompleter(this);
        getCommand("allowplayer").setExecutor(this);
        getCommand("allowplayer").setTabCompleter(this);

        fireBanReason = getConfig().getString("ban-reasons.fire", "火打ち石を使用してブロックを燃やそうとしたためBANされました。");
        lavaBanReason = getConfig().getString("ban-reasons.lava", "溶岩を使用したためBANされました。");
    }

    @Override
    public void onDisable() {
        getServer().getPluginManager().registerEvents(null,null);
        getServer().getPluginManager().disablePlugin(this);
        getLogger().info("[!]チート対策プラグインを無効化しています。");
    }

    public Set<UUID> getTrustedPlayers() {
        return trustedPlayers;
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
                trustedPlayers.add(target.getUniqueId());
                sender.sendMessage(target.getName() + " はTNTを使用できるようになりました。");
            } else if (args[0].equalsIgnoreCase("remove")) {
                trustedPlayers.remove(target.getUniqueId());
                sender.sendMessage(target.getName() + " はTNTを使用できません。");
            } else {
                sender.sendMessage("使用方法: /trusttnt <add|remove> <player>");
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("reload")) {
            reloadConfig();
            loadConfiguration();
            sender.sendMessage("§a設定ファイルが再読み込みされました。");
            getLogger().info("設定ファイルが再読み込みされました。");
            return true;
        } else if (command.getName().equalsIgnoreCase("toggleworld")) {
            if (args.length != 1) {
                sender.sendMessage("§c使用方法: /toggleworld <world>");
                return false;
            }

            String worldName = args[0];
            FileConfiguration config = getConfig();

            if (!config.contains("worlds." + worldName)) {
                sender.sendMessage("§cワールド " + worldName + " は存在しません。");
                return false;
            }

            boolean currentSetting = config.getBoolean("worlds." + worldName + ".prevent-block-placement");
            config.set("worlds." + worldName + ".prevent-block-placement", !currentSetting);
            saveConfig();

            sender.sendMessage("§aワールド " + worldName + " のブロック設置防止機能を " + (!currentSetting ? "有効" : "無効") + " に切り替えました。");
            getLogger().info("ワールド " + worldName + " のブロック設置防止機能が " + (!currentSetting ? "有効" : "無効") + " に切り替えられました。");
            return true;
        } else if (command.getName().equalsIgnoreCase("allowplayer")) {
            if (args.length != 2) {
                sender.sendMessage("使用方法: /allowplayer <add|remove> <player>");
                return true;
            }
            String action = args[0];
            String playerName = args[1];
            List<String> allowedPlayers = getConfig().getStringList("allowed-players");
            if (action.equalsIgnoreCase("add")) {
                if (!allowedPlayers.contains(playerName)) {
                    allowedPlayers.add(playerName);
                    getConfig().set("allowed-players", allowedPlayers);
                    saveConfig();
                    sender.sendMessage(playerName + " を使用可能プレイヤーリストに追加しました。");
                } else {
                    sender.sendMessage(playerName + " はすでに使用可能プレイヤーリストに追加されています。");
                }
            } else if (action.equalsIgnoreCase("remove")) {
                if (allowedPlayers.contains(playerName)) {
                    allowedPlayers.remove(playerName);
                    getConfig().set("allowed-players", allowedPlayers);
                    saveConfig();
                    sender.sendMessage(playerName + " を使用可能プレイヤーリストから削除しました。");
                } else {
                    sender.sendMessage(playerName + " は使用可能プレイヤーリストに含まれていません。");
                }
            } else {
                sender.sendMessage("使用方法: /allowplayer <add|remove> <player>");
            }
            return true;
        }
        return false;
    }



    public void loadConfiguration() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);

        // 既存のワールド設定をチェック
        getServer().getWorlds().forEach(world -> {
            String worldName = world.getName();
            if (!config.contains("worlds." + worldName)) {
                config.set("worlds." + worldName + ".prevent-block-placement", true);
                config.set("worlds." + worldName + ".blocked-items", List.of("TNT", "LAVA_BUCKET"));
            }
        });

        saveConfig(); // 設定ファイルを保存
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
        } else if (command.getName().equalsIgnoreCase("allowplayer")) { // 追加
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