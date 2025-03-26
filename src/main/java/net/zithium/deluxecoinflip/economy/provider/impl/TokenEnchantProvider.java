/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.economy.provider.impl;

import com.vk2gpz.tokenenchant.api.ITokenEnchant;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class TokenEnchantProvider extends EconomyProvider {

    private ITokenEnchant tokenEnchantAPI;

    public TokenEnchantProvider() {
        super("TokenEnchant");
    }

    @Override
    public void onEnable() {
        tokenEnchantAPI = (ITokenEnchant) Bukkit.getServer().getPluginManager().getPlugin("TokenEnchant");
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return tokenEnchantAPI.getTokens(player);
    }

    @Override
    public void withdraw(OfflinePlayer player, double amount) {
        tokenEnchantAPI.removeTokens(player, amount);
    }

    @Override
    public void deposit(OfflinePlayer player, double amount) {
        tokenEnchantAPI.addTokens(player, amount);
    }
}
