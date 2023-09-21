/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.deluxecoinflip.menu;

import fun.lewisdev.deluxecoinflip.DeluxeCoinflipPlugin;
import fun.lewisdev.deluxecoinflip.menu.inventories.CoinflipGUI;
import fun.lewisdev.deluxecoinflip.menu.inventories.GameBuilderGUI;
import fun.lewisdev.deluxecoinflip.menu.inventories.GamesGUI;

public class InventoryManager {

    private GamesGUI gamesGUI;
    private CoinflipGUI coinflipGUI;
    private GameBuilderGUI gameBuilderGUI;

    public void load(DeluxeCoinflipPlugin plugin) {
        gamesGUI = new GamesGUI(plugin);
        coinflipGUI = new CoinflipGUI(plugin);
        gameBuilderGUI = new GameBuilderGUI(plugin);
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
}
