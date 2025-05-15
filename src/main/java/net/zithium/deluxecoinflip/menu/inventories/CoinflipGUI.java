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
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.storage.StorageManager;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import net.zithium.deluxecoinflip.utility.TextUtil;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
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
    private final String coinflipGuiTitle;
    private final boolean taxEnabled;
    private final double taxRate;
    private final long minimumBroadcastWinnings;
    private static final int ANIMATION_COUNT_THRESHOLD = 12;

    public CoinflipGUI(@NotNull DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();

        // Load config values into variables for performance
        this.coinflipGuiTitle = ColorUtil.color(config.getString("coinflip-gui.title"));
        this.taxEnabled = config.getBoolean("settings.tax.enabled");
        this.taxRate = config.getDouble("settings.tax.rate");
        this.minimumBroadcastWinnings = config.getLong("settings.minimum-broadcast-winnings");
    }

    public void startGame(@NotNull Player creator, @NotNull OfflinePlayer opponent, CoinflipGame game) {
        // Notify opponent
        Messages.PLAYER_CHALLENGE.send(opponent.getPlayer(), "{OPPONENT}", creator.getName());

        // SecureRandom seeded for fairness
        SecureRandom random = new SecureRandom();
        random.setSeed(System.nanoTime() + creator.getUniqueId().hashCode() + opponent.getUniqueId().hashCode());

        // Determine weighted chances based on permissions
        double creatorWeight = getChanceMultiplier(creator);
        double opponentWeight = getChanceMultiplier(opponent);
        double totalWeight = creatorWeight + opponentWeight;
        double roll = random.nextDouble() * totalWeight;

        OfflinePlayer winner = (roll < creatorWeight) ? creator : opponent;
        OfflinePlayer loser  = winner.equals(creator) ? opponent : creator;

        runAnimation(winner, loser, game);
    }

    /**
     * Reads any "coinflip.chance.<number>" permission and returns number/100. Default 1.0.
     */
    private double getChanceMultiplier(OfflinePlayer offline) {
        Player player = Bukkit.getPlayer(offline.getUniqueId());
        if (player == null) {
            return 1.0;
        }
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission();
            if (perm.startsWith("coinflip.chance.")) {
                String numPart = perm.substring("coinflip.chance.".length());
                try {
                    return Double.parseDouble(numPart) / 100.0;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 1.0;
    }

    private void runAnimation(OfflinePlayer winner, OfflinePlayer loser, CoinflipGame game) {
        WrappedScheduler scheduler = plugin.getScheduler();
        Gui gui = Gui.gui().rows(3).title(Component.text(coinflipGuiTitle)).create();
        gui.disableAllInteractions();

        GuiItem winnerHead = new GuiItem(new ItemStackBuilder(
                winner.equals(game.getOfflinePlayer()) ? game.getCachedHead() : new ItemStack(Material.PLAYER_HEAD)
        ).withName(ChatColor.YELLOW + winner.getName())
                .setSkullOwner(winner)
                .build());

        GuiItem loserHead = new GuiItem(new ItemStackBuilder(
                winner.equals(game.getOfflinePlayer()) ? new ItemStack(Material.PLAYER_HEAD) : game.getCachedHead()
        ).withName(ChatColor.YELLOW + loser.getName())
                .setSkullOwner(loser)
                .build());

        Player winnerPlayer = Bukkit.getPlayer(winner.getUniqueId());
        Player loserPlayer  = Bukkit.getPlayer(loser.getUniqueId());

        if (winnerPlayer != null) {
            scheduler.runTaskAtEntity(winnerPlayer, () -> {
                gui.open(winnerPlayer);
                startAnimation(scheduler, gui, winnerHead, loserHead, winner, loser, game, winnerPlayer, winnerPlayer.getLocation(), true);
            });
        }
        if (loserPlayer != null) {
            scheduler.runTaskAtEntity(loserPlayer, () -> {
                gui.open(loserPlayer);
                startAnimation(scheduler, gui, winnerHead, loserHead, winner, loser, game, loserPlayer, loserPlayer.getLocation(), false);
            });
        }
    }

    private void startAnimation(WrappedScheduler scheduler,
                                Gui gui,
                                GuiItem winnerHead,
                                GuiItem loserHead,
                                OfflinePlayer winner,
                                OfflinePlayer loser,
                                CoinflipGame game,
                                Player targetPlayer,
                                Location regionLoc,
                                boolean isWinnerThread) {

        ConfigurationSection animationConfig1 = plugin.getConfig().getConfigurationSection("coinflip-gui.animation.1");
        ConfigurationSection animationConfig2 = plugin.getConfig().getConfigurationSection("coinflip-gui.animation.2");

        ItemStack firstAnimationItem = (animationConfig1 != null)
                ? ItemStackBuilder.getItemStack(animationConfig1).build()
                : new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemStack secondAnimationItem = (animationConfig2 != null)
                ? ItemStackBuilder.getItemStack(animationConfig2).build()
                : new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        class AnimationState { boolean alternate = false; int count = 0; }
        AnimationState state = new AnimationState();
        long winAmount    = game.getAmount() * 2;
        long beforeTax    = winAmount / 2;

        Runnable[] task = new Runnable[1];
        task[0] = () -> {
            if (state.count++ >= ANIMATION_COUNT_THRESHOLD) {
                // Show winner head and fill
                gui.setItem(13, winnerHead);
                gui.getFiller().fill(new GuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
                gui.disableAllInteractions();
                gui.update();

                if (targetPlayer.isOnline()) {
                    targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    scheduler.runTaskLaterAtEntity(targetPlayer, () -> {
                        if (targetPlayer.isOnline()) targetPlayer.closeInventory();
                    }, 20L);
                }

                long taxed            = 0;
                long finalWinAmount   = winAmount;
                if (taxEnabled) {
                    taxed = (long) ((taxRate * winAmount) / 100.0);
                    finalWinAmount -= taxed;
                }

                if (isWinnerThread) {
                    // Deposit funds and fire event
                    scheduler.runTask(() -> {
                        economyManager.getEconomyProvider(game.getProvider()).deposit(winner, winAmount);
                        Bukkit.getPluginManager().callEvent(new CoinflipCompletedEvent(winner, loser, winAmount));
                    });

                    // Discord webhook if enabled
                    if (config.getBoolean("discord.webhook.enabled", false) || config.getBoolean("discord.bot.enabled", false)) {
                        plugin.getDiscordHook()
                                .executeWebhook(winner, loser, economyManager.getEconomyProvider(game.getProvider()).getDisplayName(), winAmount)
                                .exceptionally(throwable -> {
                                    plugin.getLogger().severe("An error occurred when triggering the webhook.");
                                    throwable.printStackTrace();
                                    return null;
                                });
                    }

                    // Update stats
                    StorageManager storageManager = plugin.getStorageManager();
                    updatePlayerStats(storageManager, winner, finalWinAmount, beforeTax, true);
                    updatePlayerStats(storageManager, loser, 0, beforeTax, false);

                    // Send summary messages
                    String winFormatted   = TextUtil.numberFormat(finalWinAmount);
                    String taxedFormatted = TextUtil.numberFormat(taxed);

                    if (winner.isOnline()) {
                        Messages.GAME_SUMMARY_WIN.send(winner.getPlayer(), replacePlaceholders(
                                String.valueOf(taxRate), taxedFormatted, winner.getName(), loser.getName(),
                                economyManager.getEconomyProvider(game.getProvider()).getDisplayName(), winFormatted
                        ));
                    }
                    if (loser.isOnline()) {
                        Messages.GAME_SUMMARY_LOSS.send(loser.getPlayer(), replacePlaceholders(
                                String.valueOf(taxRate), taxedFormatted, winner.getName(), loser.getName(),
                                economyManager.getEconomyProvider(game.getProvider()).getDisplayName(), winFormatted
                        ));
                    }

                    // Broadcast
                    broadcastWinningMessage(finalWinAmount, taxed, winner.getName(), loser.getName(), economyManager.getEconomyProvider(game.getProvider()).getDisplayName());
                }
                return;
            }

            // Swap animation frames
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
            scheduler.runTaskLaterAtEntity(targetPlayer, task[0], 10L);
        };
        scheduler.runTaskAtLocation(regionLoc, task[0]);
    }

    private void updatePlayerStats(StorageManager storageManager, OfflinePlayer player, long winAmount, long beforeTax, boolean isWinner) {
        Optional<PlayerData> optional = storageManager.getPlayer(player.getUniqueId());
        if (optional.isPresent()) {
            PlayerData data = optional.get();
            if (isWinner) {
                data.updateWins();
                data.updateProfit(winAmount);
                data.updateGambled(beforeTax);
            } else {
                data.updateLosses();
                data.updateLosses(beforeTax);
                data.updateGambled(beforeTax);
            }
        } else {
            if (isWinner) storageManager.updateOfflinePlayerWin(player.getUniqueId(), winAmount, beforeTax);
            else         storageManager.updateOfflinePlayerLoss(player.getUniqueId(), beforeTax);
        }
    }

    private void broadcastWinningMessage(long winAmount, long tax, String winner, String loser, String currency) {
        if (winAmount < minimumBroadcastWinnings) return;
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            plugin.getStorageManager().getPlayer(player.getUniqueId()).ifPresent(data -> {
                if (data.isDisplayBroadcastMessages()) {
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

    private Object[] replacePlaceholders(String taxRate, String taxDeduction, String winner, String loser, String currency, String winnings) {
        return new Object[]{
                "{TAX_RATE}",      taxRate,
                "{TAX_DEDUCTION}", taxDeduction,
                "{WINNER}",        winner,
                "{LOSER}",         loser,
                "{CURRENCY}",      currency,
                "{WINNINGS}",      winnings
        };
    }
}
