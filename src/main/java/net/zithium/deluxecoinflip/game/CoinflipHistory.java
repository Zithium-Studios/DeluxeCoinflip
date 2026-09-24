/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.game;

import java.util.UUID;

public class CoinflipHistory {

    private final long id;
    private final UUID creatorUUID;
    private final UUID opponentUUID;
    private final UUID winnerUUID;
    private final UUID loserUUID;
    private final String provider;
    private final long betAmount;
    private final long winnings;
    private final long taxDeduction;
    private final double taxRate;
    private final boolean forfeit;
    private final long createdAt;

    public CoinflipHistory(long id,
                           UUID creatorUUID,
                           UUID opponentUUID,
                           UUID winnerUUID,
                           UUID loserUUID,
                           String provider,
                           long betAmount,
                           long winnings,
                           long taxDeduction,
                           double taxRate,
                           boolean forfeit,
                           long createdAt) {
        this.id = id;
        this.creatorUUID = creatorUUID;
        this.opponentUUID = opponentUUID;
        this.winnerUUID = winnerUUID;
        this.loserUUID = loserUUID;
        this.provider = provider;
        this.betAmount = betAmount;
        this.winnings = winnings;
        this.taxDeduction = taxDeduction;
        this.taxRate = taxRate;
        this.forfeit = forfeit;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public UUID getCreatorUUID() {
        return creatorUUID;
    }

    public UUID getOpponentUUID() {
        return opponentUUID;
    }

    public UUID getWinnerUUID() {
        return winnerUUID;
    }

    public UUID getLoserUUID() {
        return loserUUID;
    }

    public String getProvider() {
        return provider;
    }

    public long getBetAmount() {
        return betAmount;
    }

    public long getWinnings() {
        return winnings;
    }

    public long getTaxDeduction() {
        return taxDeduction;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public boolean isForfeit() {
        return forfeit;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}