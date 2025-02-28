/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.hook;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.storage.StorageManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings("deprecation")
public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final DeluxeCoinflipPlugin plugin;
    private final StorageManager storageManager;

    public PlaceholderAPIHook(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @NotNull
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "deluxecoinflip";
    }

    @NotNull
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        Optional<PlayerData> playerDataOptional = storageManager.getPlayer(player.getUniqueId());
        if (playerDataOptional.isEmpty()) {
            return "N/A";
        }

        PlayerData playerData = playerDataOptional.get();
        if (identifier.equals("games_played")) {
            return String.valueOf(playerData.getTotalGames());
        }

        if (identifier.equals("wins")) {
            return String.valueOf(playerData.getWins());
        }

        if (identifier.equals("win_percentage")) {
            return String.valueOf(playerData.getWinPercentage());
        }

        if (identifier.equals("losses")) {
            return String.valueOf(playerData.getLosses());
        }

        if (identifier.equals("profit")) {
            return String.valueOf(playerData.getProfit());
        }

        if (identifier.equals("profit_formatted")) {
            return String.valueOf(playerData.getProfitFormatted());
        }

        if (identifier.equals("total_losses")) {
            return String.valueOf(playerData.getTotalLosses());
        }

        if (identifier.equals("total_losses_formatted")) {
            return String.valueOf(playerData.getTotalLossesFormatted());
        }

        if (identifier.equals("total_gambled")) {
            return String.valueOf(playerData.getTotalGambled());
        }

        if (identifier.equals("total_gambled_formatted")) {
            return String.valueOf(playerData.getTotalGambledFormatted());
        }

        if (identifier.equals("display_broadcast_messages")) {
            return String.valueOf(playerData.isDisplayBroadcastMessages());
        }

        if (identifier.equals("total_games")) {
            return String.valueOf(plugin.getGameManager().getCoinflipGames().size());
        }

        return null;
    }
}

