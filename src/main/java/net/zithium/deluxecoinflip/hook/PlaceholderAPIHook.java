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

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getIdentifier() {
        return "deluxecoinflip";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        Optional<PlayerData> playerDataOptional = storageManager.getPlayer(player.getUniqueId());
        if (!playerDataOptional.isPresent()) {
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

        return null;
    }
}

