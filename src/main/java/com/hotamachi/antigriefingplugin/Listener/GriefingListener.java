package com.hotamachi.antigriefingplugin.Listener;

import com.hotamachi.antigriefingplugin.AntiGriefingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

public class GriefingListener implements Listener {
    private final AntiGriefingPlugin plugin;
    private final Logger logger;
    private final HashMap<UUID, Integer> blockBreakCount = new HashMap<>();

    public GriefingListener(AntiGriefingPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        new BukkitRunnable() {
            @Override
            public void run() {
                blockBreakCount.clear();
            }
        }.runTaskTimer(plugin, 0, 20L);
    }

    @EventHandler
    public void punishNuker(BlockIgniteEvent event) {
        if (!plugin.isPluginEnabled()) return;
        UUID playerUUID = event.getPlayer().getUniqueId();
        blockBreakCount.put(playerUUID, blockBreakCount.getOrDefault(playerUUID, 0) + 1);

        int maxBreaksPerSecond = plugin.getConfig().getInt("max-breaks-per-second", 10);
        if (blockBreakCount.get(playerUUID) > maxBreaksPerSecond) {
            event.setCancelled(true);
            punishPlayer(event.getPlayer(), "Nukerの可能性があるため行動が制限されました。");
            logger.warning(event.getPlayer().getName() + "が不正なブロック破壊を試みました。");
        }

    }


    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!plugin.isPluginEnabled()) return;

        if ((event.getCause() == IgniteCause.FLINT_AND_STEEL || event.getCause() == IgniteCause.LAVA) && event.getPlayer() != null) {
            event.setCancelled(true);
            punishPlayer(event.getPlayer(), plugin.getFireBanReason());
        }
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!plugin.isPluginEnabled()) return;
        if (event.getBucket() == org.bukkit.Material.LAVA_BUCKET) {
            event.setCancelled(true);
            punishPlayer(event.getPlayer(), plugin.getLavaBanReason());
        }
    }

    private void punishPlayer(Player player, String reason) {
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(player.getName(), reason, null, null);
        player.kickPlayer(reason);
    }
}



