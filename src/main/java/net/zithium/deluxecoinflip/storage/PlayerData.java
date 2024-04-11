/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.storage;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int wins, losses;
    private long profit, totalLosses, totalGambled;
    private boolean displayBroadcastMessages;

    public PlayerData(UUID uuid, int wins, int losses, long profit, long totalLosses, long totalGambled, boolean displayBroadcastMessages) {
        this.uuid = uuid;
        this.losses = losses;
        this.wins = wins;
        this.profit = profit;
        this.totalLosses = totalLosses;
        this.totalGambled = totalGambled;
        this.displayBroadcastMessages = displayBroadcastMessages;
    }

    public PlayerData(UUID uuid, int wins, int losses, long profit, long totalLosses, long totalGambled) {
        this(uuid, wins, losses, profit, totalLosses, totalGambled, true);
    }

    public PlayerData(UUID uuid) {
        this(uuid, 0, 0, 0, 0, 0, true);
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void setProfit(long profit) {
        this.profit = profit;
    }

    public int getTotalGames() {
        return wins + losses;
    }

    public int getLosses() {
        return losses;
    }

    public int getWins() {
        return wins;
    }

    public long getProfit() {
        return profit;
    }

    public void setTotalLosses(long totalLosses) {
        this.totalLosses = totalLosses;
    }

    public void setTotalGambled(long totalGambled) {
        this.totalGambled = totalGambled;
    }

    public String getProfitFormatted() {
        return NumberFormat.getNumberInstance(Locale.US).format(profit);
    }

    public long getTotalLosses() {
        return totalLosses;
    }

    public String getTotalLossesFormatted() {
        return NumberFormat.getNumberInstance(Locale.US).format(totalLosses);
    }

    public long getTotalGambled() {
        return totalGambled;
    }

    public void updateWins() {
        wins++;
    }

    public void updateLosses() {
        losses++;
    }

    public void updateProfit(long profit) {
        this.profit += profit;
    }

    public void updateLosses(long losses) {
        this.totalLosses += losses;
    }

    public void updateGambled(long gambled) {
        this.totalGambled += gambled;
    }

    public double getWinPercentage() {
        if (wins + losses == 0 || wins == 0) return 0.0;
        return Double.parseDouble(new DecimalFormat("##.##").format((wins * 100L) / (wins + losses)));
    }

    public boolean isDisplayBroadcastMessages() {
        return displayBroadcastMessages;
    }

    public void setDisplayBroadcastMessages(boolean value) {
        this.displayBroadcastMessages = value;
    }

}
