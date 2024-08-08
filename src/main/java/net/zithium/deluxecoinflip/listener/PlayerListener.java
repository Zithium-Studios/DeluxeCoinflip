package net.zithium.deluxecoinflip.listener;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;


public class PlayerListener implements Listener {
    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final GameManager gameManager;

    public PlayerListener(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.gameManager = plugin.getGameManager();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (gameManager.getCoinflipGames().containsKey(uuid)) {
            final CoinflipGame coinflipGame = gameManager.getCoinflipGames().get(uuid);
            economyManager.getEconomyProvider(coinflipGame.getProvider()).deposit(player, coinflipGame.getAmount());
            gameManager.removeCoinflipGame(uuid);
        }
    }
}
