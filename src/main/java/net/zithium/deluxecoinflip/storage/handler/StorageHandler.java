/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage.handler;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.CoinflipHistory;
import net.zithium.deluxecoinflip.storage.PlayerData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StorageHandler {

    /**
     * Enables and connects to the storage backend.
     *
     * @param plugin the plugin instance
     * @return {@code true} if the storage loaded successfully, {@code false} otherwise
     */
    boolean onEnable(final DeluxeCoinflipPlugin plugin);

    /**
     * Disables and disconnects the storage backend.
     */
    void onDisable();

    /**
     * Gets the stored data for a player.
     *
     * @param uuid the player's UUID
     * @return the player's data, or {@code null} if not found
     */
    PlayerData getPlayer(final UUID uuid);

    /**
     * Saves a player's data to storage.
     *
     * @param player the player data to save
     */
    void savePlayer(final PlayerData player);

    /**
     * Saves a coinflip game to storage.
     *
     * @param game the coinflip game to save
     */
    void saveCoinflip(final CoinflipGame game);

    /**
     * Deletes a coinflip game from storage.
     *
     * @param uuid the UUID of the game owner
     */
    void deleteCoinflip(final UUID uuid);

    /**
     * Gets all cached or active coinflip games.
     *
     * @return a map of game owner UUIDs to their coinflip games
     */
    Map<UUID, CoinflipGame> getGames();

    /**
     * Gets a coinflip game by owner UUID.
     *
     * @param uuid the owner's UUID
     * @return the coinflip game, or {@code null} if not found
     */
    CoinflipGame getCoinflipGame(final UUID uuid);

    /**
     * Saves a completed coinflip result to history storage.
     *
     * @param history the completed game history entry
     */
    void saveCoinflipHistory(final CoinflipHistory history);

    /**
     * Gets the total amount of history entries involving the given player.
     *
     * @param uuid the player UUID
     * @return total history count
     */
    int getCoinflipHistoryCount(final UUID uuid);

    /**
     * Gets a paginated slice of coinflip history for a player.
     *
     * @param uuid the player UUID
     * @param offset zero-based row offset
     * @param limit maximum amount of rows to return
     * @return latest history entries involving this player
     */
    List<CoinflipHistory> getCoinflipHistory(final UUID uuid, final int offset, final int limit);

    /**
     * Gets the most recent global coinflip history.
     *
     * @param limit maximum amount of rows to return
     * @return latest history entries
     */
    List<CoinflipHistory> getRecentCoinflipHistory(final int limit);
}