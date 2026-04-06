/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.menu;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.menu.inventories.CoinflipGUI;
import net.zithium.deluxecoinflip.menu.inventories.GameBuilderGUI;
import net.zithium.deluxecoinflip.menu.inventories.GamesGUI;
import net.zithium.deluxecoinflip.menu.inventories.HistoryGUI;

public class InventoryManager {

    private GamesGUI gamesGUI;
    private CoinflipGUI coinflipGUI;
    private GameBuilderGUI gameBuilderGUI;
    private HistoryGUI historyGUI;

    public void load(DeluxeCoinflipPlugin plugin) {
        gamesGUI = new GamesGUI(plugin);
        coinflipGUI = new CoinflipGUI(plugin);
        gameBuilderGUI = new GameBuilderGUI(plugin);
        historyGUI = new HistoryGUI(plugin);
    }

    public GamesGUI getGamesGUI() {
        return gamesGUI;
    }

    public CoinflipGUI getCoinflipGUI() {
        return coinflipGUI;
    }

    public GameBuilderGUI getGameBuilderGUI() {
        return gameBuilderGUI;
    }

    public HistoryGUI getHistoryGUI() {
        return historyGUI;
    }
}
