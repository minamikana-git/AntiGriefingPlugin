package com.hotamachi.antigriefingplugin.Listener;

import com.hotamachi.antigriefingplugin.AntiGriefingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import java.util.logging.Logger;

public class GriefingListener implements Listener {
    private final AntiGriefingPlugin plugin;
    private final Logger logger;

    public GriefingListener(AntiGriefingPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

    }
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!plugin.isPluginEnabled()) return;

        if ((event.getCause() == IgniteCause.FLINT_AND_STEEL || event.getCause() == IgniteCause.LAVA) && event.getPlayer() != null) {
            event.setCancelled(true);
            punishPlayer(event.getPlayer(),plugin.getFireBanReason());
        }
    }
    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!plugin.isPluginEnabled()) return;
        if (event.getBucket() == org.bukkit.Material.LAVA_BUCKET) {
            event.setCancelled(true);
            punishPlayer(event.getPlayer(),plugin.getLavaBanReason());
        }
    }

    private void punishPlayer(org.bukkit.entity.Player player, String reason) {
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(player.getName(), reason, null, null);
        player.kickPlayer(reason);
    }
}