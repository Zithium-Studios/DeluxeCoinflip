/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

public class CoinflipGUI {

    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final Random rand;
    private final FileConfiguration config;

    public CoinflipGUI(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        rand = new Random();
        config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
    }

    public void startGame(Player player, OfflinePlayer otherPlayer, CoinflipGame game) {

        if (otherPlayer.isOnline()) {
            Messages.PLAYER_CHALLENGE.send(otherPlayer.getPlayer(), "{OPPONENT}", player.getName());
        }

        OfflinePlayer winner = rand.nextInt(2) == 0 ? player : otherPlayer;

        if (winner.getUniqueId().equals(player.getUniqueId()))
            runAnimation(player, player, otherPlayer, game);
        else
            runAnimation(player, otherPlayer, player, game);
    }

    private void runAnimation(Player player, OfflinePlayer winner, OfflinePlayer loser, CoinflipGame game) {

        Gui gui = new Gui(3, TextUtil.color(config.getString("coinflip-gui.title")));
        gui.disableAllInteractions();

        GuiItem winnerHead, loserHead;
        if (winner.equals(game.getOfflinePlayer())) {
            winnerHead = new GuiItem(new ItemStackBuilder(game.getCachedHead()).withName(ChatColor.YELLOW + winner.getName()).build());
            loserHead = new GuiItem(new ItemStackBuilder(XMaterial.PLAYER_HEAD.parseItem()).withName(ChatColor.YELLOW + loser.getName()).setSkullOwner(loser).build());
        } else {
            winnerHead = new GuiItem(new ItemStackBuilder(XMaterial.PLAYER_HEAD.parseItem()).withName(ChatColor.YELLOW + winner.getName()).setSkullOwner(winner).build());
            loserHead = new GuiItem(new ItemStackBuilder(game.getCachedHead()).withName(ChatColor.YELLOW + loser.getName()).build());
        }

        if (winner.isOnline()) gui.open(winner.getPlayer());
        if (loser.isOnline()) gui.open(loser.getPlayer());

        new BukkitRunnable() {
            boolean alternate = false;
            int count = 0;
            long winAmount = game.getAmount() * 2;

            @Override
            public void run() {
                count++;
                gui.getGuiItems().clear();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) gui.close(player);
                });

                if (count >= 12) {
                    // Completed animation
                    gui.setItem(13, winnerHead);
                    gui.getFiller().fill(new GuiItem(XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem()));
                    gui.update();

                    if (player.isOnline())
                        player.playSound(player.getLocation(), XSound.ENTITY_PLAYER_LEVELUP.parseSound(), 1L, 0L);

                    // Check for tax
                    double taxRate = config.getDouble("settings.tax.rate");
                    long taxed = 0;
                    if (config.getBoolean("settings.tax.enabled")) {
                        taxed = (long) ((taxRate * winAmount) / 100.0);
                        winAmount = winAmount - taxed;
                    }

                    // Deposit winnings
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.getPluginManager().callEvent(new CoinflipCompletedEvent(winner, loser, winAmount));
                        economyManager.getEconomyProvider(game.getProvider()).deposit(winner, winAmount);
                    });

                    // Update player stats
                    StorageManager storageManager = plugin.getStorageManager();
                    Optional<PlayerData> winnerPlayerDataOptional = storageManager.getPlayer(winner.getUniqueId());
                    if (winnerPlayerDataOptional.isPresent()) {
                        PlayerData winnerPlayerData = winnerPlayerDataOptional.get();
                        winnerPlayerData.updateWins();
                        winnerPlayerData.updateProfit(winAmount);
                    } else {
                        storageManager.updateOfflinePlayerWin(winner.getUniqueId(), winAmount);
                    }

                    Optional<PlayerData> loserPlayerDataOptional = storageManager.getPlayer(loser.getUniqueId());
                    if (loserPlayerDataOptional.isPresent()) {
                        PlayerData loserPlayerData = loserPlayerDataOptional.get();
                        loserPlayerData.updateLosses();
                    } else {
                        storageManager.updateOfflinePlayerLoss(winner.getUniqueId());
                    }

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
                    if (winAmount >= config.getLong("settings.minimum-broadcast-winnings")) {
                        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                            storageManager.getPlayer(player.getUniqueId()).ifPresent(playerData -> {
                                if (playerData.isDisplayBroadcastMessages()) {
                                    Messages.COINFLIP_BROADCAST.send(player, replacePlaceholders(String.valueOf(taxRate), taxedFormatted, winner.getName(), loser.getName(), economyManager.getEconomyProvider(game.getProvider()).getDisplayName(), winAmountFormatted));
                                }
                            });
                        }
                    }

                    // Close anyone that still has the animation GUI open after 100 ticks (5 seconds)
                    //Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    //    // We must clone the viewer list to prevent a ConcurrentModificationException
                    //    for (HumanEntity viewer : new ArrayList<>(gui.getInventory().getViewers()))
                    //    viewer.closeInventory();
                    //},100L);

                    cancel();
                    return;
                }

                // Do animation
                if (alternate) {
                    gui.setItem(13, winnerHead);
                    gui.getFiller().fill(new GuiItem(XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem()));
                    alternate = false;
                } else {
                    gui.setItem(13, loserHead);
                    gui.getFiller().fill(new GuiItem(XMaterial.GRAY_STAINED_GLASS_PANE.parseItem()));
                    alternate = true;
                }

                if (player.isOnline())
                    player.playSound(player.getLocation(), XSound.BLOCK_WOODEN_BUTTON_CLICK_ON.parseSound(), 1L, 0L);
                gui.update();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 10L);
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
