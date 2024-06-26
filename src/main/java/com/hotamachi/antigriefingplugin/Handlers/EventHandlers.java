package com.hotamachi.antigriefingplugin.Handlers;

import com.hotamachi.antigriefingplugin.AntiGriefingPlugin;
import org.bukkit.Material;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public class EventHandlers implements Listener {

    private final AntiGriefingPlugin plugin;

    public EventHandlers(AntiGriefingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.TNT && !plugin.trustedPlayers.contains(player.getUniqueId())) {
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
}
