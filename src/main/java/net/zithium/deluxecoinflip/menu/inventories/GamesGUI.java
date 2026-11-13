/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.menu.inventories;

import com.tcoded.folialib.impl.PlatformScheduler;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.GameManager;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import net.zithium.deluxecoinflip.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

public class GamesGUI {

    private final DeluxeCoinflipPlugin plugin;
    private final PlatformScheduler scheduler;
    private final EconomyManager economyManager;
    private final Set<UUID> refreshQueued = ConcurrentHashMap.newKeySet();

    public GamesGUI(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = DeluxeCoinflipPlugin.scheduler();
        this.economyManager = plugin.getEconomyManager();
    }

    public void openInventory(Player player) {
        Optional<PlayerData> optionalPlayerData = plugin.getStorageManager().getPlayer(player.getUniqueId());
        if (optionalPlayerData.isEmpty()) {
            player.sendMessage(TextUtil.color("&cYour player data was not found, please relog or contact an administrator if the issue persists."));
            return;
        }

        PlayerData playerData = optionalPlayerData.get();
        FileConfiguration config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();

        String guiTitle = TextUtil.color(config.getString("games-gui.title", "&lCOINFLIP GAMES"));
        int guiRows = config.getInt("games-gui.rows", 6);

        String materialName = config.getString("games-gui.coinflip-game.material", "PLAYER_HEAD");
        ItemStackBuilder materialBuilder = null;
        if (!"PLAYER_HEAD".equalsIgnoreCase(materialName)) {
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                materialBuilder = new ItemStackBuilder(material);
            }
        }

        PaginatedGui gui = Gui.paginated()
                .rows(guiRows)
                .title(Component.text(guiTitle))
                .create();
        gui.setDefaultClickAction(events -> events.setCancelled(true));
        gui.setCloseGuiAction(events -> refreshQueued.remove(player.getUniqueId()));

        loadFillerItems(config, gui, player, playerData);

        placeSectionItem(config, gui, "games-gui.previous-page", player, playerData, events -> gui.previous());
        placeSectionItem(config, gui, "games-gui.next-page", player, playerData, events -> gui.next());
        placeSectionItem(config, gui, "games-gui.stats", player, playerData, null);
        placeSectionItem(config, gui, "games-gui.history", player, playerData, events ->
                plugin.getInventoryManager().getHistoryGUI().openInventory(player));
        placeSectionItem(config, gui, "games-gui.refresh", player, playerData, events -> {
            if (!refreshQueued.add(player.getUniqueId())) {
                return;
            }

            scheduler.runLater(task -> {
                try {
                    if (!player.isOnline()) {
                        return;
                    }

                    if (!player.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
                        return;
                    }

                    openInventory(player);
                } finally {
                    refreshQueued.remove(player.getUniqueId());
                }
            }, 10L);
        });

        GameManager gameManager = plugin.getGameManager();
        if (gameManager.getCoinflipGames().isEmpty()) {
            ConfigurationSection noGamesSection = config.getConfigurationSection("games-gui.no-games");
            int noGamesSlot = config.getInt("games-gui.no-games.slot", -1);
            if (noGamesSection != null && noGamesSlot >= 0) {
                ItemStack noGamesItem = buildItemWithPlaceholders(noGamesSection, line -> applyPlayerStats(line, player, playerData));
                gui.setItem(noGamesSlot, new GuiItem(noGamesItem));
            }
        } else {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            Map<UUID, CoinflipGame> gamesSnapshot = new HashMap<>(gameManager.getCoinflipGames());
            boolean taxEnabled = config.getBoolean("settings.tax.enabled");
            double taxRate = config.getDouble("settings.tax.rate");

            for (Map.Entry<UUID, CoinflipGame> entry : gamesSnapshot.entrySet()) {
                CoinflipGame coinflipGame = entry.getValue();
                EconomyProvider providerForGame = economyManager.getEconomyProvider(coinflipGame.getProvider());
                if (providerForGame == null) {
                    continue;
                }

                long amount = coinflipGame.getAmount();
                long taxed = taxEnabled ? (long) ((taxRate * amount) / 100.0) : 0L;

                String valueFormatted = numberFormat.format(amount);
                String taxedFormatted = numberFormat.format(taxed);

                Player creatorOnline = (coinflipGame.getOfflinePlayer() != null)
                        ? coinflipGame.getOfflinePlayer().getPlayer() : null;

                if (creatorOnline == null) {
                    continue;
                }

                ItemStackBuilder displayItemBuilder = (materialBuilder != null)
                        ? new ItemStackBuilder(materialBuilder.build())
                        : new ItemStackBuilder(coinflipGame.getCachedHead());

                String nameTemplate = config.getString("games-gui.coinflip-game.display_name", "&e{PLAYER}'s Coinflip");
                displayItemBuilder.withName(nameTemplate.replace("{PLAYER}", creatorOnline.getName()));

                List<String> loreTemplate = config.getStringList("games-gui.coinflip-game.lore");
                List<String> lore = new ArrayList<>(loreTemplate.size());
                for (String line : loreTemplate) {
                    lore.add(line
                            .replace("{TAX_RATE}", String.valueOf(taxRate))
                            .replace("{TAX_DEDUCTION}", taxedFormatted)
                            .replace("{AMOUNT}", valueFormatted)
                            .replace("{CURRENCY}", providerForGame.getDisplayName()));
                }

                ItemStack gameDisplayItem = displayItemBuilder.withLore(lore).build();

                GuiItem gameItem = new GuiItem(gameDisplayItem);
                gameItem.setAction(events -> {
                    if (!gameManager.getCoinflipGames().containsKey(creatorOnline.getUniqueId())) {
                        Messages.ERROR_GAME_UNAVAILABLE.send(player);
                        openInventory(player);
                        return;
                    }

                    if (player.getUniqueId().equals(creatorOnline.getUniqueId())) {
                        Messages.ERROR_COINFLIP_SELF.send(player);
                        scheduler.runAtEntity(player, task -> gui.close(player));
                        return;
                    }

                    CoinflipGame selectedGame = gameManager.getCoinflipGames().get(creatorOnline.getUniqueId());
                    EconomyProvider selectedProvider = economyManager.getEconomyProvider(selectedGame.getProvider());
                    if (selectedProvider == null) {
                        Messages.INVALID_CURRENCY.send(player);
                        return;
                    }

                    if (selectedProvider.getBalance(player) < selectedGame.getAmount()) {
                        ItemStack previousItem = events.getCurrentItem();

                        playConfiguredSound(player);

                        ConfigurationSection noFundsSection = config.getConfigurationSection("games-gui.error-no-funds");
                        if (noFundsSection != null && events.getClickedInventory() != null) {
                            events.getClickedInventory().setItem(events.getSlot(), ItemStackBuilder.getItemStack(noFundsSection).build());
                            scheduler.runLater(task -> {
                                if (events.getClickedInventory() != null) {
                                    events.getClickedInventory().setItem(events.getSlot(), previousItem);
                                }
                            }, 45L);
                        }

                        Messages.INSUFFICIENT_FUNDS.send(player);
                        return;
                    }

                    selectedProvider.withdraw(player, selectedGame.getAmount());
                    gameManager.removeCoinflipGame(creatorOnline.getUniqueId());

                    scheduler.runAtEntity(player, task -> events.getWhoClicked().closeInventory());
                    plugin.getInventoryManager().getCoinflipGUI().startGame(creatorOnline, player, selectedGame);
                });

                gui.addItem(gameItem);
            }
        }

        if (plugin.getConfigHandler(ConfigType.CONFIG).getConfig().getBoolean("games-gui.create-new-game.enabled")) {
            int newGameSlot = plugin.getConfigHandler(ConfigType.CONFIG).getConfig().getInt("games-gui.create-new-game.slot", -1);
            ConfigurationSection createSection = plugin.getConfigHandler(ConfigType.CONFIG).getConfig().getConfigurationSection("games-gui.create-new-game");
            if (newGameSlot >= 0 && createSection != null) {
                String initialProviderKey = economyManager.getEconomyProviders().keySet().stream().findFirst().orElse(null);
                ItemStack newGameItem = buildItemWithPlaceholders(createSection, line -> applyPlayerStats(line, player, playerData));
                GuiItem newGameGuiItem = getGuiItem(player, newGameItem, initialProviderKey);
                gui.setItem(newGameSlot, newGameGuiItem);
            }
        }

        scheduler.runAtEntity(player, task -> gui.open(player));

        if (player.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
            scheduler.runLater(task -> gui.update(), 2L);
        }
    }

    private @NotNull GuiItem getGuiItem(Player player, ItemStack newGameItem, String initialProviderKey) {
        GuiItem newGameGuiItem = new GuiItem(newGameItem);
        newGameGuiItem.setAction(events -> {
            if (initialProviderKey == null) {
                Messages.INVALID_CURRENCY.send(player);
                return;
            }

            plugin.getInventoryManager()
                    .getGameBuilderGUI()
                    .openGameBuilderGUI(player, new CoinflipGame(player.getUniqueId(), initialProviderKey, 0));
        });

        return newGameGuiItem;
    }

    private void loadFillerItems(FileConfiguration config, PaginatedGui gui, Player player, PlayerData data) {
        ConfigurationSection fillerSection = config.getConfigurationSection("games-gui.filler-items");
        if (fillerSection == null) {
            plugin.getLogger().log(Level.SEVERE, "Could not find the filler items section in the configuration file!");
            return;
        }

        for (String key : fillerSection.getKeys(false)) {
            ConfigurationSection fillerConfig = config.getConfigurationSection(fillerSection.getCurrentPath() + "." + key);
            if (fillerConfig == null) {
                plugin.getLogger().log(Level.WARNING, "Invalid or missing configuration for filler item: " + key);
                continue;
            }

            ItemStack item = buildItemWithPlaceholders(fillerConfig, line -> applyPlayerStats(line, player, data));

            if (fillerConfig.contains("slots")) {
                for (String slotString : fillerConfig.getStringList("slots")) {
                    try {
                        int slot = Integer.parseInt(slotString);
                        if (slot >= 0 && slot < gui.getInventory().getSize()) {
                            gui.setItem(slot, new GuiItem(item.clone()));
                        }
                    } catch (NumberFormatException numberFormatException) {
                        plugin.getLogger().log(Level.WARNING, "Invalid slot format in filler items configuration: " + slotString);
                    }
                }
            } else if (fillerConfig.contains("slot")) {
                int slot = fillerConfig.getInt("slot");
                if (slot >= 0 && slot < gui.getInventory().getSize()) {
                    gui.setItem(slot, new GuiItem(item));
                }
            }
        }
    }

    private void placeSectionItem(FileConfiguration config, PaginatedGui gui, String path, Player player,
                                  PlayerData data, GuiAction<InventoryClickEvent> clickAction) {

        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return;
        }

        if (!section.getBoolean("enabled", true)) {
            return;
        }

        int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= gui.getInventory().getSize()) {
            return;
        }

        ItemStack item = buildItemWithPlaceholders(section, line -> applyPlayerStats(line, player, data));
        GuiItem guiItem = new GuiItem(item);
        if (clickAction != null) {
            guiItem.setAction(clickAction);
        }

        gui.setItem(slot, guiItem);
    }

    private ItemStack buildItemWithPlaceholders(ConfigurationSection section,
                                                UnaryOperator<String> replacement) {

        ItemStack base = ItemStackBuilder.getItemStack(section).build();
        ItemStackBuilder builder = new ItemStackBuilder(base);

        String displayName = section.getString("display_name");
        if (displayName != null) {
            String replaced = (replacement != null) ? replacement.apply(displayName) : displayName;
            builder.withName(replaced);
        }

        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            List<String> replacedLore = new ArrayList<>(lore.size());
            for (String line : lore) {
                replacedLore.add((replacement != null) ? replacement.apply(line) : line);
            }

            builder.withLore(replacedLore);
        }

        return builder.build();
    }

    private String applyPlayerStats(String line, Player player, PlayerData data) {
        String result = line
                .replace("{WINS}", String.valueOf(data.getWins()))
                .replace("{LOSSES}", String.valueOf(data.getLosses()))
                .replace("{PROFIT}", String.valueOf(data.getProfitFormatted()))
                .replace("{WIN_PERCENTAGE}", String.valueOf(data.getWinPercentage()))
                .replace("{TOTAL_LOSSES}", String.valueOf(data.getTotalLossesFormatted()))
                .replace("{TOTAL_GAMBLED}", String.valueOf(data.getTotalGambledFormatted()))
                .replace("{PLAYER}", player.getName());

        // Apply custom stat placeholders from external plugins
        return plugin.getCustomStatManager().replacePlaceholders(player, result);
    }

    private void playConfiguredSound(Player player) {
        FileConfiguration cfg = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
        ConfigurationSection section = cfg.getConfigurationSection("games-gui.sounds.error_no_funds");

        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }

        String name = section.getString("name", Sound.BLOCK_NOTE_BLOCK_PLING.name());
        Sound chosen;
        try {
            chosen = Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            chosen = Sound.BLOCK_NOTE_BLOCK_PLING;
        }

        float vol = (float) section.getDouble("volume", (float) 1.0);
        float pitch = (float) section.getDouble("pitch", (float) 0.0);
        player.playSound(player.getLocation(), chosen, vol, pitch);
    }
}
