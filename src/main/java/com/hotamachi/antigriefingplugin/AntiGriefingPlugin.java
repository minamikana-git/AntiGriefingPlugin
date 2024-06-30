package com.hotamachi.antigriefingplugin;

import me.leoko.advancedban.manager.UUIDManager;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedban.utils.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.*;

public class AntiGriefingPlugin extends JavaPlugin implements Listener, TabExecutor {

    public Set<UUID> trustedPlayers = new HashSet<>();
    private UUIDManager uuidManager;
    private String fireBanReason;
    private String lavaBanReason;

    public Set<UUID> getTrustedPlayers() {
        return trustedPlayers;
    }

    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
        loadConfiguration();

        getLogger().info("[!] チート対策プラグインを有効化しています。");
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();

        fireBanReason = getConfig().getString("ban-reasons.fire", "火打ち石を使用してブロックを燃やそうとしたためBANされました。");
        lavaBanReason = getConfig().getString("ban-reasons.lava", "マグマを使用したためBANされました。");

        Plugin advancedBan = Bukkit.getPluginManager().getPlugin("AdvancedBan");
        if (advancedBan != null && advancedBan.isEnabled()) {
            uuidManager = UUIDManager.get();
        } else {
            getLogger().warning("AdvancedBan plugin not found or not enabled!");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("[!] チート対策プラグインを無効化しています。");
    }

    private void registerCommands() {
        getCommand("trusttnt").setExecutor(this);
        getCommand("trusttnt").setTabCompleter(this);
        getCommand("reload").setExecutor(this);
        getCommand("reload").setTabCompleter(this);
        getCommand("toggleworld").setExecutor(this);
        getCommand("toggleworld").setTabCompleter(this);
        getCommand("allowplayer").setExecutor(this);
        getCommand("allowplayer").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("trusttnt")) {
            return handleTrustTntCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("reload")) {
            reloadConfig();
            loadConfiguration();
            sender.sendMessage("§a設定ファイルが再読み込みされました。");
            return true;
        } else if (command.getName().equalsIgnoreCase("toggleworld")) {
            return handleToggleWorldCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("allowplayer")) {
            return handleAllowPlayerCommand(sender, args);
        }
        return false;
    }

    private boolean handleTrustTntCommand(CommandSender sender, String[] args) {
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
    }

    private boolean handleToggleWorldCommand(CommandSender sender, String[] args) {
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
        return true;
    }

    private boolean handleAllowPlayerCommand(CommandSender sender, String[] args) {
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
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item != null && item.getType() == Material.LAVA_BUCKET) {
                Location location = event.getClickedBlock().getLocation();
                String message = String.format("%sさんが座標(%d, %d, %d)で溶岩を使用しました！",
                        player.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
                Bukkit.getLogger().info(message);
                Bukkit.broadcastMessage(message);

                // AdvancedBanを使用してプレイヤーをBANする
                String reason = lavaBanReason;
                if (uuidManager != null) {
                    Punishment.create(
                            player.getName(), // Player's name
                            uuidManager.getUUID(player.getName()), // Player's UUID
                            reason, // Reason
                            "", // Operator name
                            PunishmentType.IP_BAN, // Type of punishment
                            -1L, // Duration (-1 for permanent ban)
                            null, // Calculation
                            true // Silent
                    );
                } else {
                    getLogger().warning("UUIDManager is not initialized properly.");
                }

                // プレイヤーをキックする
                player.kickPlayer(reason);
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

                // ネザーゲートのブロックに対する着火をチェック
                if (isNetherPortalFrame(block)) {
                    return;
                }

                String message = String.format("%sさんが座標(%d, %d, %d)で火打ち石を使用してブロックを燃やそうとしました！",
                        player.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
                Bukkit.getLogger().info(message);
                Bukkit.broadcastMessage(message);

                // AdvancedBanを使用してプレイヤーをBANする
                String reason = fireBanReason;
                if (uuidManager != null) {
                    Punishment.create(
                            player.getName(), // Player's name
                            uuidManager.getUUID(player.getName()), // Player's UUID
                            reason, // Reason
                            "", // Operator name
                            PunishmentType.BAN, // Type of punishment
                            -1L, // Duration (-1 for permanent ban)
                            null, // Calculation
                            true // Silent
                    );
                } else {
                    getLogger().warning("UUIDManager is not initialized properly.");
                }

                // プレイヤーをキックする
                player.kickPlayer(reason);
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
        if (command.getName().equalsIgnoreCase("trusttnt") || command.getName().equalsIgnoreCase("allowplayer")) {
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

    private boolean isNetherPortalFrame(Block block) {
        Material type = block.getType();
        if (type != Material.OBSIDIAN) {
            return false;
        }

        // ネザーゲートのフレームをチェック
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for (BlockFace face : faces) {
            Block relative = block.getRelative(face);
            if (relative.getType() == Material.AIR) {
                if (checkPortalFrame(relative)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkPortalFrame(Block block) {
        // ネザーゲートの最低限の高さと幅をチェック
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                Block relative = block.getRelative(x, y, 0);
                if (relative.getType() != Material.OBSIDIAN) {
                    return false;
                }
            }
        }
        return true;
    }
}
