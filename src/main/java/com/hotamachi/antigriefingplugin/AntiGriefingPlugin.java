package com.hotamachi.antigriefingplugin;


import com.hotamachi.antigriefingplugin.Listener.GriefingListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Set;
import java.util.logging.Logger;

public class AntiGriefingPlugin extends JavaPlugin {
    private Logger logger = getLogger();
    private boolean pluginEnabled = true;

    @Override
    public void onEnable() {
        setupConfig();
        registerListeners();
        registerCommands();
        loadConfiguration();
        logger.info("copyright 2024-2025 hotamachisubaru all rights reserved.");
        logger.info("version " + getDescription().getVersion());
        logger.info("author " + getDescription().getAuthors());
        logger.info("development by hotamachisubaru");
        logger.info("[!] チート対策プラグインを有効化しています。");
    }

    private void setupConfig() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
    }

    public  boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public String getFireBanReason() {
        return getConfig().getString("ban-reasons.fire", "火打ち石を使用してブロックを燃やそうとしたためBANされました。");
    }

    public String getLavaBanReason() {
        return getConfig().getString("ban-reasons.lava", "マグマを使用したためBANされました。");
    }

    @Override
    public void onDisable() {
        logger.info("[!] チート対策プラグインを無効化しています。");
    }

    private void loadConfiguration() {
        saveDefaultConfig();
        pluginEnabled = getConfig().getBoolean("pluginEnabled", true);
    }

    private void registerCommands() {
        var commandHandler = new CommandHandler(this, Set.of());
        getCommand("reload").setExecutor(commandHandler);
        getCommand("antigrief").setExecutor(commandHandler);
        getCommand("world").setExecutor(commandHandler);
        getCommand("baninfo").setExecutor(commandHandler);
    }

    private void registerListeners() {
        var griefingListener = new GriefingListener(this);
        getServer().getPluginManager().registerEvents(griefingListener, this);
    }
}