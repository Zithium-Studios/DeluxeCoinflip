/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.hook;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.storage.StorageManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

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
        return switch (identifier) {
            case "games_played" -> String.valueOf(playerData.getTotalGames());
            case "wins" -> String.valueOf(playerData.getWins());
            case "win_percentage" -> String.valueOf(playerData.getWinPercentage());
            case "losses" -> String.valueOf(playerData.getLosses());
            case "profit" -> String.valueOf(playerData.getProfit());
            case "profit_formatted" -> String.valueOf(playerData.getProfitFormatted());
            case "total_losses" -> String.valueOf(playerData.getTotalLosses());
            case "total_losses_formatted" -> String.valueOf(playerData.getTotalLossesFormatted());
            case "total_gambled" -> String.valueOf(playerData.getTotalGambled());
            case "total_gambled_formatted" -> String.valueOf(playerData.getTotalGambledFormatted());
            case "display_broadcast_messages" -> String.valueOf(playerData.isDisplayBroadcastMessages());
            case "total_games" -> String.valueOf(plugin.getGameManager().getCoinflipGames().size());
            default -> null;
        };
    }
}
