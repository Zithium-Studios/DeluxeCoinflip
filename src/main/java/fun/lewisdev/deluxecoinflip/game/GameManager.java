/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.deluxecoinflip.game;

import fun.lewisdev.deluxecoinflip.DeluxeCoinflipPlugin;
import fun.lewisdev.deluxecoinflip.config.ConfigHandler;
import fun.lewisdev.deluxecoinflip.config.ConfigType;
import fun.lewisdev.deluxecoinflip.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final DeluxeCoinflipPlugin plugin;
    private final Map<UUID, CoinflipGame> coinflipGames;
    private final StorageManager storageManager;

    public GameManager(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.coinflipGames = new HashMap<>();
        this.storageManager = plugin.getStorageManager();
    }

    /**
     * Load existing games from storage
     */
    public void onEnable() {
        // Load any legacy games (from games.yml) into cache
        // and then delete file to use new SQL based storage.
        Bukkit.getScheduler().runTask(plugin, () -> {
            ConfigHandler dataHandler = new ConfigHandler(plugin, "games");
            if(dataHandler.getFile().exists()) {
                dataHandler.saveDefaultConfig();
                FileConfiguration dataConfig = dataHandler.getConfig();

                if (dataConfig.isSet("games")) {
                    plugin.getLogger().info("Found coinflip games from legacy games.yml storage, converting...");
                    for (String uuid : dataConfig.getConfigurationSection("games").getKeys(false)) {
                        String provider = dataConfig.getString("games." + uuid + ".provider");
                        addCoinflipGame(UUID.fromString(uuid), new CoinflipGame(UUID.fromString(uuid), provider, dataConfig.getLong("games." + uuid + ".amount")));
                    }
                }
                dataHandler.getFile().delete();
            }
        });

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> coinflipGames.putAll(storageManager.getStorageHandler().getGames()));

    }

    /**
     * Add a coinflip game
     *
     * @param uuid The UUID of the player creating the game
     * @param game The coinflip game object
     */
    public void addCoinflipGame(UUID uuid, CoinflipGame game) {
        coinflipGames.put(uuid, game);
        if(Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> storageManager.getStorageHandler().saveCoinflip(game));
        }else{
            storageManager.getStorageHandler().saveCoinflip(game);
        }
    }

    /**
     * Delete an existing coinflip game
     *
     * @param uuid The UUID of the player removing the game
     */
    public void removeCoinflipGame(UUID uuid) {
        coinflipGames.remove(uuid);
        if(Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> storageManager.getStorageHandler().deleteCoinfip(uuid));
        }else{
            storageManager.getStorageHandler().deleteCoinfip(uuid);
        }
    }

    /**
     * Get all coinflip games
     *
     * @return Map of UUID and CoinflipGame object
     */
    public Map<UUID, CoinflipGame> getCoinflipGames() {
        return coinflipGames;
    }
}
