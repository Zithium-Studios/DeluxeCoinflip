/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.exception.InvalidStorageHandlerException;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.storage.handler.StorageHandler;
import net.zithium.deluxecoinflip.storage.handler.impl.SQLiteHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
        if (plugin.getConfig().getString("storage.type").equalsIgnoreCase("SQLITE")) {
            storageHandler = new SQLiteHandler();
        } else {
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
        plugin.getLogger().info("Saving player data to database...");
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
        DeluxeCoinflipPlugin.getInstance().getScheduler().runTaskAsynchronously(() -> playerDataMap.put(uuid, storageHandler.getPlayer(uuid)));
        DeluxeCoinflipPlugin.getInstance().getScheduler().runTaskAsynchronously(() -> {
            playerDataMap.put(uuid, storageHandler.getPlayer(uuid));

            CoinflipGame game = storageHandler.getCoinflipGame(uuid);
            if (game != null) {
                plugin.getScheduler().runTask(() -> {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    plugin.getEconomyManager().getEconomyProvider(game.getProvider()).deposit(player, game.getAmount());
                    Messages.GAME_REFUNDED.send(player.getPlayer(), "{AMOUNT}", game.getAmount(), "{PROVIDER}", game.getProvider());
                });

                storageHandler.deleteCoinfip(uuid);
            }
        });
    }

    public void savePlayerData(PlayerData player, boolean removeCache) {
        UUID uuid = player.getUUID();
        DeluxeCoinflipPlugin.getInstance().getScheduler().runTaskAsynchronously(() -> {
            storageHandler.savePlayer(player);
            if (removeCache) playerDataMap.remove(uuid);
        });
    }

    public StorageHandler getStorageHandler() {
        return storageHandler;
    }
}
