package com.hotamachi.antigriefingplugin;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.leoko.advancedban.manager.UUIDManager;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedban.utils.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiGriefingPlugin extends JavaPlugin implements Listener, TabExecutor {
    private Set<UUID> trustedPlayers = new HashSet<>();
    private UUIDManager uuidManager;
    private String fireBanReason;
    private String lavaBanReason;
    private boolean pluginEnabled = true;

    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }

        loadConfiguration();
        getLogger().info("[!] チート対策プラグインを有効化しています。");
        getLogger().info("copyright 2024 hotamachisubaru all rights reserved.");
        getLogger().info("Developemented by hotamachisubaru");
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();
        fireBanReason = getConfig().getString("ban-reasons.fire", "火打ち石を使用してブロックを燃やそうとしたためBANされました。");
        lavaBanReason = getConfig().getString("ban-reasons.lava", "マグマを使用したためBANされました。");

        Plugin advancedBan = Bukkit.getPluginManager().getPlugin("AdvancedBan");
        if (advancedBan != null && advancedBan.isEnabled()) {
            uuidManager = UUIDManager.get();
        } else {
            getLogger().warning("AdvancedBan プラグインがないか無効化されています。");
            getServer().getPluginManager().disablePlugin(this);
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
        getCommand("toggleantigrief").setExecutor(this);
        getCommand("toggleantigrief").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "trusttnt":
                return handleTrustTntCommand(sender, args);
            case "reload":
                reloadConfig();
                loadConfiguration();
                sender.sendMessage(ChatColor.GREEN + "設定ファイルが再読み込みされました。");
                return true;
            case "toggleworld":
                return handleToggleWorldCommand(sender, args);
            case "allowplayer":
                return handleAllowPlayerCommand(sender, args);
            case "toggleantigrief":
                return handleToggleAntiGriefCommand(sender);
            default:
                return false;
        }
    }

    private boolean handleTrustTntCommand(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.YELLOW + "使用方法: /trusttnt <add|remove> <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "プレイヤーが見つかりません。");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        if (args[0].equalsIgnoreCase("add")) {
            trustedPlayers.add(targetUUID);
            sender.sendMessage(ChatColor.GREEN + target.getName() + " はTNTを使用できるようになりました。");
        } else if (args[0].equalsIgnoreCase("remove")) {
            trustedPlayers.remove(targetUUID);
            sender.sendMessage(ChatColor.RED + target.getName() + " はTNTを使用できません。");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "使用方法: /trusttnt <add|remove> <player>");
        }

        return true;
    }

    private boolean handleToggleWorldCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "§c使用方法: /toggleworld <world>");
            return false;
        }

        String worldName = args[0];
        FileConfiguration config = getConfig();
        if (!config.contains("worlds." + worldName)) {
            sender.sendMessage(ChatColor.RED + "ワールド " + worldName + " は存在しません。");
            return false;
        }

        boolean currentSetting = config.getBoolean("worlds." + worldName + ".prevent-block-placement");
        config.set("worlds." + worldName + ".prevent-block-placement", !currentSetting);
        saveConfig();
        sender.sendMessage(ChatColor.GREEN + "ワールド " + worldName + " のブロック設置防止機能を " + (!currentSetting ? "有効" : "無効") + " に切り替えました。");
        return true;
    }

    private boolean handleAllowPlayerCommand(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.YELLOW + "使用方法: /allowplayer <add|remove> <player>");
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
                sender.sendMessage(ChatColor.GREEN + playerName + " を使用可能プレイヤーリストに追加しました。");
            } else {
                sender.sendMessage(ChatColor.YELLOW + playerName + " はすでに使用可能プレイヤーリストに追加されています。");
            }
        } else if (action.equalsIgnoreCase("remove")) {
            if (allowedPlayers.contains(playerName)) {
                allowedPlayers.remove(playerName);
                getConfig().set("allowed-players", allowedPlayers);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + playerName + " を使用可能プレイヤーリストから削除しました。");
            } else {
                sender.sendMessage(ChatColor.RED + playerName + " は使用可能プレイヤーリストに含まれていません。");
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "使用方法: /allowplayer <add|remove> <player>");
        }

        return true;
    }

    private boolean handleToggleAntiGriefCommand(CommandSender sender) {
        pluginEnabled = !pluginEnabled;
        sender.sendMessage(ChatColor.GREEN + "荒らし対策プラグインが " + (pluginEnabled ? "有効" : "無効") + " になりました。");
        return true;
    }

    @EventHandler
    public void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
        if (!pluginEnabled) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.TNT && !trustedPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            event.getItem().remove();
            player.getInventory().addItem(new ItemStack(Material.SAND, 4));
            player.getInventory().addItem(new ItemStack(Material.GUNPOWDER, 5));
            player.sendMessage(ChatColor.YELLOW + "TNTの所有は禁止されています。材料に変換しました。");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!pluginEnabled) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (blockType == Material.LAVA_BUCKET) {
            punishPlayer(player, lavaBanReason);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!pluginEnabled) return;

        if (event.getCause() == IgniteCause.FLINT_AND_STEEL || event.getCause() == IgniteCause.LAVA) {
            Player player = event.getPlayer();
            if (player != null) {
                punishPlayer(player, fireBanReason);
                event.setCancelled(true);
            }
        }
    }

    private void punishPlayer(Player player, String reason) {
        if (uuidManager != null) {
            Punishment.create(player.getName(), uuidManager.getUUID(player.getName()), reason, "CONSOLE", PunishmentType.BAN, -1L, null, false);
        }
        player.kickPlayer(reason);
    }

    private void loadConfiguration() {
        saveDefaultConfig();
        reloadConfig();
    }
}
