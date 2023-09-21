/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.deluxecoinflip.economy.provider.impl;

import dev.drawethree.ultraprisoncore.UltraPrisonCore;
import fun.lewisdev.deluxecoinflip.economy.provider.EconomyProvider;
import dev.drawethree.ultraprisoncore.api.enums.LostCause;
import dev.drawethree.ultraprisoncore.api.enums.ReceiveCause;
import dev.drawethree.ultraprisoncore.tokens.api.UltraPrisonTokensAPI;
import org.bukkit.OfflinePlayer;

public class UltraPrisonCoreTokensProvider extends EconomyProvider {

    private UltraPrisonTokensAPI tokensAPI;

    public UltraPrisonCoreTokensProvider() {
        super("UltraPrisonCore");
    }

    @Override
    public void onEnable() {
        tokensAPI = UltraPrisonCore.getInstance().getTokens().getApi();
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return tokensAPI.getPlayerTokens(player);
    }

    @Override
    public void withdraw(OfflinePlayer player, double amount) {
        tokensAPI.removeTokens(player, (long) amount, LostCause.WITHDRAW);
    }

    @Override
    public void deposit(OfflinePlayer player, double amount) {
        tokensAPI.addTokens(player, (long) amount, ReceiveCause.GIVE);
    }
}
