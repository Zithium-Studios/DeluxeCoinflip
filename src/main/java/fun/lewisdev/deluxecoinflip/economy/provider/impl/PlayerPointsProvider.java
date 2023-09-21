/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.deluxecoinflip.economy.provider.impl;

import fun.lewisdev.deluxecoinflip.economy.provider.EconomyProvider;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.OfflinePlayer;

public class PlayerPointsProvider extends EconomyProvider {

    private PlayerPointsAPI api;

    public PlayerPointsProvider() {
        super("PlayerPoints");
    }

    @Override
    public void onEnable() {
        api = PlayerPoints.getInstance().getAPI();
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return api.look(player.getUniqueId());
    }

    @Override
    public void withdraw(OfflinePlayer player, double amount) {
        api.take(player.getUniqueId(), (int) amount);
    }

    @Override
    public void deposit(OfflinePlayer player, double amount) {
        api.give(player.getUniqueId(), (int) amount);
    }
}
