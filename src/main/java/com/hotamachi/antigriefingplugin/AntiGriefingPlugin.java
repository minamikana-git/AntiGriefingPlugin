package com.hotamachi.antigriefingplugin;


import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.block.BlockFace;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class AntiGriefingPlugin extends JavaPlugin implements Listener, TabExecutor {

    public Set<UUID> trustedPlayers = new HashSet<>();

    public Set<UUID> getTrustedPlayers() {
        return trustedPlayers;
    }

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
        getCommand("allowplayer").setExecutor(this); // 追加
        getCommand("allowplayer").setTabCompleter(this); // 追加

        fireBanReason = getConfig().getString("ban-reasons.fire", "火打ち石を使用してブロックを燃やそうとしたためBANされました。");
        lavaBanReason = getConfig().getString("ban-reasons.lava", "マグマを使用したためBANされました。");
    }


    @Override
    public void onDisable() {
        getLogger().info("[!]チート対策プラグインを無効化しています。");
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
        } else if (command.getName().equalsIgnoreCase("allowplayer")) { // 新しいコマンド
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

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.TNT && !trustedPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            event.getItem().remove(); // 拾われたTNTアイテムを削除
            player.getInventory().addItem(new ItemStack(Material.SAND, 4)); // TNTの材料（例：砂4個）
            player.getInventory().addItem(new ItemStack(Material.GUNPOWDER, 5)); // TNTの材料（例：火薬5個）
            player.sendMessage("TNTの所有は禁止されています。材料に変換しました。");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material placedType = event.getBlockPlaced().getType();
        String worldName = player.getWorld().getName();
        List<String> blockedItems = getConfig().getStringList("worlds." + worldName + ".blocked-items");

        // プレイヤーが信頼されている場合、TNTの設置を許可
        if (trustedPlayers.contains(player.getUniqueId()) && placedType == Material.TNT) {
            return;
        }

        // ネザーゲート関連のブロックを除外
        if (placedType == Material.OBSIDIAN || placedType == Material.NETHER_PORTAL) {
            return;
        }

        if (getConfig().getBoolean("worlds." + worldName + ".prevent-block-placement")) {
            if (blockedItems.contains(placedType.name())) {
                event.setCancelled(true);
                player.sendMessage("§c" + placedType.name() + " の設置は許可されていません！");
                if (getConfig().getBoolean("settings.enable-logging")) {
                    getLogger().info(player.getName() + "が" + worldName + "で" + placedType.name() + "を設置しようとしました。");
                }
            }
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        String worldName = event.getWorld().getName();
        FileConfiguration config = getConfig();

        // 新規ワールドの設定を追加
        if (!config.contains("worlds." + worldName)) {
            config.set("worlds." + worldName + ".prevent-block-placement", true);
            config.set("worlds." + worldName + ".blocked-items", List.of("TNT", "LAVA_BUCKET"));
            saveConfig(); // 設定ファイルを保存
            getLogger().info("新しいワールド " + worldName + " のデフォルト設定を追加しました。");
        }
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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item != null && item.getType() == Material.LAVA_BUCKET) {
                Location location = event.getClickedBlock().getLocation();
                String message = String.format("%sさんが座標(%d, %d, %d)で溶岩を使用しました！",
                        player.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
                getLogger().info(message);
                Bukkit.broadcastMessage(message);

                // 現在の日付を取得
                Date banDate = new Date();

                // プレイヤーをBANする
                Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), "溶岩を使用したためBANされました。", banDate, null);
                player.banPlayerIP("溶岩を使用したためBANされました。");


            }
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) {
            Player player = event.getPlayer();
            if (player != null) {
                Block block = event.getBlock();
                Location location = block.getLocation();

                // ネザーゲートの中央で着火が行われたかをチェック
                if (isNetherPortalCenter(block)) {
                    return;
                }

                String message = String.format("%sさんが座標(%d, %d, %d)で火打ち石を使用してブロックを燃やそうとしました！",
                        player.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
                getLogger().info(message);
                Bukkit.broadcastMessage(message);

                // 現在の日付を取得
                Date banDate = new Date();

                // プレイヤーをBANする
                Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), "火打ち石を使用してブロックを燃やそうとしたためBANされました。", banDate, null);
                player.banPlayer("火打ち石を使用してブロックを燃やそうとしたためBANされました。");


            }
        }
    }

    private boolean isNetherPortalCenter(Block block) {
        // ネザーゲートの枠の中かどうかをチェックする
        if (block.getType() != Material.AIR) {
            return false;
        }

        BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
        for (BlockFace face : faces) {
            Block relative = block.getRelative(face);
            if (relative.getType() == Material.OBSIDIAN) {
                return true;
            }
        }
        return false;
    }

}