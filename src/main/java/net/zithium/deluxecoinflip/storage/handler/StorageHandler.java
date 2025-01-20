/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.storage.handler;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.storage.PlayerData;

import java.util.Map;
import java.util.UUID;

public interface StorageHandler {

    /**
     * Called when a storage handler is enabling
     *
     * @param plugin The plugin
     * @return true if loaded successfully. false if load has failed.
     */
    boolean onEnable(final DeluxeCoinflipPlugin plugin);

    /**
     * Called when a storage handler is disabling
     */
    void onDisable();

    PlayerData getPlayer(final UUID uuid);

    void savePlayer(final PlayerData player);

    void saveCoinflip(final CoinflipGame game);

    void deleteCoinfip(final UUID uuid);

    void dropGamesTable();

    Map<UUID, CoinflipGame> getGames();

}
