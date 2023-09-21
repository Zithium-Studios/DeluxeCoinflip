/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.deluxecoinflip.economy.provider.impl;

import fun.lewisdev.deluxecoinflip.economy.provider.EconomyProvider;
import me.revils.revenchants.api.CurrencyReceiveReason;
import me.revils.revenchants.api.RevEnchantsApi;
import org.bukkit.OfflinePlayer;

public class RevEnchantsProvider extends EconomyProvider {

    public RevEnchantsProvider() {
        super("RevEnchants");
    }

    @Override
    public void onEnable() {
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return RevEnchantsApi.getCurrency(player, "RevTokens");
    }

    @Override
    public void withdraw(OfflinePlayer player, double amount) {
        RevEnchantsApi.withdrawCurrency(player, Double.valueOf(amount).longValue(), "RevTokens");
    }

    @Override
    public void deposit(OfflinePlayer player, double amount) {
        RevEnchantsApi.depositCurrency(player, Double.valueOf(amount).longValue(), "RevTokens", CurrencyReceiveReason.CONSOLE);
    }
}
