package com.hotamachi.antigriefingplugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiGriefingPlugin extends JavaPlugin implements Listener, TabExecutor {
    public Set<UUID> trustedPlayers = new HashSet();
    private UUIDManager uuidManager;
    private String fireBanReason;
    private String lavaBanReason;

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
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("trusttnt")) {
            return this.handleTrustTntCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("reload")) {
            this.reloadConfig();
            this.loadConfiguration();
            sender.sendMessage("§a設定ファイルが再読み込みされました。");
            return true;
        } else if (command.getName().equalsIgnoreCase("toggleworld")) {
            return this.handleToggleWorldCommand(sender, args);
        } else {
            return command.getName().equalsIgnoreCase("allowplayer") ? this.handleAllowPlayerCommand(sender, args) : false;
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
                if (args[0].equalsIgnoreCase("add")) {
                    this.trustedPlayers.add(target.getUniqueId());
                    sender.sendMessage(target.getName() + " はTNTを使用できるようになりました。");
                } else if (args[0].equalsIgnoreCase("remove")) {
                    this.trustedPlayers.remove(target.getUniqueId());
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

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material placedType = event.getBlockPlaced().getType();
        String worldName = player.getWorld().getName();
        List<String> blockedItems = this.getConfig().getStringList("worlds." + worldName + ".blocked-items");
        if (!this.trustedPlayers.contains(player.getUniqueId()) || placedType != Material.TNT) {
            if (placedType != Material.OBSIDIAN && placedType != Material.NETHER_PORTAL) {
                if (this.getConfig().getBoolean("worlds." + worldName + ".prevent-block-placement") && blockedItems.contains(placedType.name())) {
                    event.setCancelled(true);
                    player.sendMessage("§c" + placedType.name() + " の設置は許可されていません！");
                    if (this.getConfig().getBoolean("settings.enable-logging")) {
                        this.getLogger().info(player.getName() + "が" + worldName + "で" + placedType.name() + "を設置しようとしました。");
                    }
                }

            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && item != null && item.getType() == Material.LAVA_BUCKET) {
            Location location = event.getClickedBlock().getLocation();
            String message = String.format("%sさんが座標(%d, %d, %d)で溶岩を使用しました！", player.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Bukkit.getLogger().info(message);
            Bukkit.broadcastMessage(message);
            String reason = this.lavaBanReason;
            if (this.uuidManager != null) {
                Punishment.create(player.getName(), this.uuidManager.getUUID(player.getName()), reason, "CONSOLE", PunishmentType.IP_BAN, -1L, (String)null, false);
            } else {
                this.getLogger().warning("UUIDManager is not initialized properly.");
            }

            player.kickPlayer(reason);
        }

    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getCause() == IgniteCause.FLINT_AND_STEEL) {
            Player player = event.getPlayer();
            if (player != null) {
                Block block = event.getBlock();
                Location location = block.getLocation();
                if (this.isNetherPortalFrame(block)) {
                    return;
                }

                String message = String.format("%sさんが座標(%d, %d, %d)で火打ち石を使用してブロックを燃やそうとしました！", player.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
                Bukkit.getLogger().info(message);
                Bukkit.broadcastMessage(message);
                String reason = this.fireBanReason;
                if (this.uuidManager != null) {
                    Punishment.create(player.getName(), this.uuidManager.getUUID(player.getName()), reason, "CONSOLE", PunishmentType.BAN, -1L, (String)null, false);
                } else {
                    this.getLogger().warning("UUIDManager is not initialized properly.");
                }

                player.kickPlayer(reason);
            }
        }

    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        String worldName = event.getWorld().getName();
        FileConfiguration config = this.getConfig();
        if (!config.contains("worlds." + worldName)) {
            config.set("worlds." + worldName + ".prevent-block-placement", true);
            config.set("worlds." + worldName + ".blocked-items", List.of("TNT", "LAVA_BUCKET"));
            this.saveConfig();
            this.getLogger().info("新しいワールド " + worldName + " のデフォルト設定を追加しました。");
        }

    }

    public void loadConfiguration() {
        FileConfiguration config = this.getConfig();
        config.options().copyDefaults(true);
        this.getServer().getWorlds().forEach((world) -> {
            String worldName = world.getName();
            if (!config.contains("worlds." + worldName)) {
                config.set("worlds." + worldName + ".prevent-block-placement", true);
                config.set("worlds." + worldName + ".blocked-items", List.of("TNT", "LAVA_BUCKET"));
            }

        });
        this.saveConfig();
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("trusttnt") || command.getName().equalsIgnoreCase("allowplayer")) {
            if (args.length == 1) {
                return Arrays.asList("add", "remove");
            }

            if (args.length == 2) {
                List<String> playerNames = new ArrayList();
                Iterator var6 = Bukkit.getOnlinePlayers().iterator();

                while(var6.hasNext()) {
                    Player player = (Player)var6.next();
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
        } else {
            BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
            BlockFace[] var4 = faces;
            int var5 = faces.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                BlockFace face = var4[var6];
                Block relative = block.getRelative(face);
                if (relative.getType() == Material.AIR && this.checkPortalFrame(relative)) {
                    return true;
                }
            }

            return false;
        }
    }

    private boolean checkPortalFrame(Block block) {
        for(int y = 0; y <= 2; ++y) {
            for(int x = -1; x <= 1; ++x) {
                Block relative = block.getRelative(x, y, 0);
                if (relative.getType() != Material.OBSIDIAN) {
                    return false;
                }
            }
        }

        return true;
    }
}
