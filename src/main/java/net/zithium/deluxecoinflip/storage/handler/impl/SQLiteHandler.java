/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
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

public class SQLiteHandler implements StorageHandler {

    private File file;
    private Connection connection;

    @Override
    public boolean onEnable(final DeluxeCoinflipPlugin plugin) {
        file = new File(plugin.getDataFolder(), "database.db");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        getConnection();
        createTable();
        return true;
    }

    @Override
    public void onDisable() {
        try {
            if (!connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            if (connection != null && !connection.isClosed()) return connection;
            else {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                return connection;
            }
        } catch (SQLException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return connection;
    }

    private void createTable() {
        try {
            Connection connection = getConnection();
            String sql = "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(255) NOT NULL PRIMARY KEY, " +
                    "wins INTEGER, " +
                    "losses INTEGER, " +
                    "profit BIGINT," +
                    "broadcasts BOOLEAN," +
                    "active_game BOOLEAN" + ");";
            Statement statement = connection.createStatement();
            statement.execute(sql);

            sql = "CREATE TABLE IF NOT EXISTS games (" +
                    "uuid VARCHAR(255) NOT NULL PRIMARY KEY, " +
                    "provider VARCHAR(255)," +
                    "amount BIGINT);";
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PlayerData getPlayer(final UUID uuid) {
        try {
            Connection connection = getConnection();
            String sql = "SELECT * FROM players WHERE uuid ='" + uuid + "';";
            ResultSet resultSet = connection.createStatement().executeQuery(sql);
            if (resultSet.next()) {
                PlayerData playerData = new PlayerData(uuid);
                playerData.setWins(resultSet.getInt("wins"));
                playerData.setLosses(resultSet.getInt("losses"));
                playerData.setProfit(resultSet.getLong("profit"));
                playerData.setDisplayBroadcastMessages(resultSet.getBoolean("broadcasts"));
                playerData.setHasActiveGame(resultSet.getBoolean("active_game"));

                return playerData;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return new PlayerData(uuid);
    }

    @Override
    public void savePlayer(final PlayerData player) {
        try {
            Connection connection = getConnection();
            String sql = "REPLACE INTO 'players' (uuid, wins, losses, profit, broadcasts, active_game) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, player.getUUID().toString());
            pstmt.setInt(2, player.getWins());
            pstmt.setInt(3, player.getLosses());
            pstmt.setLong(4, player.getProfit());
            pstmt.setBoolean(5, player.isDisplayBroadcastMessages());
            pstmt.setBoolean(6, player.hasActiveGame());
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveCoinflip(CoinflipGame game) {
        try {
            Connection connection = getConnection();
            String sql = "REPLACE INTO 'games' (uuid, provider, amount) VALUES (?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, game.getPlayerUUID().toString());
            pstmt.setString(2, game.getProvider());
            pstmt.setLong(3, game.getAmount());
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteCoinfip(UUID uuid) {
        try {
            Connection connection = getConnection();
            String sql = "DELETE FROM 'games' WHERE uuid='" + uuid.toString() + "';";
            Statement statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<UUID, CoinflipGame> getGames() {
        Map<UUID, CoinflipGame> games = new HashMap<>();
        try {
            Connection connection = getConnection();
            String sql = "SELECT * FROM games;";
            ResultSet resultSet = connection.createStatement().executeQuery(sql);
            if (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                String provider = resultSet.getString("provider");
                long amount = resultSet.getLong("amount");
                games.put(uuid, new CoinflipGame(uuid, provider, amount));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return games;
    }
}
