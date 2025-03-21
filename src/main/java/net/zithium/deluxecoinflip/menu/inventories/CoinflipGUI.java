package net.zithium.deluxecoinflip.menu.inventories;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
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
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.*;

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

        runAnimation(creator, winner, loser, game);
    }


    private void runAnimation(Player player, OfflinePlayer winner, OfflinePlayer loser, CoinflipGame game) {
        final WrappedScheduler scheduler = plugin.getScheduler();
        Gui gui = Gui.gui().rows(3).title(Component.text(coinflipGuiTitle)).create();
        gui.disableAllInteractions();

        GuiItem winnerHead = new GuiItem(new ItemStackBuilder(
                winner.equals(game.getOfflinePlayer()) ? game.getCachedHead() : new ItemStack(Material.PLAYER_HEAD)
        ).withName(ChatColor.YELLOW + winner.getName()).setSkullOwner(winner).build());

        GuiItem loserHead = new GuiItem(new ItemStackBuilder(
                winner.equals(game.getOfflinePlayer()) ? new ItemStack(Material.PLAYER_HEAD) : game.getCachedHead()
        ).withName(ChatColor.YELLOW + loser.getName()).setSkullOwner(loser).build());

        Location fallbackLocation = Bukkit.getWorlds().get(0).getSpawnLocation();

        Player winnerPlayer = Bukkit.getPlayer(winner.getUniqueId());
        Player loserPlayer = Bukkit.getPlayer(loser.getUniqueId());

        if (winnerPlayer != null) {
            scheduler.runTaskAtLocation(winnerPlayer.getLocation(), () -> gui.open(winnerPlayer));
        }

        if (loserPlayer != null) {
            Location taskLocation = (winnerPlayer != null) ? winnerPlayer.getLocation() : fallbackLocation;
            scheduler.runTaskAtLocation(taskLocation, () -> gui.open(loserPlayer));
        }

        ConfigurationSection animationConfig1 = plugin.getConfig().getConfigurationSection("coinflip-gui.animation.1.");
        ConfigurationSection animationConfig2 = plugin.getConfig().getConfigurationSection("coinflip-gui.animation.2.");

        ItemStack firstAnimationItem = (animationConfig1 != null)
                ? ItemStackBuilder.getItemStack(animationConfig1).build()
                : new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);

        ItemStack secondAnimationItem = (animationConfig2 != null)
                ? ItemStackBuilder.getItemStack(animationConfig2).build()
                : new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        new WrappedRunnable() {
            boolean alternate = false;
            int count = 0;
            long winAmount = game.getAmount() * 2;
            long beforeTax = winAmount / 2;

            @Override
            public void run() {
                count++;
                if (count >= ANIMATION_COUNT_THRESHOLD) {
                    // Final state
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
                    updatePlayerStats(storageManager, winner, winAmount, beforeTax, true);
                    updatePlayerStats(storageManager, loser, 0, beforeTax, false);

                    // Send messages
                    String winAmountFormatted = TextUtil.numberFormat(winAmount);
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
                    broadcastWinningMessage(winAmount, taxed, winner.getName(), loser.getName(),
                            economyManager.getEconomyProvider(game.getProvider()).getDisplayName());

                    cancel();
                    return;
                }

                // Animation swapping
                gui.setItem(13, alternate ? winnerHead : loserHead);
                gui.getFiller().fill(new GuiItem(alternate ? firstAnimationItem : secondAnimationItem));
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