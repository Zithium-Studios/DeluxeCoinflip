package fun.lewisdev.deluxecoinflip.menu.inventories;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import fun.lewisdev.deluxecoinflip.DeluxeCoinflipPlugin;
import fun.lewisdev.deluxecoinflip.config.ConfigType;
import fun.lewisdev.deluxecoinflip.config.Messages;
import fun.lewisdev.deluxecoinflip.api.events.CoinflipCompletedEvent;
import fun.lewisdev.deluxecoinflip.economy.EconomyManager;
import fun.lewisdev.deluxecoinflip.game.CoinflipGame;
import fun.lewisdev.deluxecoinflip.storage.PlayerData;
import fun.lewisdev.deluxecoinflip.storage.StorageManager;
import fun.lewisdev.deluxecoinflip.utility.ItemStackBuilder;
import fun.lewisdev.deluxecoinflip.utility.TextUtil;
import fun.lewisdev.deluxecoinflip.utility.universal.XMaterial;
import fun.lewisdev.deluxecoinflip.utility.universal.XSound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

public class CoinflipGUI implements Listener {

    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final FileConfiguration config;
    private final Random rand;

    public CoinflipGUI(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
        this.rand = new Random();
    }

    public void startGame(Player player, OfflinePlayer otherPlayer, CoinflipGame game) {
        if (!player.isOnline() || !otherPlayer.isOnline()) {
            // One of the players is not online, end the game.
            return;
        }

        Messages.PLAYER_CHALLENGE.send(otherPlayer.getPlayer(), "{OPPONENT}", player.getName());

        OfflinePlayer winner = rand.nextBoolean() ? player : otherPlayer;
        OfflinePlayer loser = winner.equals(player) ? otherPlayer : player;

        runAnimation(player, winner, loser, game);
    }

    private void runAnimation(Player player, OfflinePlayer winner, OfflinePlayer loser, CoinflipGame game) {
        @SuppressWarnings("deprecation") // Suppressing new Gui() deprecation error.
        Gui gui = new Gui(3, TextUtil.color(config.getString("coinflip-gui.title")));
        gui.disableAllInteractions();

        GuiItem winnerHead = new GuiItem(new ItemStackBuilder(
                winner.equals(game.getOfflinePlayer()) ? game.getCachedHead() : XMaterial.PLAYER_HEAD.parseItem()
        ).withName(ChatColor.YELLOW + winner.getName()).setSkullOwner(winner).build());

        GuiItem loserHead = new GuiItem(new ItemStackBuilder(
                winner.equals(game.getOfflinePlayer()) ? XMaterial.PLAYER_HEAD.parseItem() : game.getCachedHead()
        ).withName(ChatColor.YELLOW + loser.getName()).setSkullOwner(loser).build());

        if (winner.isOnline()) {
            gui.open(winner.getPlayer());
        }
        if (loser.isOnline()) {
            gui.open(loser.getPlayer());
        }

        new BukkitRunnable() {
            boolean alternate = false;
            int count = 0;
            long winAmount = game.getAmount() * 2;

            @Override
            public void run() {
                count++;
                gui.getGuiItems().clear();

                if (count >= 12) {
                    // Completed animation
                    gui.setItem(13, winnerHead);
                    gui.getFiller().fill(new GuiItem(XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem()));
                    gui.disableAllInteractions();
                    gui.update();

                    if (player.isOnline()) {
                        player.playSound(player.getLocation(), XSound.ENTITY_PLAYER_LEVELUP.parseSound(), 1L, 0L);
                    }

                    double taxRate = config.getDouble("settings.tax.rate");
                    long taxed = 0;

                    if (config.getBoolean("settings.tax.enabled")) {
                        taxed = (long) ((taxRate * winAmount) / 100.0);
                        winAmount -= taxed;
                    }

                    // Deposit winnings
                    Bukkit.getPluginManager().callEvent(new CoinflipCompletedEvent(winner, loser, winAmount));
                    economyManager.getEconomyProvider(game.getProvider()).deposit(winner, winAmount);

                    // Update player stats
                    StorageManager storageManager = plugin.getStorageManager();
                    updatePlayerStats(storageManager, winner, winAmount, true);
                    updatePlayerStats(storageManager, loser, 0, false);

                    String winAmountFormatted = TextUtil.numberFormat(winAmount);
                    String taxedFormatted = TextUtil.numberFormat(taxed);

                    // Send win/loss messages
                    sendGameSummaryMessage(winner, loser, taxRate, taxedFormatted, winAmountFormatted, true, game);
                    sendGameSummaryMessage(loser, winner, taxRate, taxedFormatted, winAmountFormatted, false, game);

                    // Broadcast to the server
                    broadcastWinningMessage(winAmount, winner.getName(), loser.getName(), economyManager.getEconomyProvider(game.getProvider()).getDisplayName());

                    closeAnimationGUI(gui);

                    cancel();
                }

                // Do animation
                if (alternate) {
                    gui.setItem(13, winnerHead);
                    gui.getFiller().fill(new GuiItem(XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem()));
                } else {
                    gui.setItem(13, loserHead);
                    gui.getFiller().fill(new GuiItem(XMaterial.GRAY_STAINED_GLASS_PANE.parseItem()));
                }

                alternate = !alternate;

                if (player.isOnline()) {
                    player.playSound(player.getLocation(), XSound.BLOCK_WOODEN_BUTTON_CLICK_ON.parseSound(), 1L, 0L);
                }

                gui.update();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 10L);
    }

    private void updatePlayerStats(StorageManager storageManager, OfflinePlayer player, long winAmount, boolean isWinner) {
        Optional<PlayerData> playerDataOptional = storageManager.getPlayer(player.getUniqueId());
        if (playerDataOptional.isPresent()) {
            PlayerData playerData = playerDataOptional.get();
            if (isWinner) {
                playerData.updateWins();
                playerData.updateProfit(winAmount);
            } else {
                playerData.updateLosses();
            }
        } else {
            if (isWinner) {
                storageManager.updateOfflinePlayerWin(player.getUniqueId(), winAmount);
            } else {
                storageManager.updateOfflinePlayerLoss(player.getUniqueId());
            }
        }
    }

    private void sendGameSummaryMessage(OfflinePlayer player, OfflinePlayer opponent, double taxRate, String taxedFormatted, String winAmountFormatted, boolean isWinner, CoinflipGame game) {
        if (player.isOnline()) {
            Messages message = isWinner ? Messages.GAME_SUMMARY_WIN : Messages.GAME_SUMMARY_LOSS;
            message.send(player.getPlayer(), replacePlaceholders(
                    String.valueOf(taxRate),
                    taxedFormatted,
                    player.getName(),
                    opponent.getName(),
                    economyManager.getEconomyProvider(game.getProvider()).getDisplayName(),
                    winAmountFormatted
            ));
        }
    }


    private void broadcastWinningMessage(long winAmount, String winner, String loser, String currency) {
        if (winAmount >= config.getLong("settings.minimum-broadcast-winnings")) {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                plugin.getStorageManager().getPlayer(player.getUniqueId()).ifPresent(playerData -> {
                    if (playerData.isDisplayBroadcastMessages()) {
                        Messages.COINFLIP_BROADCAST.send(player, replacePlaceholders(
                                String.valueOf(config.getDouble("settings.tax.rate")),
                                TextUtil.numberFormat(0),
                                winner, // Replace with the actual player name
                                loser, // Replace with the actual opponent name
                                currency, // Replace with the actual currency name
                                TextUtil.numberFormat(winAmount)
                        ));
                    }
                });
            }
        }
    }

    private void closeAnimationGUI(Gui gui) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (HumanEntity viewer : new ArrayList<>(gui.getInventory().getViewers())) {
                viewer.closeInventory();
            }
        }, 100L);
    }

    private Object[] replacePlaceholders(String taxRate, String taxDeduction, String winner, String loser, String currency, String winnings) {
        return new Object[]{"{TAX_RATE}", taxRate,
                "{TAX_DEDUCTION}", taxDeduction,
                "{WINNER}", winner,
                "{LOSER}", loser,
                "{CURRENCY}", currency,
                "{WINNINGS}", winnings};
    }
}
