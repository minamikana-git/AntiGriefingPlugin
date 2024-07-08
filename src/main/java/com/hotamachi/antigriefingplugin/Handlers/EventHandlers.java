package com.hotamachi.antigriefingplugin.Handlers;

import com.hotamachi.antigriefingplugin.AntiGriefingPlugin;
import me.leoko.advancedban.manager.UUIDManager;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedban.utils.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class EventHandlers implements Listener {

    private final AntiGriefingPlugin plugin;
    private final UUIDManager uuidManager;

    public EventHandlers(AntiGriefingPlugin plugin) {
        this.plugin = plugin;
        Plugin advancedBan = Bukkit.getPluginManager().getPlugin("AdvancedBan");
        if (advancedBan != null && advancedBan.isEnabled()) {
            this.uuidManager = UUIDManager.get();
        } else {
            throw new IllegalStateException("AdvancedBan plugin not found or not enabled!");
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.TNT && !plugin.getTrustedPlayers().contains(player.getUniqueId())) {
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
        List<String> blockedItems = plugin.getConfig().getStringList("worlds." + worldName + ".blocked-items");

        // プレイヤーが信頼されている場合、TNTの設置を許可
        if (plugin.getTrustedPlayers().contains(player.getUniqueId()) && placedType == Material.TNT) {
            return;
        }

        // ネザーゲート関連のブロックを除外
        if (placedType == Material.OBSIDIAN || placedType == Material.NETHER_PORTAL) {
            return;
        }

        if (plugin.getConfig().getBoolean("worlds." + worldName + ".prevent-block-placement")) {
            if (blockedItems.contains(placedType.name())) {
                event.setCancelled(true);
                player.sendMessage("§c" + placedType.name() + " の設置は許可されていません！");
                if (plugin.getConfig().getBoolean("settings.enable-logging")) {
                    plugin.getLogger().info(player.getName() + "が" + worldName + "で" + placedType.name() + "を設置しようとしました。");
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
                String reason = plugin.getConfig().getString("ban-reasons.lava", "溶岩を使用したためBANされました。");
                if (uuidManager != null) {
                    Punishment.create(
                            player.getName(), // Player's name
                            uuidManager.getUUID(player.getName()), // Player's UUID
                            reason, // Reason
                            "CONSOLE", // Operator name
                            PunishmentType.IP_BAN, // Type of punishment
                            -1L, // Duration (-1 for permanent ban)
                            null, // Calculation
                            true // Silent
                    );
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
                String reason = plugin.getConfig().getString("ban-reasons.fire", "火打ち石を使用してブロックを燃やそうとしたためBANされました。");
                if (uuidManager != null) {
                    Punishment.create(
                            player.getName(), // Player's name
                            uuidManager.getUUID(player.getName()), // Player's UUID
                            reason, // Reason
                            "CONSOLE", // Operator name
                            PunishmentType.BAN, // Type of punishment
                            -1L, // Duration (-1 for permanent ban)
                            null, // Calculation
                            true // Silent
                    );
                }

                // プレイヤーをキックする
                player.kickPlayer(reason);
            }
        }
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