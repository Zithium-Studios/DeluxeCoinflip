/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.menu.inventories;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.GameManager;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import net.zithium.deluxecoinflip.utility.TextUtil;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;

public class GamesGUI {

    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final FileConfiguration config;

    private final ItemStackBuilder materialBuilder;
    private final String GUI_TITLE;
    private final int GUI_ROWS;

    public GamesGUI(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();

        GUI_TITLE = TextUtil.color(config.getString("games-gui.title"));
        GUI_ROWS = config.getInt("games-gui.rows");

        if (config.contains("games-gui.coinflip-game.material") && !config.getString("games-gui.coinflip-game.material").equalsIgnoreCase("PLAYER_HEAD")) {
            materialBuilder = new ItemStackBuilder(Material.matchMaterial(config.getString("games-gui.coinflip-game.material")));
        } else {
            materialBuilder = null; // Set it to null if not configured
        }
    }

    public void openInventory(Player player) {
        // Fetch player data
        Optional<PlayerData> optionalPlayerData = plugin.getStorageManager().getPlayer(player.getUniqueId());
        if (optionalPlayerData.isEmpty()) {
            player.sendMessage(TextUtil.color("&cYour player data was not found, please relog or contact an administrator if the issue persists."));
            return;
        }

        PlayerData playerData = optionalPlayerData.get();

        // Create the inventory
        PaginatedGui gui = Gui.paginated().rows(GUI_ROWS).title(Component.text(GUI_TITLE)).create();
        gui.setDefaultClickAction(event -> event.setCancelled(true));

        loadFillerItems(gui);

        // Navigation items
        gui.setItem(config.getInt("games-gui.previous-page.slot"), new GuiItem(ItemStackBuilder.getItemStack(config.getConfigurationSection("games-gui.previous-page")).build(), event -> gui.previous()));
        gui.setItem(config.getInt("games-gui.next-page.slot"), new GuiItem(ItemStackBuilder.getItemStack(config.getConfigurationSection("games-gui.next-page")).build(), event -> gui.next()));

        gui.setDefaultClickAction(event -> event.setCancelled(true));

        // Update filler items to include placeholders
        for (Map.Entry<Integer, GuiItem> entry : gui.getGuiItems().entrySet()) {
            int slot = entry.getKey();
            GuiItem guiItem = entry.getValue();
            if (!guiItem.getItemStack().hasItemMeta()) continue;

            final ItemStack originalItem = gui.getGuiItem(slot).getItemStack().clone();
            if (!originalItem.hasItemMeta()) continue;
            ItemMeta meta = originalItem.getItemMeta().clone();

            ItemStackBuilder newItemBuilder = new ItemStackBuilder(originalItem);
            if (meta.hasDisplayName()) {
                newItemBuilder.withName(meta.getDisplayName().replace("{PLAYER}", player.getName()));
            }
            if (meta.hasLore()) {
                newItemBuilder.withLore(meta.getLore().stream().map(line -> line
                                .replace("{WINS}", String.valueOf(playerData.getWins()))
                                .replace("{LOSSES}", String.valueOf(playerData.getLosses()))
                                .replace("{PROFIT}", String.valueOf(playerData.getProfitFormatted()))
                                .replace("{WIN_PERCENTAGE}", String.valueOf(playerData.getWinPercentage()))
                                .replace("{TOTAL_LOSSES}", String.valueOf(playerData.getTotalLossesFormatted()))
                                .replace("{TOTAL_GAMBLED}", String.valueOf(playerData.getTotalGambledFormatted()))
                                .replace("{PLAYER}", player.getName()))
                        .toList());
            }

            ItemStack newItem = newItemBuilder.build();
            guiItem.setItemStack(newItem);
            gui.getInventory().setItem(slot, guiItem.getItemStack());
        }

        // Open Game Builder GUI
        if (config.getBoolean("games-gui.create-new-game.enabled")) {
            gui.setItem(config.getInt("games-gui.create-new-game.slot"), new GuiItem(ItemStackBuilder.getItemStack(config.getConfigurationSection("games-gui.create-new-game")).build(),
                    event -> plugin.getInventoryManager().getGameBuilderGUI().openGameBuilderGUI(player, new CoinflipGame(player.getUniqueId(), economyManager.getEconomyProviders().entrySet().stream().findFirst().get().getKey(), 0))));
        }

        double taxRate = config.getDouble("settings.tax.rate");

        // Check if there is any games available
        GameManager gameManager = plugin.getGameManager();
        if (gameManager.getCoinflipGames().isEmpty()) {
            ItemStack noGames = ItemStackBuilder.getItemStack(config.getConfigurationSection("games-gui.no-games")).build();
            gui.setItem(config.getInt("games-gui.no-games.slot"), new GuiItem(noGames));
        }

        // Otherwise, list available games
        else {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            for (Map.Entry<UUID, CoinflipGame> entry : gameManager.getCoinflipGames().entrySet()) {
                CoinflipGame coinflipGame = entry.getValue();
                if (economyManager.getEconomyProvider(coinflipGame.getProvider()) == null) continue;

                long taxed = 0;
                if (config.getBoolean("settings.tax.enabled"))
                    taxed = (long) ((taxRate * coinflipGame.getAmount()) / 100.0);

                String valueFormatted = numberFormat.format(coinflipGame.getAmount());
                String taxedFormatted = numberFormat.format(taxed);

                OfflinePlayer playerFromID = coinflipGame.getOfflinePlayer();
                if (playerFromID == null) continue;

                ItemStackBuilder builder;
                if (materialBuilder != null) {
                    // Create a new instance with the same properties as materialBuilder
                    builder = new ItemStackBuilder(materialBuilder.build());
                } else {
                    builder = new ItemStackBuilder(coinflipGame.getCachedHead());
                }

                if (config.contains("games-gui.coinflip-game.material") && !config.getString("games-gui.coinflip-game.material").equalsIgnoreCase("PLAYER_HEAD")) {
                    builder = new ItemStackBuilder(Material.matchMaterial(config.getString("games-gui.coinflip-game.material")));
                } else {
                    builder = new ItemStackBuilder(coinflipGame.getCachedHead());
                }

                builder.withName(config.getString("games-gui.coinflip-game.display_name").replace("{PLAYER}", playerFromID.getName()));
                builder.withLore(config.getStringList("games-gui.coinflip-game.lore").stream().map(line -> line
                                .replace("{TAX_RATE}", String.valueOf(taxRate))
                                .replace("{TAX_DEDUCTION}", taxedFormatted)
                                .replace("{AMOUNT}", valueFormatted)
                                .replace("{CURRENCY}", economyManager.getEconomyProvider(coinflipGame.getProvider()).getDisplayName()))
                        .toList());

                GuiItem guiItem = new GuiItem(builder.build());
                guiItem.setAction(event -> {
                    if (!gameManager.getCoinflipGames().containsKey(playerFromID.getUniqueId())) {
                        Messages.ERROR_GAME_UNAVAILABLE.send(player);
                        openInventory(player);
                        return;
                    }

                    if (player.getUniqueId().equals(playerFromID.getUniqueId())) {
                        Messages.ERROR_COINFLIP_SELF.send(player);
                        gui.close(player);
                        return;
                    }

                    CoinflipGame game = gameManager.getCoinflipGames().get(playerFromID.getUniqueId());
                    if (economyManager.getEconomyProvider(game.getProvider()).getBalance(player) < game.getAmount()) {
                        ItemStack previousItem = event.getCurrentItem();
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1L, 0L);
                        event.getClickedInventory().setItem(event.getSlot(), ItemStackBuilder.getItemStack(config.getConfigurationSection("games-gui.error-no-funds")).build());
                        plugin.getScheduler().runTaskLater(() -> event.getClickedInventory().setItem(event.getSlot(), previousItem), 45L);
                        Messages.INSUFFICIENT_FUNDS.send(player);
                        return;
                    }

                    economyManager.getEconomyProvider(game.getProvider()).withdraw(player, game.getAmount());
                    plugin.getGameManager().removeCoinflipGame(playerFromID.getUniqueId());

                    event.getWhoClicked().closeInventory();
                    plugin.getInventoryManager().getCoinflipGUI().startGame(player, playerFromID, game);
                });
                gui.addItem(guiItem);
            }
        }

        gui.open(player);
        gui.update();
    }

    private void loadFillerItems(PaginatedGui gui) {
        ConfigurationSection section = config.getConfigurationSection("games-gui.filler-items");

        if (section != null) {
            for (String entry : section.getKeys(false)) {
                ConfigurationSection fillerConfig = config.getConfigurationSection(section.getCurrentPath() + "." + entry);

                if (fillerConfig != null) {
                    ItemStackBuilder builder = ItemStackBuilder.getItemStack(fillerConfig);

                    if (fillerConfig.contains("slots")) {
                        List<String> slotStrings = fillerConfig.getStringList("slots");
                        slotStrings.forEach(slotString -> {
                            try {
                                int slot = Integer.parseInt(slotString);
                                gui.setItem(slot, new GuiItem(builder.build()));
                            } catch (NumberFormatException e) {
                                // Handle invalid slot format (e.g., not a number)
                                plugin.getLogger().log(Level.WARNING, "Invalid slot format in filler items configuration: " + slotString);
                            }
                        });
                    } else if (fillerConfig.contains("slot")) {
                        int slot = fillerConfig.getInt("slot");
                        gui.setItem(slot, new GuiItem(builder.build()));
                    }
                } else {
                    // Handle missing or invalid filler item configuration
                    plugin.getLogger().log(Level.WARNING, "Invalid or missing configuration for filler item: " + entry);
                }
            }
        } else {
            // Handle missing filler items section in the configuration
            plugin.getLogger().log(Level.SEVERE, "Could not find the filler items section in the configuration file!");
        }
    }


}
