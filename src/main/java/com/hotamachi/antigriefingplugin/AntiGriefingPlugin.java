package com.hotamachi.antigriefingplugin;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedban.utils.PunishmentType;
import org.bukkit.Bukkit;
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

public class AntiGriefingPlugin extends JavaPlugin implements Listener, TabExecutor {

    // Variables for managing trusted players, UUID manager, and ban reasons
    private Set<UUID> trustedPlayers = new HashSet<>();
    private UUIDManager uuidManager;
    private String fireBanReason;
    private String lavaBanReason;
    private boolean pluginEnabled = true;



    // Accessor for trusted players
    public Set<UUID> getTrustedPlayers() {
        return trustedPlayers;
    }

    // Plugin enable logic
    @Override
    public void onEnable() {
        // Load configuration
        if (!(new File(getDataFolder(), "config.yml")).exists()) {
            saveDefaultConfig();
        }
        loadConfiguration();
        getLogger().info("[!] チート対策プラグインを有効化しています。");

        // Register events and commands
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();

        // Set ban reasons from config
        fireBanReason = getConfig().getString("ban-reasons.fire", "火打ち石を使用してブロックを燃やそうとしたためBANされました。");
        lavaBanReason = getConfig().getString("ban-reasons.lava", "マグマを使用したためBANされました。");

        // Check for AdvancedBan plugin
        Plugin advancedBan = Bukkit.getPluginManager().getPlugin("AdvancedBan");
        if (advancedBan != null && advancedBan.isEnabled()) {
            uuidManager = UUIDManager.get();
        } else {
            getLogger().warning("AdvancedBan プラグインがないか無効化されています。");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    // Plugin disable logic
    @Override
    public void onDisable() {
        getLogger().info("[!] チート対策プラグインを無効化しています。");
    }

    // Register commands
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

    // Command handling
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "trusttnt":
                return handleTrustTntCommand(sender, args);
            case "reload":
                reloadConfig();
                loadConfiguration();
                sender.sendMessage("§a設定ファイルが再読み込みされました。");
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

    // Handle /trusttnt command
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

        UUID targetUUID = target.getUniqueId();
        if (args[0].equalsIgnoreCase("add")) {
            trustedPlayers.add(targetUUID);
            sender.sendMessage(target.getName() + " はTNTを使用できるようになりました。");
        } else if (args[0].equalsIgnoreCase("remove")) {
            trustedPlayers.remove(targetUUID);
            sender.sendMessage(target.getName() + " はTNTを使用できません。");
        } else {
            sender.sendMessage("使用方法: /trusttnt <add|remove> <player>");
        }

        return true;
    }

    // Handle /toggleworld command
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

    // Handle /allowplayer command
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

    // Handle /toggleantigrief command
    private boolean handleToggleAntiGriefCommand(CommandSender sender) {
        pluginEnabled = !pluginEnabled;
        sender.sendMessage("荒らし対策プラグインが " + (pluginEnabled ? "有効" : "無効") + " になりました。");
        return true;
    }

    // Event handlers
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (!pluginEnabled) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.TNT && !trustedPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            event.getItem().remove();
            player.getInventory().addItem(new ItemStack(Material.SAND, 4));
            player.getInventory().addItem(new ItemStack(Material.GUNPOWDER, 5));
            player.sendMessage("TNTの所有は禁止されています。材料に変換しました。");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!pluginEnabled) return;
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        Material placedType = event.getBlock().getType();
        Set<String> blockedItems = getConfig().getStringList("blocked-items").stream().collect(Collectors.toSet());

        if (getConfig().getBoolean("worlds." + worldName + ".prevent-block-placement", false) && !blockedItems.contains(placedType.toString())) {
            event.setCancelled(true);
            player.sendMessage("§cこのワールドではブロックの設置が許可されていません！");
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!pluginEnabled) return;
        if (event.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) {
            Player player = event.getPlayer();
            String reason = fireBanReason;
            if (player != null && !trustedPlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage("§c火打石の使用は禁止されています！");
                banPlayer(player, fireBanReason);
            player.banPlayer(reason);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!pluginEnabled) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            Block block = event.getClickedBlock();
            BlockFace face = event.getBlockFace();
            if (item != null && item.getType() == Material.LAVA_BUCKET && block != null) {
                Block placedBlock = block.getRelative(face);
                if (placedBlock.getType() == Material.AIR) {
                    event.setCancelled(true);
                    String reason = lavaBanReason;
                    player.sendMessage("§cこのエリアではマグマの使用は禁止されています！");
                    banPlayer(player, lavaBanReason);
                player.banPlayer(reason);
                }
            }
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        getLogger().info("ワールド " + event.getWorld().getName() + " が読み込まれました。");
    }

    // Utility methods
    public void loadConfiguration() {
        trustedPlayers.clear();
        List<String> trustedPlayersList = getConfig().getStringList("trusted-players");
        for (String uuidString : trustedPlayersList) {
            try {
                trustedPlayers.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                getLogger().warning("無効な UUID: " + uuidString);
            }
        }
    }

    private void banPlayer(Player player, String reason) {
        if (uuidManager != null) {
            // プレイヤーのUUIDを取得
            String uuid = uuidManager.getUUID(player.getName());

            // Punishmentオブジェクトの作成と適用
            Punishment.create(player.getName(), uuid, reason, "CONSOLE", PunishmentType.BAN, -1L, (String)null, false);

            // プレイヤーがオンラインの場合は強制的に切断
            if (player.isOnline()) {
                player.kickPlayer(reason);
            }

            // コンソールに通知
            getLogger().info(player.getName() + " がバンされました: " + reason);
        }
    }



    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("trusttnt") || command.getName().equalsIgnoreCase("allowplayer")) {
            if (args.length == 1) {
                if ("add".startsWith(args[0].toLowerCase())) {
                    suggestions.add("add");
                }
                if ("remove".startsWith(args[0].toLowerCase())) {
                    suggestions.add("remove");
                }
            } else if (args.length == 2) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        suggestions.add(player.getName());
                    }
                }
            }
        }
        return suggestions;
    }
}
