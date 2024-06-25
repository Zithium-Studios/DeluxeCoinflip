package net.zithium.deluxecoinflip.menu.inventories;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.api.events.CoinflipCompletedEvent;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.storage.StorageManager;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import net.zithium.deluxecoinflip.utility.TextUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Random;

public class CoinflipGUI implements Listener {

    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final FileConfiguration config;
    private final String coinflipGuiTitle;
    private final boolean taxEnabled;
    private final double taxRate;
    private final long minimumBroadcastWinnings;
    private static final int ANIMATION_COUNT_THRESHOLD = 12;

    public CoinflipGUI(@NotNull DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();

        // Load config values into variables this helps improve performance.
        this.coinflipGuiTitle = TextUtil.color(config.getString("coinflip-gui.title"));
        this.taxEnabled = config.getBoolean("settings.tax.enabled");
        this.taxRate = config.getDouble("settings.tax.rate");
        this.minimumBroadcastWinnings = config.getLong("settings.minimum-broadcast-winnings");
    }

    public void startGame(@NotNull Player player, @NotNull OfflinePlayer otherPlayer, CoinflipGame game) {

        Messages.PLAYER_CHALLENGE.send(otherPlayer.getPlayer(), "{OPPONENT}", player.getName());

        Random rand = new Random(System.currentTimeMillis());
        OfflinePlayer winner = rand.nextBoolean() ? player : otherPlayer;
        OfflinePlayer loser = winner.equals(player) ? otherPlayer : player;

        runAnimation(player, winner, loser, game);
    }

    private void runAnimation(Player player, OfflinePlayer winner, OfflinePlayer loser, CoinflipGame game) {

        Gui gui = Gui.gui().rows(3).title(Component.text(coinflipGuiTitle)).create();
        gui.disableAllInteractions();

        GuiItem winnerHead = new GuiItem(new ItemStackBuilder(
                winner.equals(game.getOfflinePlayer()) ? game.getCachedHead() : new ItemStack(Material.PLAYER_HEAD)
        ).withName(ChatColor.YELLOW + winner.getName()).setSkullOwner(winner).build());

        GuiItem loserHead = new GuiItem(new ItemStackBuilder(
                winner.equals(game.getOfflinePlayer()) ? new ItemStack(Material.PLAYER_HEAD) : game.getCachedHead()
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
            long beforeTax = winAmount / 2;

            @Override
            public void run() {
                count++;
                gui.getGuiItems().clear();

                if (count >= ANIMATION_COUNT_THRESHOLD) {
                    // Completed animation
                    gui.setItem(13, winnerHead);
                    gui.getFiller().fill(new GuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
                    gui.disableAllInteractions();
                    gui.update();

                    if (player.isOnline()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1L, 0L);
                    }

                    long taxed = 0;

                    if (taxEnabled) {
                        taxed = (long) ((taxRate * winAmount) / 100.0);
                        winAmount -= taxed;
                    }

                    // Deposit winnings synchronously
                    Bukkit.getScheduler().runTask(plugin, () -> economyManager.getEconomyProvider(game.getProvider()).deposit(winner, winAmount));

                    // Run event.
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new CoinflipCompletedEvent(winner, loser, winAmount)));


                    // Update player stats
                    StorageManager storageManager = plugin.getStorageManager();
                    updatePlayerStats(storageManager, winner, winAmount, beforeTax, true);
                    updatePlayerStats(storageManager, loser, 0, beforeTax, false);

                    String winAmountFormatted = TextUtil.numberFormat(winAmount);
                    String taxedFormatted = TextUtil.numberFormat(taxed);

                    // Send win/loss messages
                    if (winner.isOnline()) {
                        Messages.GAME_SUMMARY_WIN.send(winner.getPlayer(), replacePlaceholders(String.valueOf(taxRate), taxedFormatted, winner.getName(), loser.getName(), economyManager.getEconomyProvider(game.getProvider()).getDisplayName(), winAmountFormatted));
                    }
                    if (loser.isOnline()) {
                        Messages.GAME_SUMMARY_LOSS.send(loser.getPlayer(), replacePlaceholders(String.valueOf(taxRate), taxedFormatted, winner.getName(), loser.getName(), economyManager.getEconomyProvider(game.getProvider()).getDisplayName(), winAmountFormatted));
                    }
                    // Broadcast to the server
                    broadcastWinningMessage(winAmount, taxed, winner.getName(), loser.getName(), economyManager.getEconomyProvider(game.getProvider()).getDisplayName());

                    //closeAnimationGUI(gui);

                    cancel();
                }

                // Do animation
                if (alternate) {
                    ConfigurationSection animationSection = plugin.getConfig().getConfigurationSection("coinflip-gui.animation.1");
                    if (animationSection != null) {
                        ItemStack firstAnimationItem = ItemStackBuilder.getItemStack(animationSection).build();
                        gui.setItem(13, winnerHead);
                        gui.getFiller().fill(new GuiItem(firstAnimationItem));
                    } else {
                        gui.setItem(13, winnerHead);
                        gui.getFiller().fill(new GuiItem(Material.YELLOW_STAINED_GLASS_PANE));
                        plugin.getLogger().warning("Missing configuration section for first animation frame.");
                    }
                } else {
                    ConfigurationSection animationSection = plugin.getConfig().getConfigurationSection("coinflip-gui.animation.2");
                    if (animationSection != null) {
                        ItemStack secondAnimationItem = ItemStackBuilder.getItemStack(animationSection).build();
                        gui.setItem(13, loserHead);
                        gui.getFiller().fill(new GuiItem(secondAnimationItem));
                    } else {
                        gui.setItem(13, loserHead);
                        gui.getFiller().fill(new GuiItem(Material.GRAY_STAINED_GLASS_PANE));
                        plugin.getLogger().warning("Missing configuration section for second animation frame.");
                    }
                }

                alternate = !alternate;

                if (player.isOnline()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1L, 0L);
                }

                gui.update();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 10L);
    }

    private void updatePlayerStats(StorageManager storageManager, OfflinePlayer player, long winAmount, long beforeTax, boolean isWinner) {
        Optional<PlayerData> playerDataOptional = storageManager.getPlayer(player.getUniqueId());
        if (playerDataOptional.isPresent()) {
            PlayerData playerData = playerDataOptional.get();
            if (isWinner) {
                playerData.updateWins();
                playerData.updateProfit(winAmount);
                playerData.updateGambled(beforeTax);
            } else {
                playerData.updateLosses();
                playerData.updateLosses(beforeTax);
                playerData.updateGambled(beforeTax);
            }
        } else {
            if (isWinner) {
                storageManager.updateOfflinePlayerWin(player.getUniqueId(), winAmount, beforeTax);
            } else {
                storageManager.updateOfflinePlayerLoss(player.getUniqueId(), beforeTax);
            }
        }
    }

    private void broadcastWinningMessage(long winAmount, long tax, String winner, String loser, String currency) {
        if (winAmount >= minimumBroadcastWinnings) {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                plugin.getStorageManager().getPlayer(player.getUniqueId()).ifPresent(playerData -> {
                    if (playerData.isDisplayBroadcastMessages()) {
                        Messages.COINFLIP_BROADCAST.send(player, replacePlaceholders(
                                String.valueOf(taxRate),
                                TextUtil.numberFormat(tax),
                                winner,
                                loser,
                                currency,
                                TextUtil.numberFormat(winAmount)
                        ));
                    }
                });
            }
        }
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