package com.hotamachi.antigriefingplugin

import me.leoko.advancedban.manager.UUIDManager
import me.leoko.advancedban.utils.Punishment
import me.leoko.advancedban.utils.PunishmentType
import org.bukkit.Bukkit
import org.bukkit.Bukkit.*
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.logging.Logger

class AntigriefingPlugin : JavaPlugin(), Listener {
    private val trustedPlayers: MutableSet<UUID?> = HashSet<UUID?>()
    private var uuidManager: UUIDManager? = null
    private var fireBanReason: String? = null
    private var lavaBanReason: String? = null
    private var pluginEnabled = false
    private val logger: Logger = getLogger()


    override fun onEnable() {
        logger.info("[!]荒らし対策プラグインを有効化しています。")
        setupSettings()
        loadSettings()
        checkAdvancedBan()
        registerListener()
        registerCommands()
       logger.info("copyright 2024 hotamachisubaru all rights reserved.")
    }

    private fun setupSettings() {
        // 設定ファイルの初期化
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs()
            saveResource("config.yml", false)
            saveDefaultConfig()
        }
    }

    private fun loadSettings() {
        // 設定値をロード
        fireBanReason =
            getConfig().getString("ban-reasons.fire")
        lavaBanReason = getConfig().getString("ban-reasons.lava")
        pluginEnabled = getConfig().getBoolean("pluginEnabled", true)
    }

    private fun checkAdvancedBan() {
        // AdvancedBanの確認
        if (getPluginManager().isPluginEnabled("AdvancedBan")) {
            uuidManager = UUIDManager.get()
        } else {
           logger.warning("AdvancedBan プラグインが見つからないか無効化されています。プラグインを無効化します。")
            getServer().getPluginManager().disablePlugin(this)
        }
    }

    private fun registerListener() {
        getServer().getPluginManager().registerEvents(this, this)
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
            punishLavaPlayer(player, lavaBanReason)
            event.setCancelled(true)
        }
    }

    @EventHandler
    fun onBlockIgnite(event: BlockIgniteEvent) {
        if (!pluginEnabled) return

        if (event.getCause() == IgniteCause.FLINT_AND_STEEL || event.getCause() == IgniteCause.LAVA) {
            val player = event.getPlayer()
            if (player != null) {
                punishFirePlayer(player, fireBanReason)
                event.setCancelled(true)
            }
        }
    }

    private fun punishFirePlayer(player: Player, reason: String?) {
        if (uuidManager != null) {
            Punishment.create(
                player.getName(),
                uuidManager!!.getUUID(player.getName()),
                reason,
                "CONSOLE",
                PunishmentType.KICK,
                1800,
                null,
                false
            )
        }
        player.kickPlayer(reason)
    }

    private fun punishLavaPlayer(player: Player, reason: String?) {
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
        player.banPlayer(reason)
    }

    override fun onDisable() {
       logger.info("荒らし対策プラグインを無効化しました。")
    }
}