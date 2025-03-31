/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.economy.provider.impl;

import me.mraxetv.beasttokens.api.BeastTokensAPI;
import me.mraxetv.beasttokens.api.handlers.BTTokensManager;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import org.bukkit.OfflinePlayer;

public class BeastTokensProvider extends EconomyProvider {

    private BTTokensManager tokensManager;

    public BeastTokensProvider() {
        super("BeastTokens");
    }

    @Override
    public void onEnable() {
        tokensManager = BeastTokensAPI.getTokensManager();
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (player.isOnline()) {
            return tokensManager.getTokens(player.getPlayer());
        } else {
            return tokensManager.getTokens(player);
        }
    }

    @Override
    public void withdraw(OfflinePlayer player, double amount) {
        if (player.isOnline()) {
            tokensManager.removeTokens(player.getPlayer(), amount);
        } else {
            tokensManager.removeTokens(player, amount);
        }
    }

    @Override
    public void deposit(OfflinePlayer player, double amount) {
        if (player.isOnline()) {
            tokensManager.addTokens(player.getPlayer(), amount);
        } else {
            tokensManager.addTokens(player, amount);
        }
    }
}
