/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage;

import com.tcoded.folialib.impl.PlatformScheduler;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.exception.InvalidStorageHandlerException;
import net.zithium.deluxecoinflip.storage.handler.StorageHandler;
import net.zithium.deluxecoinflip.storage.handler.impl.MySQLHandler;
import net.zithium.deluxecoinflip.storage.handler.impl.SQLiteHandler;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager implements Listener {

    private final DeluxeCoinflipPlugin plugin;
    private final PlatformScheduler scheduler;
    private final Map<UUID, PlayerData> playerDataMap;
    private StorageHandler storageHandler;

    public StorageManager(final DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = DeluxeCoinflipPlugin.scheduler();
        this.playerDataMap = new ConcurrentHashMap<>();
    }

    public void onEnable() {
        final String configuredType = plugin.getConfig().getString("storage.type", "SQLITE");
        final String storageType = configuredType.trim().toUpperCase(Locale.ROOT);

        switch (storageType) {
            case "SQLITE":
                this.storageHandler = new SQLiteHandler();
                break;
            case "MYSQL":
                this.storageHandler = new MySQLHandler();
                break;
            default:
                throw new InvalidStorageHandlerException("Invalid storage handler specified: " + configuredType);
        }

        if (!this.storageHandler.onEnable(plugin)) {
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        Bukkit.getOnlinePlayers().forEach(player -> this.loadPlayerData(player.getUniqueId()));
    }

    public void onDisable(final boolean shutdown) {
        if (shutdown && this.storageHandler != null) {
            this.storageHandler.onDisable();
        }

        this.playerDataMap.clear();
    }

    public Optional<PlayerData> getPlayer(final UUID uuid) {
        return Optional.ofNullable(this.playerDataMap.get(uuid));
    }

    public PlayerData getOrLoadPlayer(final UUID uuid) {
        return this.playerDataMap.computeIfAbsent(uuid, ignored -> this.storageHandler.getPlayer(uuid));
    }

    public void updateOfflinePlayerWin(final UUID uuid, final long profit, final long beforeTax) {
        this.scheduler.runAsync(task -> {
            final PlayerData playerData = this.storageHandler.getPlayer(uuid);
            playerData.updateWins();
            playerData.updateProfit(profit);
            playerData.updateGambled(beforeTax);
            this.storageHandler.savePlayer(playerData);
        });
    }

    public void updateOfflinePlayerLoss(final UUID uuid, final long beforeTax) {
        this.scheduler.runAsync(task -> {
            final PlayerData playerData = this.storageHandler.getPlayer(uuid);
            playerData.updateLosses();
            playerData.updateLosses(beforeTax);
            playerData.updateGambled(beforeTax);
            this.storageHandler.savePlayer(playerData);
        });
    }

    public void loadPlayerData(final UUID uuid) {
        this.scheduler.runAsync(task -> {
            final PlayerData data = this.storageHandler.getPlayer(uuid);
            this.playerDataMap.put(uuid, data);
        });
    }

    public void savePlayerData(final PlayerData player, final boolean removeCache) {
        final UUID uuid = player.getUUID();
        this.scheduler.runAsync(task -> {
            this.storageHandler.savePlayer(player);
            if (removeCache) {
                this.playerDataMap.remove(uuid);
            } else {
                this.playerDataMap.put(uuid, player);
            }
        });
    }

    public void saveAllPlayerData() {
        this.playerDataMap.values().forEach(player -> this.storageHandler.savePlayer(player));
    }

    public Map<UUID, PlayerData> getPlayerDataMap() {
        return this.playerDataMap;
    }

    public StorageHandler getStorageHandler() {
        return this.storageHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        this.loadPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.getPlayer(event.getPlayer().getUniqueId()).ifPresent(data -> this.savePlayerData(data, true));
    }
}