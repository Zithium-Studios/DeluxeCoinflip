/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.game;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.storage.StorageManager;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final DeluxeCoinflipPlugin plugin;
    private final Map<UUID, CoinflipGame> coinflipGames;
    private final StorageManager storageManager;

    private boolean canStartGame = false;

    public GameManager(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.coinflipGames = new HashMap<>();
        this.storageManager = plugin.getStorageManager();
    }
    /**
     * Add a coinflip game
     *
     * @param uuid The UUID of the player creating the game
     * @param game The coinflip game object
     */
    public void addCoinflipGame(UUID uuid, CoinflipGame game) {
        coinflipGames.put(uuid, game);
        if (Bukkit.isPrimaryThread()) {
            plugin.getScheduler().runTaskAsynchronously(() -> storageManager.getStorageHandler().saveCoinflip(game));
        } else {
            storageManager.getStorageHandler().saveCoinflip(game);
        }
    }

    /**
     * Delete an existing coinflip game
     *
     * @param uuid The UUID of the player removing the game
     */
    public void removeCoinflipGame(UUID uuid) {
        coinflipGames.remove(uuid);
        if (Bukkit.isPrimaryThread()) {
            plugin.getScheduler().runTaskAsynchronously(() -> storageManager.getStorageHandler().deleteCoinfip(uuid));
        } else {
            storageManager.getStorageHandler().deleteCoinfip(uuid);
        }
    }

    /**
     * Get all coinflip games
     *
     * @return Map of UUID and CoinflipGame object
     */
    public Map<UUID, CoinflipGame> getCoinflipGames() {
        return coinflipGames;
    }

    public boolean canStartGame() {
        return canStartGame;
    }

    public void canStartGame(boolean canStartGame) {
        this.canStartGame = canStartGame;
    }
}
