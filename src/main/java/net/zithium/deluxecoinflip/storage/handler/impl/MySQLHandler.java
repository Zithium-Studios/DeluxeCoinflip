/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage.handler.impl;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.CoinflipHistory;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.storage.handler.StorageHandler;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class MySQLHandler implements StorageHandler {

    private static final String TABLE_PLAYERS = "players";
    private static final String TABLE_GAMES = "games";
    private static final String TABLE_HISTORY = "histories";

    private DeluxeCoinflipPlugin plugin;

    private String tablePrefix;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean useSsl;

    @Override
    public boolean onEnable(final DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;

        this.tablePrefix = plugin.getConfig().getString("storage.mysql.table-prefix", "coinflip_");
        this.host = plugin.getConfig().getString("storage.mysql.host", "127.0.0.1");
        this.port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        this.database = plugin.getConfig().getString("storage.mysql.database", "deluxecoinflip");
        this.username = plugin.getConfig().getString("storage.mysql.username", "root");
        this.password = plugin.getConfig().getString("storage.mysql.password", "");
        this.useSsl = plugin.getConfig().getBoolean("storage.mysql.ssl", false);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "MySQL JDBC driver not found.", e);
            return false;
        }

        try (Connection connection = this.getConnection()) {
            if (connection == null || connection.isClosed()) {
                plugin.getLogger().severe("Could not establish a MySQL connection.");
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to MySQL.", e);
            return false;
        }

        this.createTables();
        return true;
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Saving player data to database...");

        Map<UUID, PlayerData> playerDataMap = DeluxeCoinflipPlugin.getInstance().getStorageManager().getPlayerDataMap();

        final String sql = "INSERT INTO " + getTableName(TABLE_PLAYERS) + " (uuid, wins, losses, profit, total_loss, total_gambled, broadcasts) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "wins = VALUES(wins), " +
                "losses = VALUES(losses), " +
                "profit = VALUES(profit), " +
                "total_loss = VALUES(total_loss), " +
                "total_gambled = VALUES(total_gambled), " +
                "broadcasts = VALUES(broadcasts);";

        try (Connection connection = this.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                for (PlayerData player : new ArrayList<>(playerDataMap.values())) {
                    preparedStatement.setString(1, player.getUUID().toString());
                    preparedStatement.setInt(2, player.getWins());
                    preparedStatement.setInt(3, player.getLosses());
                    preparedStatement.setLong(4, player.getProfit());
                    preparedStatement.setLong(5, player.getTotalLosses());
                    preparedStatement.setLong(6, player.getTotalGambled());
                    preparedStatement.setBoolean(7, player.isDisplayBroadcastMessages());
                    preparedStatement.addBatch();
                }

                preparedStatement.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while saving player data on shutdown.", e);
        } finally {
            playerDataMap.clear();
        }
    }

    public Connection getConnection() {
        try {
            String url = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database
                    + "?useSSL=" + this.useSsl
                    + "&allowPublicKeyRetrieval=true"
                    + "&useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&serverTimezone=UTC"
                    + "&autoReconnect=true";

            return DriverManager.getConnection(url, this.username, this.password);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while setting up the MySQL connection.", e);
            throw new IllegalStateException("Unable to open MySQL connection", e);
        }
    }

    private void createTables() {
        try (Connection connection = this.getConnection();
             Statement statement = connection.createStatement()) {

            String createPlayersTable = "CREATE TABLE IF NOT EXISTS " + getTableName(TABLE_PLAYERS) + " (" +
                    "uuid VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "wins INT NOT NULL DEFAULT 0, " +
                    "losses INT NOT NULL DEFAULT 0, " +
                    "profit BIGINT NOT NULL DEFAULT 0, " +
                    "total_loss BIGINT NOT NULL DEFAULT 0, " +
                    "total_gambled BIGINT NOT NULL DEFAULT 0, " +
                    "broadcasts TINYINT(1) NOT NULL DEFAULT 1" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            statement.executeUpdate(createPlayersTable);

            String createGamesTable = "CREATE TABLE IF NOT EXISTS " + getTableName(TABLE_GAMES) + " (" +
                    "uuid VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "provider VARCHAR(255) NOT NULL, " +
                    "amount BIGINT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            statement.executeUpdate(createGamesTable);

            String createHistoryTable = "CREATE TABLE IF NOT EXISTS " + getTableName(TABLE_HISTORY) + " (" +
                    "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                    "creator_uuid VARCHAR(36) NOT NULL, " +
                    "opponent_uuid VARCHAR(36) NOT NULL, " +
                    "winner_uuid VARCHAR(36) NOT NULL, " +
                    "loser_uuid VARCHAR(36) NOT NULL, " +
                    "provider VARCHAR(255) NOT NULL, " +
                    "bet_amount BIGINT NOT NULL, " +
                    "winnings BIGINT NOT NULL, " +
                    "tax_deduction BIGINT NOT NULL, " +
                    "tax_rate DOUBLE NOT NULL, " +
                    "forfeit TINYINT(1) NOT NULL, " +
                    "created_at BIGINT NOT NULL, " +
                    "INDEX idx_coinflip_history_creator_uuid (creator_uuid), " +
                    "INDEX idx_coinflip_history_opponent_uuid (opponent_uuid), " +
                    "INDEX idx_coinflip_history_winner_uuid (winner_uuid), " +
                    "INDEX idx_coinflip_history_loser_uuid (loser_uuid), " +
                    "INDEX idx_coinflip_history_created_at (created_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            statement.executeUpdate(createHistoryTable);

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while creating MySQL tables.", e);
        }
    }

    @Override
    public PlayerData getPlayer(final UUID uuid) {
        final String sql = "SELECT wins, losses, profit, total_loss, total_gambled, broadcasts FROM " + getTableName(TABLE_PLAYERS) + " WHERE uuid = ?;";

        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, uuid.toString());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    PlayerData playerData = new PlayerData(uuid);
                    playerData.setWins(resultSet.getInt("wins"));
                    playerData.setLosses(resultSet.getInt("losses"));
                    playerData.setProfit(resultSet.getLong("profit"));
                    playerData.setTotalLosses(resultSet.getLong("total_loss"));
                    playerData.setTotalGambled(resultSet.getLong("total_gambled"));
                    playerData.setDisplayBroadcastMessages(resultSet.getBoolean("broadcasts"));
                    return playerData;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to get a player's data.", e);
        }

        return new PlayerData(uuid);
    }

    @Override
    public void savePlayer(final PlayerData player) {
        final String sql = "INSERT INTO " + getTableName(TABLE_PLAYERS) + " (uuid, wins, losses, profit, total_loss, total_gambled, broadcasts) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "wins = VALUES(wins), " +
                "losses = VALUES(losses), " +
                "profit = VALUES(profit), " +
                "total_loss = VALUES(total_loss), " +
                "total_gambled = VALUES(total_gambled), " +
                "broadcasts = VALUES(broadcasts);";

        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, player.getUUID().toString());
            preparedStatement.setInt(2, player.getWins());
            preparedStatement.setInt(3, player.getLosses());
            preparedStatement.setLong(4, player.getProfit());
            preparedStatement.setLong(5, player.getTotalLosses());
            preparedStatement.setLong(6, player.getTotalGambled());
            preparedStatement.setBoolean(7, player.isDisplayBroadcastMessages());
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to save a player's data.", e);
        }
    }

    @Override
    public void saveCoinflip(final CoinflipGame game) {
        final String sql = "INSERT INTO " + getTableName(TABLE_GAMES) + " (uuid, provider, amount) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE provider = VALUES(provider), amount = VALUES(amount);";

        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, game.getPlayerUUID().toString());
            preparedStatement.setString(2, game.getProvider());
            preparedStatement.setLong(3, game.getAmount());
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to save a coinflip game.", e);
        }
    }

    @Override
    public void deleteCoinflip(final UUID uuid) {
        final String sql = "DELETE FROM " + getTableName(TABLE_GAMES) + " WHERE uuid = ?;";

        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, uuid.toString());
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to delete a coinflip game.", e);
        }
    }

    @Override
    public Map<UUID, CoinflipGame> getGames() {
        Map<UUID, CoinflipGame> games = new HashMap<>();
        final String sql = "SELECT uuid, provider, amount FROM " + getTableName(TABLE_GAMES) + ";";

        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
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

    @Override
    public CoinflipGame getCoinflipGame(@NotNull final UUID uuid) {
        final String sql = "SELECT provider, amount FROM " + getTableName(TABLE_GAMES) + " WHERE uuid = ?;";

        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, uuid.toString());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                final String provider = resultSet.getString("provider");
                final long amount = resultSet.getLong("amount");
                return new CoinflipGame(uuid, provider, amount);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to get a coinflip game.", e);
            return null;
        }
    }

    @Override
    public void saveCoinflipHistory(final CoinflipHistory history) {
        final String sql = "INSERT INTO " + getTableName(TABLE_HISTORY) + " " +
                "(creator_uuid, opponent_uuid, winner_uuid, loser_uuid, provider, bet_amount, winnings, tax_deduction, tax_rate, forfeit, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, history.getCreatorUUID().toString());
            preparedStatement.setString(2, history.getOpponentUUID().toString());
            preparedStatement.setString(3, history.getWinnerUUID().toString());
            preparedStatement.setString(4, history.getLoserUUID().toString());
            preparedStatement.setString(5, history.getProvider());
            preparedStatement.setLong(6, history.getBetAmount());
            preparedStatement.setLong(7, history.getWinnings());
            preparedStatement.setLong(8, history.getTaxDeduction());
            preparedStatement.setDouble(9, history.getTaxRate());
            preparedStatement.setBoolean(10, history.isForfeit());
            preparedStatement.setLong(11, history.getCreatedAt());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to save coinflip history.", e);
        }
    }

    @Override
    public int getCoinflipHistoryCount(final UUID uuid) {
        final String sql = "SELECT COUNT(*) FROM " + getTableName(TABLE_HISTORY) + " " +
                "WHERE creator_uuid = ? OR opponent_uuid = ? OR winner_uuid = ? OR loser_uuid = ?;";

        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            String value = uuid.toString();
            preparedStatement.setString(1, value);
            preparedStatement.setString(2, value);
            preparedStatement.setString(3, value);
            preparedStatement.setString(4, value);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to count player coinflip history.", e);
        }

        return 0;
    }

    @Override
    public List<CoinflipHistory> getCoinflipHistory(final UUID uuid, final int offset, final int limit) {
        List<CoinflipHistory> history = new ArrayList<>();

        final String sql = "SELECT * FROM " + getTableName(TABLE_HISTORY) + " " +
                "WHERE creator_uuid = ? OR opponent_uuid = ? OR winner_uuid = ? OR loser_uuid = ? " +
                "ORDER BY created_at DESC " +
                "LIMIT ? OFFSET ?;";

        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            String value = uuid.toString();
            preparedStatement.setString(1, value);
            preparedStatement.setString(2, value);
            preparedStatement.setString(3, value);
            preparedStatement.setString(4, value);
            preparedStatement.setInt(5, Math.max(limit, 1));
            preparedStatement.setInt(6, Math.max(offset, 0));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    history.add(this.mapHistory(resultSet));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to get player coinflip history.", e);
        }

        return history;
    }

    @Override
    public List<CoinflipHistory> getRecentCoinflipHistory(final int limit) {
        List<CoinflipHistory> history = new ArrayList<>();
        final String sql = "SELECT * FROM " + getTableName(getTableName(TABLE_HISTORY)) + " ORDER BY created_at DESC LIMIT ?;";

        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, Math.max(limit, 1));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    history.add(this.mapHistory(resultSet));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while attempting to get recent coinflip history.", e);
        }

        return history;
    }

    private CoinflipHistory mapHistory(final ResultSet resultSet) throws SQLException {
        return new CoinflipHistory(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("creator_uuid")),
                UUID.fromString(resultSet.getString("opponent_uuid")),
                UUID.fromString(resultSet.getString("winner_uuid")),
                UUID.fromString(resultSet.getString("loser_uuid")),
                resultSet.getString("provider"),
                resultSet.getLong("bet_amount"),
                resultSet.getLong("winnings"),
                resultSet.getLong("tax_deduction"),
                resultSet.getDouble("tax_rate"),
                resultSet.getBoolean("forfeit"),
                resultSet.getLong("created_at")
        );
    }

    private String getTableName(String base) {
        return this.tablePrefix + base;
    }
}