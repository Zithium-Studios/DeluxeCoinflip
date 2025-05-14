/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.menu.inventories;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import net.kyori.adventure.text.Component;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.api.events.CoinflipCompletedEvent;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.GameAnimationRunner;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.storage.StorageManager;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import net.zithium.deluxecoinflip.utility.TextUtil;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CoinflipGUI implements Listener {

    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final FileConfiguration config;
    private final GameAnimationRunner gameAnimationRunner;
    private final String coinflipGuiTitle;
    private final boolean taxEnabled;
    private final double taxRate;
    private final long minimumBroadcastWinnings;
    private static final int ANIMATION_COUNT_THRESHOLD = 12;

    public CoinflipGUI(@NotNull DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
        this.gameAnimationRunner = new GameAnimationRunner(plugin);

        // Load config values into variables this helps improve performance.
        this.coinflipGuiTitle = ColorUtil.color(config.getString("coinflip-gui.title"));
        this.taxEnabled = config.getBoolean("settings.tax.enabled");
        this.taxRate = config.getDouble("settings.tax.rate");
        this.minimumBroadcastWinnings = config.getLong("settings.minimum-broadcast-winnings");
    }

    public void startGame(@NotNull Player creator, @NotNull OfflinePlayer opponent, CoinflipGame game) {
        // Send the challenge message BEFORE any swapping
        Messages.PLAYER_CHALLENGE.send(opponent.getPlayer(), "{OPPONENT}", creator.getName());

        // SecureRandom for better randomness
        SecureRandom random = new SecureRandom();
        random.setSeed(System.nanoTime() + creator.getUniqueId().hashCode() + opponent.getUniqueId().hashCode());

        // Randomly shuffle player order
        List<OfflinePlayer> players = new ArrayList<>(Arrays.asList(creator, opponent));
        Collections.shuffle(players, random);

        creator = (Player) players.get(0);
        opponent = players.get(1);

        OfflinePlayer winner = players.get(random.nextInt(players.size()));
        OfflinePlayer loser = (winner == creator) ? opponent : creator;

        Gui gameGui = Gui.gui().rows(3).title(Component.text(coinflipGuiTitle)).create();
        gameGui.disableAllInteractions();


        this.gameAnimationRunner.runAnimation(winner, loser, game, gameGui);
    }

    public void startAnimation(WrappedScheduler scheduler, Gui gui, GuiItem winnerHead, GuiItem loserHead,
                                OfflinePlayer winner, OfflinePlayer loser, CoinflipGame game,
                                Player targetPlayer, Location regionLoc, boolean isWinnerThread) {

        ConfigurationSection animationConfig1 = plugin.getConfig().getConfigurationSection("coinflip-gui.animation.1");
        ConfigurationSection animationConfig2 = plugin.getConfig().getConfigurationSection("coinflip-gui.animation.2");

        ItemStack firstAnimationItem = (animationConfig1 != null)
                ? ItemStackBuilder.getItemStack(animationConfig1).build()
                : new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);

        ItemStack secondAnimationItem = (animationConfig2 != null)
                ? ItemStackBuilder.getItemStack(animationConfig2).build()
                : new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        class AnimationState {
            boolean alternate = false;
            int count = 0;
        }

        AnimationState state = new AnimationState();
        long winAmount = game.getAmount() * 2;
        long beforeTax = winAmount / 2;

        Runnable[] task = new Runnable[1];
        task[0] = () -> {
            if (state.count++ >= ANIMATION_COUNT_THRESHOLD) {
                // Final state
                gui.setItem(13, winnerHead);
                gui.getFiller().fill(new GuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
                gui.disableAllInteractions();
                gui.update();

                if (targetPlayer.isOnline()) {
                    targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    scheduler.runTaskLaterAtEntity(targetPlayer, () -> {
                        if (targetPlayer.isOnline()) {
                            targetPlayer.closeInventory();
                        }
                    }, 20L);
                }

                long taxed = 0;
                long finalWinAmount = winAmount;
                if (taxEnabled) {
                    taxed = (long) ((taxRate * winAmount) / 100.0);
                    finalWinAmount -= taxed;
                }

                if (isWinnerThread) {
                    scheduler.runTask(() -> {
                        economyManager.getEconomyProvider(game.getProvider()).deposit(winner, winAmount);
                        Bukkit.getPluginManager().callEvent(new CoinflipCompletedEvent(winner, loser, winAmount));
                    });

                    if (config.getBoolean("discord.webhook.enabled", false) || config.getBoolean("discord.bot.enabled", false))
                        plugin.getDiscordHook().executeWebhook(winner, loser, economyManager.getEconomyProvider(game.getProvider()).getDisplayName(), winAmount).exceptionally(throwable -> {
                            plugin.getLogger().severe("An error occurred when triggering the webhook.");
                            throwable.printStackTrace();
                            return null;
                        });


                    // Update player stats
                    StorageManager storageManager = plugin.getStorageManager();
                    updatePlayerStats(storageManager, winner, finalWinAmount, beforeTax, true);
                    updatePlayerStats(storageManager, loser, 0, beforeTax, false);

                    // Send messages
                    String winAmountFormatted = TextUtil.numberFormat(finalWinAmount);
                    String taxedFormatted = TextUtil.numberFormat(taxed);

                    if (winner.isOnline()) {
                        Messages.GAME_SUMMARY_WIN.send(winner.getPlayer(), replacePlaceholders(
                                String.valueOf(taxRate), taxedFormatted, winner.getName(), loser.getName(),
                                economyManager.getEconomyProvider(game.getProvider()).getDisplayName(), winAmountFormatted
                        ));
                    }

                    if (loser.isOnline()) {
                        Messages.GAME_SUMMARY_LOSS.send(loser.getPlayer(), replacePlaceholders(
                                String.valueOf(taxRate), taxedFormatted, winner.getName(), loser.getName(),
                                economyManager.getEconomyProvider(game.getProvider()).getDisplayName(), winAmountFormatted
                        ));
                    }

                    // Broadcast results
                    broadcastWinningMessage(finalWinAmount, taxed, winner.getName(), loser.getName(),
                            economyManager.getEconomyProvider(game.getProvider()).getDisplayName());
                }

                return;
            }

            // Animation swapping
            gui.setItem(13, state.alternate ? winnerHead : loserHead);

            GuiItem filler = new GuiItem(state.alternate ? firstAnimationItem.clone() : secondAnimationItem.clone());

            for (int i = 0; i < gui.getInventory().getSize(); i++) {
                if (i != 13) gui.setItem(i, filler);
            }

            state.alternate = !state.alternate;

            if (targetPlayer.isOnline()) {
                targetPlayer.playSound(targetPlayer.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1f, 1f);
                if (targetPlayer.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
                    gui.update();
                }
            }

            scheduler.runTaskLaterAtLocation(regionLoc, task[0], 10L);
        };

        scheduler.runTaskAtLocation(regionLoc, task[0]);
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
