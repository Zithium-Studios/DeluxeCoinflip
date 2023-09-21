/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.deluxecoinflip.storage.handler;

import fun.lewisdev.deluxecoinflip.DeluxeCoinflipPlugin;
import fun.lewisdev.deluxecoinflip.game.CoinflipGame;
import fun.lewisdev.deluxecoinflip.storage.PlayerData;

import java.util.Map;
import java.util.UUID;

public interface StorageHandler {

    /**
     * Called when a storage handler is enabling
     *
     * @param plugin The plugin
     * @return true if the enable was successful, otherwise false
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

    Map<UUID, CoinflipGame> getGames();

}
