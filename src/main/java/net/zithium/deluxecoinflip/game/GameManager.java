/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.game;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class GameManager {

    private final DeluxeCoinflipPlugin plugin;
    private final Map<UUID, CoinflipGame> coinflipGames;
    private final StorageManager storageManager;

    private final TreeMap<Double, Double> taxBrackets = new TreeMap<>();

    private boolean canStartGame = false;

    public GameManager(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.coinflipGames = new HashMap<>();
        this.storageManager = plugin.getStorageManager();
    }

    public void onEnable() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("settings.tax.brackets");
        double defaultTaxRate = plugin.getConfig().getDouble("settings.tax.rate");
        if (section == null || defaultTaxRate > 0) {
            taxBrackets.put(0.0, defaultTaxRate);
            return;
        }

        for (String key : section.getKeys(false)) {
            double amount = section.getDouble(key + ".amount");
            double tax = section.getDouble(key + ".rate");
            taxBrackets.put(amount, tax);
        }
    }

    public double calculateTax(double amount) {
        return taxBrackets.floorEntry(amount).getValue();
    }

    /**
     * Add a coinflip game
     *
     * @param uuid The UUID of the player creating the game
     * @param game The coinflip game object
     */
    public void addCoinflipGame(UUID uuid, CoinflipGame game) {
        coinflipGames.put(uuid, game);
        if (Bukkit.isPrimaryThread()) {
            plugin.getScheduler().runTaskAsynchronously(() -> storageManager.getStorageHandler().saveCoinflip(game));
        } else {
            storageManager.getStorageHandler().saveCoinflip(game);
        }
    }

    /**
     * Delete an existing coinflip game
     *
     * @param uuid The UUID of the player removing the game
     */
    public void removeCoinflipGame(UUID uuid, boolean refund) {
        CoinflipGame removed = this.coinflipGames.remove(uuid);
        if (Bukkit.isPrimaryThread()) {
            plugin.getScheduler().runTaskAsynchronously(() -> storageManager.getStorageHandler().deleteCoinfip(uuid));
        } else {
            storageManager.getStorageHandler().deleteCoinfip(uuid);
        }

        if (removed != null && refund) {
            EconomyProvider provider = plugin.getEconomyManager().getEconomyProvider(removed.getProvider());
            if (provider != null) {
                provider.deposit(Bukkit.getOfflinePlayer(removed.getPlayerUUID()), removed.getAmount());
            }
        }
    }

    public void removeCoinflipGame(UUID uniqueId) {
        removeCoinflipGame(uniqueId, false);
    }

    /**
     * Get all coinflip games
     *
     * @return Map of UUID and CoinflipGame object
     */
    public Map<UUID, CoinflipGame> getCoinflipGames() {
        return coinflipGames;
    }

    public boolean canStartGame() {
        return canStartGame;
    }

    public void canStartGame(boolean canStartGame) {
        this.canStartGame = canStartGame;
    }

}
