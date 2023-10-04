/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.deluxecoinflip;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fun.lewisdev.deluxecoinflip.api.DeluxeCoinflipAPI;
import fun.lewisdev.deluxecoinflip.command.CoinflipCommand;
import fun.lewisdev.deluxecoinflip.config.ConfigHandler;
import fun.lewisdev.deluxecoinflip.config.ConfigType;
import fun.lewisdev.deluxecoinflip.config.Messages;
import fun.lewisdev.deluxecoinflip.economy.EconomyManager;
import fun.lewisdev.deluxecoinflip.economy.provider.EconomyProvider;
import fun.lewisdev.deluxecoinflip.game.CoinflipGame;
import fun.lewisdev.deluxecoinflip.game.GameManager;
import fun.lewisdev.deluxecoinflip.hook.PlaceholderAPIHook;
import fun.lewisdev.deluxecoinflip.listener.PlayerChatListener;
import fun.lewisdev.deluxecoinflip.menu.InventoryManager;
import fun.lewisdev.deluxecoinflip.storage.PlayerData;
import fun.lewisdev.deluxecoinflip.storage.StorageManager;
import me.mattstudios.mf.base.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DeluxeCoinflipPlugin extends JavaPlugin implements DeluxeCoinflipAPI {

    private Map<ConfigType, ConfigHandler> configMap;
    private StorageManager storageManager;
    private GameManager gameManager;
    private InventoryManager inventoryManager;
    private EconomyManager economyManager;

    private Cache<UUID, CoinflipGame> listenerCache;

    public void onEnable() {
        long start = System.currentTimeMillis();

        getLogger().log(Level.INFO, "");
        getLogger().log(Level.INFO, " __ __    DeluxeCoinflip v" + getDescription().getVersion());
        getLogger().log(Level.INFO, "/  |_     Author: " + getDescription().getAuthors().get(0));
        getLogger().log(Level.INFO, "\\_ |      (c) Lewis D 2020-2022. All rights reserved.");
        getLogger().log(Level.INFO, "");

        listenerCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).maximumSize(500).build();

        // Register configurations
        configMap = new HashMap<>();
        registerConfig(ConfigType.CONFIG);
        registerConfig(ConfigType.MESSAGES);
        Messages.setConfiguration(configMap.get(ConfigType.MESSAGES).getConfig());

        // Load storage
        storageManager = new StorageManager(this);
        try {
            storageManager.onEnable();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "There was an issue attempting to load the storage handler.", ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        (economyManager = new EconomyManager(this)).onEnable();
        (gameManager = new GameManager(this)).onEnable();

        inventoryManager = new InventoryManager();
        inventoryManager.load(this);

        // Load command manager
        CommandManager commandManager = new CommandManager(this, true);
        commandManager.getCompletionHandler().register("#providers", input -> economyManager.getEconomyProviders().values().stream().map(EconomyProvider::getDisplayName).collect(Collectors.toList()));
        commandManager.getMessageHandler().register("cmd.no.permission", Messages.NO_PERMISSION::send);
        // Register commands
        commandManager.register(new CoinflipCommand(this, getConfigHandler(ConfigType.CONFIG).getConfig().getStringList("settings.command_aliases")));

        // Register listeners
        new PlayerChatListener(this);

        // PlaceholderAPI Hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
            getLogger().log(Level.INFO, "Hooked into PlaceholderAPI successfully");
        }

        getLogger().log(Level.INFO, "");
        getLogger().log(Level.INFO, "Successfully loaded in " + (System.currentTimeMillis() - start) + "ms");
        getLogger().log(Level.INFO, "");
    }

    public void onDisable() {
        if (storageManager != null) storageManager.onDisable(true);
    }

    // Plugin reload handling
    public void reload() {
        configMap.values().forEach(ConfigHandler::reload);
        Messages.setConfiguration(configMap.get(ConfigType.MESSAGES).getConfig());

        inventoryManager.load(this);
        economyManager.onEnable();
    }

    // Method to register a configuration file
    private void registerConfig(ConfigType type) {
        ConfigHandler handler = new ConfigHandler(this, type.toString().toLowerCase());
        handler.saveDefaultConfig();
        configMap.put(type, handler);
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public ConfigHandler getConfigHandler(ConfigType type) {
        return configMap.get(type);
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public Cache<UUID, CoinflipGame> getListenerCache() {
        return listenerCache;
    }

    // API methods
    @Override
    public void registerEconomyProvider(EconomyProvider provider, String requiredPlugin) {
        economyManager.registerEconomyProvider(provider, requiredPlugin);
    }

    @Override
    public Optional<PlayerData> getPlayerData(Player player) {
        return storageManager.getPlayer(player.getUniqueId());
    }
}



