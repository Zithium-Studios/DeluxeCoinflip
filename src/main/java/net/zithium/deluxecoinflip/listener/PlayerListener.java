package net.zithium.deluxecoinflip.listener;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;

import java.util.UUID;
import java.util.logging.Level;

public class PlayerListener implements Listener {
    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final GameManager gameManager;

    public PlayerListener(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.gameManager = plugin.getGameManager();
        this.storageManager = plugin.getStorageManager();
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer() == null) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (gameManager.getCoinflipGames().containsKey(uuid)) {
            final CoinflipGame coinflipGame = gameManager.getCoinflipGames().get(uuid);
            economyManager.getEconomyProvider(coinflipGame.getProvider()).deposit(player, coinflipGame.getAmount());
            gameManager.removeCoinflipGame(uuid);
        }

    }


    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            plugin.getLogger().log(Level.INFO, "Starting cleanup event.");
            plugin.clearGames();
            plugin.getLogger().log(Level.INFO, "Cleanup completed.");
        }

    }
}
