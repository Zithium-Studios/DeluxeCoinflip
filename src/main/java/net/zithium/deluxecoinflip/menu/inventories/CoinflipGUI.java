/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.menu.inventories;

import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.api.events.CoinflipCompletedEvent;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.CoinflipHistory;
import net.zithium.deluxecoinflip.game.GameAnimationRunner;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.storage.StorageManager;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import net.zithium.deluxecoinflip.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

public class CoinflipGUI {

    private final DeluxeCoinflipPlugin plugin;
    private final PlatformScheduler scheduler;
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
        this.scheduler = DeluxeCoinflipPlugin.scheduler();
        this.economyManager = plugin.getEconomyManager();
        this.config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
        this.gameAnimationRunner = new GameAnimationRunner(plugin);
        this.coinflipGuiTitle = TextUtil.color(config.getString("coinflip-gui.title", "&lFLIPPING COIN..."));
        this.taxEnabled = config.getBoolean("settings.tax.enabled");
        this.taxRate = config.getDouble("settings.tax.rate");
        this.minimumBroadcastWinnings = config.getLong("settings.minimum-broadcast-winnings");
    }

    public void startGame(@NotNull Player creator, @NotNull Player opponent, CoinflipGame game) {
        if (opponent.isOnline()) {
            Messages.PLAYER_CHALLENGE.send(opponent, "{OPPONENT}", creator.getName());
        }

        game.attachOpponent(opponent.getUniqueId());
        game.setActiveGame(true);
        DeluxeCoinflipPlugin.getInstance().getActiveGamesCache().register(game);

        SecureRandom random = new SecureRandom();
        random.setSeed(System.nanoTime() + creator.getUniqueId().hashCode() + opponent.getUniqueId().hashCode());

        List<Player> players = new ArrayList<>(Arrays.asList(creator, opponent));
        Collections.shuffle(players, random);

        creator = players.get(0);
        opponent = players.get(1);

        OfflinePlayer winner = Bukkit.getOfflinePlayer(players.get(random.nextInt(players.size())).getUniqueId());
        OfflinePlayer loser = Bukkit.getOfflinePlayer(winner.getUniqueId().equals(creator.getUniqueId())
                ? opponent.getUniqueId() : creator.getUniqueId());

        Gui winnerGui = createGameGui();
        Gui loserGui = createGameGui();

        this.gameAnimationRunner.runAnimation(winner, loser, game, winnerGui, loserGui, random);
    }

    private Gui createGameGui() {
        Gui gui = Gui.gui().rows(3).title(Component.text(coinflipGuiTitle)).create();
        gui.disableAllInteractions();
        return gui;
    }

    public void startAnimation(Gui gui, GuiItem winnerHead, GuiItem loserHead,
                               OfflinePlayer winner, OfflinePlayer loser, CoinflipGame game,
                               Player targetPlayer, SecureRandom random, boolean isWinnerThread) {

        List<ItemStack> animationItems = new ArrayList<>();
        FileConfiguration cfg = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
        ConfigurationSection animationSection = cfg.getConfigurationSection("coinflip-gui.animation");
        if (animationSection != null) {
            for (String key : animationSection.getKeys(false)) {
                ConfigurationSection animationConfig = animationSection.getConfigurationSection(key);
                if (animationConfig != null) {
                    ItemStack item = ItemStackBuilder.getItemStack(animationConfig).build();
                    animationItems.add(item);
                }
            }
        }

        if (animationItems.isEmpty()) {
            animationItems.add(new ItemStack(Material.YELLOW_STAINED_GLASS_PANE));
            animationItems.add(new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        final UUID player1Id = game.getPlayerUUID();
        final UUID player2Id = game.getOpponentUUID();
        final AtomicBoolean finished = new AtomicBoolean(false);

        if (player2Id == null) {
            plugin.getLogger().warning("Attempted to start animation with null opponent UUID");
            return;
        }

        class AnimationState {
            boolean headRandomization = random.nextBoolean();
            int glassIndex = random.nextInt(animationItems.size());
            int count = 0;
        }

        AnimationState state = new AnimationState();

        long winAmount = game.getAmount() * 2L;
        long beforeTax = winAmount / 2L;

        class AnimationLoop implements Consumer<WrappedTask> {
            @Override
            public void accept(WrappedTask task) {
                if (finished.get()) {
                    return;
                }

                Player player1 = Bukkit.getPlayer(player1Id);
                Player player2 = Bukkit.getPlayer(player2Id);

                if (player1 == null || player2 == null) {
                    if (isWinnerThread) {
                        finishGame(finished, game, player1Id, player2Id, winAmount, beforeTax, true);
                    }

                    return;
                }

                if (state.count++ >= ANIMATION_COUNT_THRESHOLD) {
                    gui.setItem(13, winnerHead);
                    gui.getFiller().fill(new GuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
                    gui.disableAllInteractions();
                    gui.update();

                    if (targetPlayer.isOnline()) {
                        playConfiguredSound(targetPlayer, "coinflip-gui.sounds.animation_complete", Sound.ENTITY_PLAYER_LEVELUP);
                        if (targetPlayer.isOnline() && targetPlayer.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
                            scheduler.runAtEntityLater(targetPlayer, innerTask -> targetPlayer.closeInventory(), 20L);
                        }
                    }

                    if (isWinnerThread) {
                        finishGame(finished, game, winner.getUniqueId(), loser.getUniqueId(), winAmount, beforeTax, false);
                    }

                    return;
                }

                gui.setItem(13, state.headRandomization ? winnerHead : loserHead);

                ItemStack currentItem = animationItems.get(state.glassIndex).clone();
                GuiItem filler = new GuiItem(currentItem);
                for (int i = 0; i < gui.getInventory().getSize(); i++) {
                    if (i != 13) gui.setItem(i, filler);
                }

                state.headRandomization = !state.headRandomization;
                state.glassIndex = (state.glassIndex + 1) % animationItems.size();

                if (targetPlayer.isOnline() && targetPlayer.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
                    playConfiguredSound(targetPlayer, "coinflip-gui.sounds.animation_tick", Sound.BLOCK_WOODEN_BUTTON_CLICK_ON);
                    gui.update();
                }

                scheduler.runAtEntityLater(targetPlayer, this, 10L);
            }
        }

        scheduler.runAtEntity(targetPlayer, new AnimationLoop());
    }

    private void finishGame(AtomicBoolean finished, CoinflipGame game,
                            UUID winnerId, UUID loserId,
                            long winAmount, long beforeTax, boolean forfeit) {

        if (!finished.compareAndSet(false, true)) {
            return;
        }

        final UUID opponentId = game.getOpponentUUID();
        if (opponentId == null) {
            plugin.getGameManager().removeCoinflipGame(game.getPlayerUUID());
            plugin.getActiveGamesCache().unregister(game);
            return;
        }

        if (forfeit) {
            Player player1 = Bukkit.getPlayer(game.getPlayerUUID());
            Player player2 = Bukkit.getPlayer(opponentId);

            if (player1 != null) {
                winnerId = game.getPlayerUUID();
                loserId = opponentId;
            } else if (player2 != null) {
                winnerId = opponentId;
                loserId = game.getPlayerUUID();
            } else {
                plugin.getGameManager().removeCoinflipGame(game.getPlayerUUID());
                plugin.getActiveGamesCache().unregister(game);
                return;
            }
        }

        OfflinePlayer winner = Bukkit.getOfflinePlayer(winnerId);
        OfflinePlayer loser = Bukkit.getOfflinePlayer(loserId);

        EconomyProvider provider = economyManager.getEconomyProvider(game.getProvider());
        if (provider == null) {
            plugin.getLogger().warning("Missing economy provider '" + game.getProvider() + "'; payout skipped.");
            plugin.getGameManager().removeCoinflipGame(game.getPlayerUUID());
            plugin.getActiveGamesCache().unregister(game);
            return;
        }

        long taxed = 0L;
        long finalWinAmount = winAmount;
        if (taxEnabled) {
            taxed = (long) ((taxRate * winAmount) / 100.0);
            finalWinAmount -= taxed;
        }

        final long providedWinAmount = finalWinAmount;
        final UUID finalWinnerId = winnerId;
        final UUID finalLoserId = loserId;

        scheduler.runNextTick(innerTask -> {
            provider.deposit(winner, providedWinAmount);
            new CoinflipCompletedEvent(finalWinnerId, finalLoserId, winner, loser, providedWinAmount, forfeit).callEvent();
            plugin.getGameManager().removeCoinflipGame(game.getPlayerUUID());
            plugin.getActiveGamesCache().unregister(game);
        });

        StorageManager storageManager = plugin.getStorageManager();
        updatePlayerStats(storageManager, winner, finalWinAmount, beforeTax, true);
        updatePlayerStats(storageManager, loser, 0L, beforeTax, false);

        /*
         * Save completed game into persistent history after the final
         * winner/loser, tax and winnings have been resolved.
         */
        CoinflipHistory history = new CoinflipHistory(
                0L,
                game.getPlayerUUID(),
                opponentId,
                winnerId,
                loserId,
                game.getProvider(),
                game.getAmount(),
                finalWinAmount,
                taxed,
                taxEnabled ? taxRate : 0.0D,
                forfeit,
                System.currentTimeMillis()
        );
        storageManager.getStorageHandler().saveCoinflipHistory(history);

        String winAmountFormatted = TextUtil.numberFormat(finalWinAmount);
        String taxedFormatted = TextUtil.numberFormat(taxed);
        String providerName = provider.getDisplayName();

        Player winnerOnline = winner.getPlayer();
        Player loserOnline = loser.getPlayer();

        if (forfeit) {
            String loserName = loser.getName() != null ? loser.getName() : "Unknown";

            if (winnerOnline != null) {
                scheduler.runAtEntity(winnerOnline, innerTask -> winnerOnline.closeInventory());
                Messages.GAME_FORFEIT.send(winnerOnline, replacePlaceholders(
                        String.valueOf(taxRate), taxedFormatted, winner.getName(), loserName,
                        providerName, winAmountFormatted));
            }

            if (loserOnline != null) {
                scheduler.runAtEntity(loserOnline, innerTask -> loserOnline.closeInventory());
            }
        } else {
            if (winnerOnline != null) {
                Messages.GAME_SUMMARY_WIN.send(winnerOnline, replacePlaceholders(
                        String.valueOf(taxRate), taxedFormatted, winner.getName(), loser.getName(),
                        providerName, winAmountFormatted));
            }

            if (loserOnline != null) {
                Messages.GAME_SUMMARY_LOSS.send(loserOnline, replacePlaceholders(
                        String.valueOf(taxRate), taxedFormatted, winner.getName(), loser.getName(),
                        providerName, winAmountFormatted));
            }

            broadcastWinningMessage(finalWinAmount, taxed, winner.getName(), loser.getName(), providerName);
        }

        final boolean webhookEnabled = config.getBoolean("discord.webhook.enabled", false)
                || config.getBoolean("discord.bot.enabled", false);

        if (webhookEnabled) {
            plugin.getDiscordHook().executeWebhook(winner, loser, providerName, winAmount)
                    .exceptionally(ex -> {
                        plugin.getLogger().log(Level.SEVERE, "Discord webhook error", ex);
                        return null;
                    });
        }
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
        return new Object[]{
                "{TAX_RATE}", taxRate,
                "{TAX_DEDUCTION}", taxDeduction,
                "{WINNER}", winner,
                "{LOSER}", loser,
                "{CURRENCY}", currency,
                "{WINNINGS}", winnings
        };
    }

    private void playConfiguredSound(Player player, String path, Sound def) {
        FileConfiguration cfg = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
        ConfigurationSection section = cfg.getConfigurationSection(path);

        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }

        String name = section.getString("name", def.name());
        Sound chosen;
        try {
            chosen = Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            chosen = def;
        }

        float vol = (float) section.getDouble("volume", (float) 1.0);
        float pitch = (float) section.getDouble("pitch", (float) 1.0);
        player.playSound(player.getLocation(), chosen, vol, pitch);
    }
}
