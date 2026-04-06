/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.game;

import com.tcoded.folialib.impl.PlatformScheduler;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.storage.StorageManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final DeluxeCoinflipPlugin plugin;
    private final PlatformScheduler scheduler;
    private final Map<UUID, CoinflipGame> coinflipGames;
    private final StorageManager storageManager;

    public GameManager(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = DeluxeCoinflipPlugin.scheduler();
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
        scheduler.runAsync(task -> storageManager.getStorageHandler().saveCoinflip(game));
    }

    /**
     * Delete an existing coinflip game
     *
     * <p>Scheduling on Folia when the plugin is disabling does not
     * work and shoots an exception. Please refrain from modifying
     * this logic unless you know what you're doing.</p>
     *
     * @param uuid The UUID of the player removing the game
     */
    public void removeCoinflipGame(@NotNull UUID uuid) {
        coinflipGames.remove(uuid);

        if (!plugin.isEnabled()) {
            try {
                storageManager.getStorageHandler().deleteCoinflip(uuid);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to delete coinflip for " + uuid + " during shutdown: " + ex.getMessage());
            }

            return;
        }

        scheduler.runAsync(task -> {
            try {
                storageManager.getStorageHandler().deleteCoinflip(uuid);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to delete coinflip for " + uuid + ": " + ex.getMessage());
            }
        });
    }

    /**
     * Save a completed game into the persistent history storage.
     *
     * <p>This should only be called once a winner/loser has been decided
     * and all balances/statistics have been finalized.</p>
     *
     * @param history the completed history entry
     */
    public void saveHistory(@NotNull CoinflipHistory history) {
        if (!plugin.isEnabled()) {
            try {
                storageManager.getStorageHandler().saveCoinflipHistory(history);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to save coinflip history during shutdown: " + ex.getMessage());
            }

            return;
        }

        scheduler.runAsync(task -> {
            try {
                storageManager.getStorageHandler().saveCoinflipHistory(history);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to save coinflip history: " + ex.getMessage());
            }
        });
    }

    /**
     * Get all coinflip games
     *
     * @return Map of UUID and CoinflipGame object
     */
    public Map<UUID, CoinflipGame> getCoinflipGames() {
        return coinflipGames;
    }

    public CoinflipGame getCoinflipGame(@NotNull UUID playerUUID) {
        return coinflipGames.get(playerUUID);
    }
}
