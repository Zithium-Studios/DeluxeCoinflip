/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.api.events;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CoinflipCompletedEvent extends Event {

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    private final OfflinePlayer winner;
    private final OfflinePlayer loser;
    private final long winnings;

    public CoinflipCompletedEvent(OfflinePlayer winner, OfflinePlayer loser, long winnings) {
        this.winner = winner;
        this.loser = loser;
        this.winnings = winnings;
    }

    public OfflinePlayer getWinner() {
        return winner;
    }

    public OfflinePlayer getLoser() {
        return loser;
    }

    public long getWinnings() {
        return winnings;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}