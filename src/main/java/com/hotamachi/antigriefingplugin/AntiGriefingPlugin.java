package com.hotamachi.antigriefingplugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiGriefingPlugin extends JavaPlugin implements Listener, TabExecutor {
    private Set<UUID> trustedPlayers = new HashSet();
    private UUIDManager uuidManager;
    private String fireBanReason;
    private String lavaBanReason;
    private boolean pluginEnabled = true;

    public AntiGriefingPlugin() {
    }

    public Set<UUID> getTrustedPlayers() {
        return this.trustedPlayers;
    }

    public void onEnable() {
        if (!(new File(this.getDataFolder(), "config.yml")).exists()) {
            this.saveDefaultConfig();
        }

        this.loadConfiguration();
        this.getLogger().info("[!] チート対策プラグインを有効化しています。");
        this.getServer().getPluginManager().registerEvents(this, this);
        this.registerCommands();
        this.fireBanReason = this.getConfig().getString("ban-reasons.fire", "火打ち石を使用してブロックを燃やそうとしたためBANされました。");
        this.lavaBanReason = this.getConfig().getString("ban-reasons.lava", "マグマを使用したためBANされました。");
        Plugin advancedBan = Bukkit.getPluginManager().getPlugin("AdvancedBan");
        if (advancedBan != null && advancedBan.isEnabled()) {
            this.uuidManager = UUIDManager.get();
        } else {
            this.getLogger().warning("AdvancedBan プラグインがないか無効化されています。");
            this.getServer().getPluginManager().disablePlugin(this);
        }

    }

    public void onDisable() {
        this.getLogger().info("[!] チート対策プラグインを無効化しています。");
    }

    private void registerCommands() {
        this.getCommand("trusttnt").setExecutor(this);
        this.getCommand("trusttnt").setTabCompleter(this);
        this.getCommand("reload").setExecutor(this);
        this.getCommand("reload").setTabCompleter(this);
        this.getCommand("toggleworld").setExecutor(this);
        this.getCommand("toggleworld").setTabCompleter(this);
        this.getCommand("allowplayer").setExecutor(this);
        this.getCommand("allowplayer").setTabCompleter(this);
        this.getCommand("toggleantigrief").setExecutor(this);
        this.getCommand("toggleantigrief").setTabCompleter(this);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "trusttnt":
                return this.handleTrustTntCommand(sender, args);
            case "reload":
                this.reloadConfig();
                this.loadConfiguration();
                sender.sendMessage("§a設定ファイルが再読み込みされました。");
                return true;
            case "toggleworld":
                return this.handleToggleWorldCommand(sender, args);
            case "allowplayer":
                return this.handleAllowPlayerCommand(sender, args);
            case "toggleantigrief":
                return this.handleToggleAntiGriefCommand(sender);
            default:
                return false;
        }
    }

    private boolean handleTrustTntCommand(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("使用方法: /trusttnt <add|remove> <player>");
            return true;
        } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("プレイヤーが見つかりません");
                return true;
            } else {
                UUID targetUUID = target.getUniqueId();
                if (args[0].equalsIgnoreCase("add")) {
                    this.trustedPlayers.add(targetUUID);
                    sender.sendMessage(target.getName() + " はTNTを使用できるようになりました。");
                } else if (args[0].equalsIgnoreCase("remove")) {
                    this.trustedPlayers.remove(targetUUID);
                    sender.sendMessage(target.getName() + " はTNTを使用できません。");
                } else {
                    sender.sendMessage("使用方法: /trusttnt <add|remove> <player>");
                }

                return true;
            }
        }
    }

    private boolean handleToggleWorldCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§c使用方法: /toggleworld <world>");
            return false;
        } else {
            String worldName = args[0];
            FileConfiguration config = this.getConfig();
            if (!config.contains("worlds." + worldName)) {
                sender.sendMessage("§cワールド " + worldName + " は存在しません。");
                return false;
            } else {
                boolean currentSetting = config.getBoolean("worlds." + worldName + ".prevent-block-placement");
                config.set("worlds." + worldName + ".prevent-block-placement", !currentSetting);
                this.saveConfig();
                sender.sendMessage("§aワールド " + worldName + " のブロック設置防止機能を " + (!currentSetting ? "有効" : "無効") + " に切り替えました。");
                return true;
            }
        }
    }

    private boolean handleAllowPlayerCommand(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("使用方法: /allowplayer <add|remove> <player>");
            return true;
        } else {
            String action = args[0];
            String playerName = args[1];
            List<String> allowedPlayers = this.getConfig().getStringList("allowed-players");
            if (action.equalsIgnoreCase("add")) {
                if (!allowedPlayers.contains(playerName)) {
                    allowedPlayers.add(playerName);
                    this.getConfig().set("allowed-players", allowedPlayers);
                    this.saveConfig();
                    sender.sendMessage(playerName + " を使用可能プレイヤーリストに追加しました。");
                } else {
                    sender.sendMessage(playerName + " はすでに使用可能プレイヤーリストに追加されています。");
                }
            } else if (action.equalsIgnoreCase("remove")) {
                if (allowedPlayers.contains(playerName)) {
                    allowedPlayers.remove(playerName);
                    this.getConfig().set("allowed-players", allowedPlayers);
                    this.saveConfig();
                    sender.sendMessage(playerName + " を使用可能プレイヤーリストから削除しました。");
                } else {
                    sender.sendMessage(playerName + " は使用可能プレイヤーリストに含まれていません。");
                }
            } else {
                sender.sendMessage("使用方法: /allowplayer <add|remove> <player>");
            }

            return true;
        }
    }

    private boolean handleToggleAntiGriefCommand(CommandSender sender) {
        this.pluginEnabled = !this.pluginEnabled;
        sender.sendMessage("荒らし対策プラグインが " + (this.pluginEnabled ? "有効" : "無効") + " になりました。");
        return true;
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (this.pluginEnabled) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem().getItemStack();
            if (item.getType() == Material.TNT && !this.trustedPlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
                event.getItem().remove();
                player.getInventory().addItem(new ItemStack[]{new ItemStack(Material.SAND, 4)});
                player.getInventory().addItem(new ItemStack[]{new ItemStack(Material.GUNPOWDER, 5)});
                player.sendMessage("TNTの所有は禁止されています。材料に変換しました。");
            }

        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (this.pluginEnabled) {
            Player player = event.getPlayer();
            String worldName = player.getWorld().getName();
            Material placedType = event.getBlock().getType();
            Set<String> blockedItems = (Set)this.getConfig().getStringList("blocked-items").stream().collect(Collectors.toSet());
            if (this.getConfig().getBoolean("worlds." + worldName + ".prevent-block-placement", false) && !blockedItems.contains(placedType.toString())) {
                event.setCancelled(true);
                player.sendMessage("§cこのワールドではブロックの設置が許可されていません！");
            }

        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (this.pluginEnabled) {
            if (event.getCause() == IgniteCause.FLINT_AND_STEEL) {
                Player player = event.getPlayer();
                String reason = this.fireBanReason;
                if (player != null && !this.trustedPlayers.contains(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage("§c火打石の使用は禁止されています！");
                    this.banPlayer(player, this.fireBanReason);
                    player.banPlayer(reason);
                }
            }

        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (this.pluginEnabled) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Player player = event.getPlayer();
                ItemStack item = event.getItem();
                Block block = event.getClickedBlock();
                BlockFace face = event.getBlockFace();
                if (item != null && item.getType() == Material.LAVA_BUCKET && block != null) {
                    Block placedBlock = block.getRelative(face);
                    if (placedBlock.getType() == Material.AIR) {
                        event.setCancelled(true);
                        String reason = this.lavaBanReason;
                        player.sendMessage("§cこのエリアではマグマの使用は禁止されています！");
                        this.banPlayer(player, this.lavaBanReason);
                        player.banPlayer(reason);
                    }
                }
            }

        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        this.getLogger().info("ワールド " + event.getWorld().getName() + " が読み込まれました。");
    }

    public void loadConfiguration() {
        this.trustedPlayers.clear();
        List<String> trustedPlayersList = this.getConfig().getStringList("trusted-players");
        Iterator var2 = trustedPlayersList.iterator();

        while(var2.hasNext()) {
            String uuidString = (String)var2.next();

            try {
                this.trustedPlayers.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException var5) {
                this.getLogger().warning("無効な UUID: " + uuidString);
            }
        }

    }

    private void banPlayer(Player player, String reason) {
        if (this.uuidManager != null) {
            String uuid = this.uuidManager.getUUID(player.getName());
            Punishment.create(player.getName(), uuid, reason, "CONSOLE", PunishmentType.BAN, -1L, (String)null, false);
            if (player.isOnline()) {
                player.kickPlayer(reason);
            }

            Logger var10000 = this.getLogger();
            String var10001 = player.getName();
            var10000.info(var10001 + " がバンされました: " + reason);
        }

    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList();
        if (command.getName().equalsIgnoreCase("trusttnt") || command.getName().equalsIgnoreCase("allowplayer")) {
            if (args.length == 1) {
                if ("add".startsWith(args[0].toLowerCase())) {
                    suggestions.add("add");
                }

                if ("remove".startsWith(args[0].toLowerCase())) {
                    suggestions.add("remove");
                }
            } else if (args.length == 2) {
                Iterator var6 = Bukkit.getOnlinePlayers().iterator();

                while(var6.hasNext()) {
                    Player player = (Player)var6.next();
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        suggestions.add(player.getName());
                    }
                }
            }
        }

        return suggestions;
    }
}
