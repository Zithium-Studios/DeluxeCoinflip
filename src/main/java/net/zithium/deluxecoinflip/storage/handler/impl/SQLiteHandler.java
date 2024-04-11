/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2023 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage.handler.impl;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.storage.handler.StorageHandler;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteHandler implements StorageHandler {

    private DeluxeCoinflipPlugin plugin;
    private File file;
    private Connection connection;

    @Override
    public boolean onEnable(final DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "database.db");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Error occurred while creating the database file.", e);
                return false;
            }
        }
        createTable();
        return true;
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while closing the database connection.", e);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + file);
            }
            return connection;
        } catch (SQLException | ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to setup the database connection.", ex);
        }
        return connection;
    }

    private void createTable() {
        try (Connection tableConnection = getConnection();
             Statement statement = tableConnection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(255) NOT NULL PRIMARY KEY, " +
                    "wins INTEGER, " +
                    "losses INTEGER, " +
                    "profit BIGINT," +
                    "broadcasts BOOLEAN);";
            statement.execute(sql);

            sql = "CREATE TABLE IF NOT EXISTS games (" +
                    "uuid VARCHAR(255) NOT NULL PRIMARY KEY, " +
                    "provider VARCHAR(255)," +
                    "amount BIGINT);";
            statement.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while creating database tables.", e);
        }
    }

    @Override
    public PlayerData getPlayer(final UUID uuid) {
        String sql = "SELECT wins, losses, profit, broadcasts FROM players WHERE uuid=?;";
        try (Connection playerConnection = getConnection();
             PreparedStatement preparedStatement = playerConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, uuid.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                PlayerData playerData = new PlayerData(uuid);
                playerData.setWins(resultSet.getInt("wins"));
                playerData.setLosses(resultSet.getInt("losses"));
                playerData.setProfit(resultSet.getLong("profit"));
                playerData.setDisplayBroadcastMessages(resultSet.getBoolean("broadcasts"));

                return playerData;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to get a player's data.", e);
            return null;
        }
        return new PlayerData(uuid);
    }

    @Override
    public void savePlayer(final PlayerData player) {
        String sql = "REPLACE INTO players (uuid, wins, losses, profit, broadcasts) VALUES (?, ?, ?, ?, ?)";
        try (Connection playerConnection = getConnection();
             PreparedStatement preparedStatement = playerConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, player.getUUID().toString());
            preparedStatement.setInt(2, player.getWins());
            preparedStatement.setInt(3, player.getLosses());
            preparedStatement.setLong(4, player.getProfit());
            preparedStatement.setBoolean(5, player.isDisplayBroadcastMessages());
            preparedStatement.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to save a player's data.", e);
        }
    }

    @Override
    public void saveCoinflip(CoinflipGame game) {
        String sql = "REPLACE INTO games (uuid, provider, amount) VALUES (?, ?, ?)";
        try (Connection coinflipConnection = getConnection();
             PreparedStatement preparedStatement = coinflipConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, game.getPlayerUUID().toString());
            preparedStatement.setString(2, game.getProvider());
            preparedStatement.setLong(3, game.getAmount());
            preparedStatement.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to save a coinflip game.", e);
        }
    }

    @Override
    public void deleteCoinfip(UUID uuid) {
        String sql = "DELETE FROM games WHERE uuid=?;";
        try (Connection coinflipConnection = getConnection();
             PreparedStatement preparedStatement = coinflipConnection.prepareStatement(sql)) {
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to delete a coinflip game.", e);
        }
    }

    @Override
    public void dropGamesTable() {
        String sql = "DROP TABLE IF EXISTS games;";
        try (Connection tableConnection = getConnection();
             PreparedStatement preparedStatement = tableConnection.prepareStatement(sql)) {
            preparedStatement.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to delete all coinflip games.", e);
        }
    }


    @Override
    public Map<UUID, CoinflipGame> getGames() {
        Map<UUID, CoinflipGame> games = new HashMap<>();
        String sql = "SELECT uuid, provider, amount FROM games;";
        try (Connection gamesConnection = getConnection();
             PreparedStatement preparedStatement = gamesConnection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                String provider = resultSet.getString("provider");
                long amount = resultSet.getLong("amount");
                games.put(uuid, new CoinflipGame(uuid, provider, amount));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to get all coinflip games.", e);
        }
        return games;
    }
}
