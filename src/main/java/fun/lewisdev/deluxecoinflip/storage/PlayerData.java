/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.deluxecoinflip.storage;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int wins, losses;
    private long profit;
    private boolean displayBroadcastMessages;

    public PlayerData(UUID uuid, int wins, int losses, long profit, boolean displayBroadcastMessages) {
        this.uuid = uuid;
        this.losses = losses;
        this.wins = wins;
        this.profit = profit;
        this.displayBroadcastMessages = displayBroadcastMessages;
    }

    public PlayerData(UUID uuid, int wins, int losses, long profit) {
        this(uuid, wins, losses, profit, true);
    }

    public PlayerData(UUID uuid) {
        this(uuid, 0, 0, 0, true);
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

    public String getProfitFormatted() {
        return NumberFormat.getNumberInstance(Locale.US).format(profit);
    }

    public void updateWins() {
        wins++;
    }

    public void updateLosses() {
        losses++;
    }

    public void updateProfit(double profit) {
        this.profit += profit;
    }

    public double getWinPercentage() {
        if(wins + losses == 0 || wins == 0) return 0.0;
        return Double.parseDouble(new DecimalFormat("##.##").format((wins * 100) / (wins + losses)));
    }

    public boolean isDisplayBroadcastMessages() {
        return displayBroadcastMessages;
    }

    public void setDisplayBroadcastMessages(boolean value) {
        this.displayBroadcastMessages = value;
    }
}
