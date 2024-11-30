package com.hotamachi.antigriefingplugin

import me.leoko.advancedban.manager.UUIDManager
import me.leoko.advancedban.utils.Punishment
import me.leoko.advancedban.utils.PunishmentType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class AntiGriefingPlugin : JavaPlugin(), Listener {
    private val trustedPlayers: MutableSet<UUID?> = HashSet<UUID?>()
    private var uuidManager: UUIDManager? = null
    private var fireBanReason: String? = null
    private var lavaBanReason: String? = null
    private var pluginEnabled = false

    override fun onEnable() {
        // 設定ファイルの初期化
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs()
        }
        saveDefaultConfig()

        // 設定値をロード
        fireBanReason =
            getConfig().getString("ban-reasons.fire", "火打ち石を使用してブロックを燃やそうとしたためBANされました。")
        lavaBanReason = getConfig().getString("ban-reasons.lava", "マグマを使用したためBANされました。")
        pluginEnabled = getConfig().getBoolean("pluginEnabled", true)

        // AdvancedBanの確認
        if (Bukkit.getPluginManager().isPluginEnabled("AdvancedBan")) {
            uuidManager = UUIDManager.get()
        } else {
            getLogger().warning("AdvancedBan プラグインが見つからないか無効化されています。プラグインを無効化します。")
            getServer().getPluginManager().disablePlugin(this)
            return
        }

        // イベントリスナーとコマンドの登録
        getServer().getPluginManager().registerEvents(this, this)
        registerCommands()
    }

    override fun onDisable() {
        getLogger().info("荒らし対策プラグインを無効化しました。")
    }

    private fun registerCommands() {
        val commandHandler = CommandHandler(this)
        getCommand("reload")!!.setExecutor(commandHandler)
        getCommand("toggleworld")!!.setExecutor(commandHandler)
        getCommand("toggleantigrief")!!.setExecutor(commandHandler)
    }

    @EventHandler
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (!pluginEnabled) return

        val player = event.getPlayer()
        if (event.getBucket() == Material.LAVA_BUCKET) {
            punishPlayer(player, lavaBanReason)
            event.setCancelled(true)
        }
    }

    @EventHandler
    fun onBlockIgnite(event: BlockIgniteEvent) {
        if (!pluginEnabled) return

        if (event.getCause() == IgniteCause.FLINT_AND_STEEL || event.getCause() == IgniteCause.LAVA) {
            val player = event.getPlayer()
            if (player != null) {
                punishPlayer(player, fireBanReason)
                event.setCancelled(true)
            }
        }
    }

    private fun punishPlayer(player: Player, reason: String?) {
        if (uuidManager != null) {
            Punishment.create(
                player.getName(),
                uuidManager!!.getUUID(player.getName()),
                reason,
                "CONSOLE",
                PunishmentType.BAN,
                -1L,
                null,
                false
            )
        }
        player.kickPlayer(reason)
    }
}
