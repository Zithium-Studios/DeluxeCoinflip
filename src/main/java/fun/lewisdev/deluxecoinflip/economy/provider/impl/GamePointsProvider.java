/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.deluxecoinflip.economy.provider.impl;

import fun.lewisdev.deluxecoinflip.economy.provider.EconomyProvider;
import org.bukkit.OfflinePlayer;
import su.nightexpress.gamepoints.api.GamePointsAPI;

public class GamePointsProvider extends EconomyProvider {

    public GamePointsProvider() {
        super("GamePoints");
    }

    @Override
    public void onEnable() {

    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return GamePointsAPI.getUserData(player.getPlayer()).getBalance();
    }

    @Override
    public void withdraw(OfflinePlayer player, double amount) {
        GamePointsAPI.getUserData(player.getPlayer()).takePoints((int) amount);
    }

    @Override
    public void deposit(OfflinePlayer player, double amount) {
        GamePointsAPI.getUserData(player.getPlayer()).addPoints((int) amount);
    }
}
