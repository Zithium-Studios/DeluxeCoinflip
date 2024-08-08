/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.storage;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.exception.InvalidStorageHandlerException;
import net.zithium.deluxecoinflip.storage.handler.StorageHandler;
import net.zithium.deluxecoinflip.storage.handler.impl.SQLiteHandler;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

public class StorageManager {

    private final DeluxeCoinflipPlugin plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private StorageHandler storageHandler;

    public StorageManager(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
    }

    public void onEnable() {
        switch (plugin.getConfig().getString("storage.type").toUpperCase()) {
            case "SQLITE":
                storageHandler = new SQLiteHandler();
                break;
            default:
                throw new InvalidStorageHandlerException("Invalid storage handler specified");
        }

        if (!storageHandler.onEnable(plugin)) {
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        Stream.of(
                new Listener() {
                    @EventHandler(priority = EventPriority.MONITOR)
                    public void onPlayerJoin(final PlayerJoinEvent event) {
                        loadPlayerData(event.getPlayer().getUniqueId());
                    }

                }, new Listener() {
                    @EventHandler(priority = EventPriority.MONITOR)
                    public void onPlayerQuit(final PlayerQuitEvent event) {
                        getPlayer(event.getPlayer().getUniqueId()).ifPresent(data -> savePlayerData(data, true));
                    }
                }).forEach(listener -> plugin.getServer().getPluginManager().registerEvents(listener, plugin));

        Bukkit.getOnlinePlayers().forEach(player -> loadPlayerData(player.getUniqueId()));
    }

    public void onDisable(boolean shutdown) {
        // Delete old data folder if empty
        File directory = new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "data");
        if (directory.exists() && isDirectoryEmpty(directory.toPath())) directory.delete();

        plugin.getLogger().info("Saving player data to database..");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.execute(() -> {
            for (PlayerData player : new ArrayList<>(playerDataMap.values())) {
                storageHandler.savePlayer(player);
            }

            if (shutdown) {
                playerDataMap.clear();
                storageHandler.onDisable();
            }

        });
        scheduler.shutdown();
    }

    public Optional<PlayerData> getPlayer(UUID uuid) {
        return Optional.ofNullable(playerDataMap.get(uuid));
    }

    public void updateOfflinePlayerWin(UUID uuid, long profit, long beforeTax) {
        PlayerData playerData = storageHandler.getPlayer(uuid);
        playerData.updateWins();
        playerData.updateProfit(profit);
        playerData.updateGambled(beforeTax);
        savePlayerData(playerData, false);
    }

    public void updateOfflinePlayerLoss(UUID uuid, long beforeTax) {
        PlayerData playerData = storageHandler.getPlayer(uuid);
        playerData.updateLosses();
        playerData.updateLosses(beforeTax);
        playerData.updateGambled(beforeTax);
        savePlayerData(playerData, false);
    }

    public void loadPlayerData(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> playerDataMap.put(uuid, storageHandler.getPlayer(uuid)));
    }

    public void savePlayerData(PlayerData player, boolean removeCache) {
        UUID uuid = player.getUUID();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            storageHandler.savePlayer(player);
            if (removeCache) playerDataMap.remove(uuid);
        });
    }

    public StorageHandler getStorageHandler() {
        return storageHandler;
    }

    private static boolean isDirectoryEmpty(final Path directory) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException ex) {
            return false;
        }
    }
}
