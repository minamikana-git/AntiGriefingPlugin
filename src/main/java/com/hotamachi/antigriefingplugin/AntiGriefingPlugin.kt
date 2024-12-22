package com.hotamachi.antigriefingplugin

import me.leoko.advancedban.manager.UUIDManager
import me.leoko.advancedban.utils.Punishment
import me.leoko.advancedban.utils.PunishmentType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

class AntiGriefingPlugin : JavaPlugin() {
    private val trustedPlayers: Set<UUID> = HashSet()
    private var uuidManager: UUIDManager? = null
    private var fireBanReason: String? = null
    private var lavaBanReason: String? = null
    private var pluginEnabled = false

    override fun onEnable() {
        if (!File(dataFolder, "config.yml").exists()) {
            saveDefaultConfig()
        }

        logger.info("copyright 2024 hotamachisubaru all rights reserved.")
        logger.info("version " + description.version)
        logger.info("author " + description.authors)
        logger.info("development by hotamachisubaru")
        logger.info("[!] チート対策プラグインを有効化しています。")

        registerCommands()
        loadConfiguration()
        fireBanReason =
            config.getString("ban-reasons.fire", "火打ち石を使用してブロックを燃やそうとしたためkickされました。")
        lavaBanReason = config.getString("ban-reasons.lava", "マグマを使用したためBANされました。")

        val advancedBan = Bukkit.getPluginManager().getPlugin("AdvancedBan")
        if (advancedBan != null && advancedBan.isEnabled) {
            uuidManager = UUIDManager.get()
        } else {
            logger.warning("AdvancedBan プラグインがないか無効化されています。")
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        logger.info("[!] チート対策プラグインを無効化しています。")
    }

    private fun loadConfiguration() {
        saveDefaultConfig()
        pluginEnabled = config.getBoolean("pluginEnabled", true)
    }

    private fun registerCommands() {
        val commandHandler = CommandHandler(this)
        getCommand("reload")!!.setExecutor(commandHandler)
        getCommand("toggleworld")!!.setExecutor(commandHandler)
        getCommand("toggleantigrief")!!.setExecutor(commandHandler)
    }

    @EventHandler
    fun punishLava(event: PlayerBucketEmptyEvent) {
        if (!pluginEnabled) return

        val player = event.player
        val bucket = event.bucket

        if (bucket == Material.LAVA_BUCKET) {
            punishPlayer(player, lavaBanReason)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun punishIgnite(event: BlockIgniteEvent) {
        if (!pluginEnabled) return

        if (event.cause == IgniteCause.FLINT_AND_STEEL || event.cause == IgniteCause.LAVA) {
            val player = event.player
            if (player != null) {
                punishPlayer(player, fireBanReason)
                event.isCancelled = true
            }
        }
    }

    private fun punishPlayer(player: Player, reason: String?) {
        if (uuidManager != null) {
            Punishment.create(
                player.name,
                uuidManager!!.getUUID(player.name),
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
}
