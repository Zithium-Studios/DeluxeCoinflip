/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.deluxecoinflip.api.events;

import fun.lewisdev.deluxecoinflip.game.CoinflipGame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CoinflipCreatedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    private final Player player;
    private final CoinflipGame coinflipGame;
    private boolean cancelled;

    public CoinflipCreatedEvent(Player player, CoinflipGame coinflipGame) {
        this.player = player;
        this.coinflipGame = coinflipGame;
    }

    public Player getPlayer() {
        return player;
    }

    public CoinflipGame getCoinflipGame() {
        return coinflipGame;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
}