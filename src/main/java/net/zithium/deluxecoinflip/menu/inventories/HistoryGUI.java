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
import net.zithium.deluxecoinflip.game.CoinflipHistory;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import net.zithium.deluxecoinflip.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

public class HistoryGUI {

    private final DeluxeCoinflipPlugin plugin;
    private final PlatformScheduler scheduler;

    public HistoryGUI(final DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = DeluxeCoinflipPlugin.scheduler();
    }

    public void openInventory(final Player viewer) {
        this.openInventory(viewer, viewer.getUniqueId(), 1);
    }

    public void openInventory(final Player viewer, final UUID targetUUID) {
        this.openInventory(viewer, targetUUID, 1);
    }

    public void openInventory(final Player viewer, final UUID targetUUID, final int requestedPage) {
        final Optional<PlayerData> optionalPlayerData = this.plugin.getStorageManager().getPlayer(targetUUID);
        if (optionalPlayerData.isEmpty()) {
            viewer.sendMessage(TextUtil.color("&cThat player's data was not found, please relog or contact an administrator if the issue persists."));
            return;
        }

        final PlayerData playerData = optionalPlayerData.get();
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        final String targetName = target.getName() != null ? target.getName() : targetUUID.toString();

        final FileConfiguration config = this.plugin.getConfigHandler(ConfigType.CONFIG).getConfig();

        final String guiTitle = TextUtil.color(
                config.getString("history-gui.title", "&lCOINFLIP HISTORY")
                        .replace("{PLAYER}", targetName)
                        .replace("{VIEWER}", viewer.getName())
        );

        final int guiRows = Math.max(1, config.getInt("history-gui.rows", 4));
        final int pageSize = this.getPageSize(guiRows);
        final String dateFormatPattern = config.getString("history-gui.date-format", "dd/MM/yyyy HH:mm");

        final int totalEntries = Math.max(0, this.plugin.getStorageManager().getStorageHandler().getCoinflipHistoryCount(targetUUID));
        final int totalPages = Math.max(1, (int) Math.ceil(totalEntries / (double) pageSize));
        final int currentPage = Math.min(Math.max(1, requestedPage), totalPages);
        final int offset = (currentPage - 1) * pageSize;

        final List<CoinflipHistory> historyList = this.plugin.getStorageManager()
                .getStorageHandler()
                .getCoinflipHistory(targetUUID, offset, pageSize);

        final PaginatedGui gui = Gui.paginated()
                .rows(guiRows)
                .title(Component.text(guiTitle))
                .create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));

        if (historyList.isEmpty()) {
            final ConfigurationSection noHistorySection = config.getConfigurationSection("history-gui.no-history");
            final int noHistorySlot = config.getInt("history-gui.no-history.slot", -1);

            if (noHistorySection != null && noHistorySlot >= 0 && noHistorySlot < gui.getInventory().getSize()) {
                final ItemStack noHistoryItem = this.buildItemWithPlaceholders(
                        noHistorySection,
                        line -> this.applyPlayerStats(line, viewer, targetName, playerData)
                                .replace("{PAGE}", String.valueOf(currentPage))
                                .replace("{TOTAL_PAGES}", String.valueOf(totalPages))
                                .replace("{TOTAL_ENTRIES}", String.valueOf(totalEntries))
                );
                gui.setItem(noHistorySlot, new GuiItem(noHistoryItem));
            }
        } else {
            final ConfigurationSection historyItemSection = config.getConfigurationSection("history-gui.history-item");
            if (historyItemSection == null) {
                this.plugin.getLogger().warning("Missing configuration section: history-gui.history-item");
                return;
            }

            final SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
            final List<Integer> contentSlots = this.getContentSlots(guiRows);

            for (int i = 0; i < historyList.size() && i < contentSlots.size(); i++) {
                final CoinflipHistory history = historyList.get(i);

                final boolean won = targetUUID.equals(history.getWinnerUUID());
                final UUID opponentUUID = targetUUID.equals(history.getCreatorUUID())
                        ? history.getOpponentUUID()
                        : history.getCreatorUUID();

                final OfflinePlayer opponent = Bukkit.getOfflinePlayer(opponentUUID);
                final String opponentName = opponent.getName() != null ? opponent.getName() : opponentUUID.toString();
                final String result = won ? "&aWIN" : "&cLOSS";
                final String type = history.isForfeit() ? "Forfeit" : "Normal";
                final String date = dateFormat.format(new Date(history.getCreatedAt()));

                final ItemStackBuilder builder;
                if (opponent.hasPlayedBefore() || opponent.isOnline()) {
                    builder = new ItemStackBuilder(Material.PLAYER_HEAD).setSkullOwner(opponent);
                } else {
                    builder = new ItemStackBuilder(Material.BOOK);
                }

                final String displayName = historyItemSection.getString("display_name", "&e{OPPONENT}");
                builder.withName(displayName
                        .replace("{PLAYER}", targetName)
                        .replace("{VIEWER}", viewer.getName())
                        .replace("{OPPONENT}", opponentName)
                        .replace("{PAGE}", String.valueOf(currentPage))
                        .replace("{TOTAL_PAGES}", String.valueOf(totalPages))
                        .replace("{TOTAL_ENTRIES}", String.valueOf(totalEntries)));

                final List<String> loreTemplate = historyItemSection.getStringList("lore");
                final List<String> lore = new ArrayList<>(loreTemplate.size());

                for (final String line : loreTemplate) {
                    lore.add(line
                            .replace("{PLAYER}", targetName)
                            .replace("{VIEWER}", viewer.getName())
                            .replace("{OPPONENT}", opponentName)
                            .replace("{RESULT}", TextUtil.color(result))
                            .replace("{AMOUNT}", TextUtil.numberFormat(history.getBetAmount()))
                            .replace("{WINNINGS}", TextUtil.numberFormat(history.getWinnings()))
                            .replace("{CURRENCY}", history.getProvider())
                            .replace("{TAX_DEDUCTION}", TextUtil.numberFormat(history.getTaxDeduction()))
                            .replace("{TAX_RATE}", String.valueOf(history.getTaxRate()))
                            .replace("{DATE}", date)
                            .replace("{TYPE}", type)
                            .replace("{PAGE}", String.valueOf(currentPage))
                            .replace("{TOTAL_PAGES}", String.valueOf(totalPages))
                            .replace("{TOTAL_ENTRIES}", String.valueOf(totalEntries)));
                }

                builder.withLore(lore);
                gui.setItem(contentSlots.get(i), new GuiItem(builder.build()));
            }
        }

        this.loadFillerItems(config, gui, viewer, targetName, playerData);

        this.placeSectionItem(
                config,
                gui,
                "history-gui.previous-page",
                viewer,
                targetName,
                playerData,
                event -> {
                    if (currentPage > 1) {
                        this.openInventory(viewer, targetUUID, currentPage - 1);
                    }
                }
        );

        this.placeSectionItem(
                config,
                gui,
                "history-gui.next-page",
                viewer,
                targetName,
                playerData,
                event -> {
                    if (currentPage < totalPages) {
                        this.openInventory(viewer, targetUUID, currentPage + 1);
                    }
                }
        );

        this.placeSectionItem(
                config,
                gui,
                "history-gui.back",
                viewer,
                targetName,
                playerData,
                event -> this.plugin.getInventoryManager().getGamesGUI().openInventory(viewer)
        );

        this.scheduler.runAtEntity(viewer, task -> gui.open(viewer));
    }

    private int getPageSize(final int rows) {
        if (rows <= 1) {
            return 9;
        }
        return (rows - 1) * 9;
    }

    private List<Integer> getContentSlots(final int rows) {
        final List<Integer> slots = new ArrayList<>();
        final int contentRows = Math.max(1, rows - 1);
        final int maxSlot = contentRows * 9;

        for (int slot = 0; slot < maxSlot; slot++) {
            slots.add(slot);
        }

        return slots;
    }

    private void loadFillerItems(final FileConfiguration config, final PaginatedGui gui, final Player viewer, final String targetName, final PlayerData data) {
        final ConfigurationSection fillerSection = config.getConfigurationSection("history-gui.filler-items");
        if (fillerSection == null) {
            return;
        }

        for (final String key : fillerSection.getKeys(false)) {
            final ConfigurationSection fillerConfig = config.getConfigurationSection(fillerSection.getCurrentPath() + "." + key);
            if (fillerConfig == null) {
                this.plugin.getLogger().log(Level.WARNING, "Invalid or missing configuration for history filler item: " + key);
                continue;
            }

            final ItemStack item = this.buildItemWithPlaceholders(
                    fillerConfig,
                    line -> this.applyPlayerStats(line, viewer, targetName, data)
            );

            if (fillerConfig.contains("slots")) {
                for (final String slotString : fillerConfig.getStringList("slots")) {
                    try {
                        final int slot = Integer.parseInt(slotString);
                        if (slot >= 0 && slot < gui.getInventory().getSize()) {
                            gui.setItem(slot, new GuiItem(item.clone()));
                        }
                    } catch (final NumberFormatException exception) {
                        this.plugin.getLogger().log(Level.WARNING, "Invalid slot format in history filler items configuration: " + slotString);
                    }
                }
            } else if (fillerConfig.contains("slot")) {
                final int slot = fillerConfig.getInt("slot");
                if (slot >= 0 && slot < gui.getInventory().getSize()) {
                    gui.setItem(slot, new GuiItem(item));
                }
            }
        }
    }

    private void placeSectionItem(
            final FileConfiguration config,
            final PaginatedGui gui,
            final String path,
            final Player viewer,
            final String targetName,
            final PlayerData data,
            final GuiAction<InventoryClickEvent> clickAction
    ) {
        final ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return;
        }

        if (!section.getBoolean("enabled", true)) {
            return;
        }

        final int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= gui.getInventory().getSize()) {
            return;
        }

        final ItemStack item = this.buildItemWithPlaceholders(
                section,
                line -> this.applyPlayerStats(line, viewer, targetName, data)
        );

        final GuiItem guiItem = new GuiItem(item);

        if (clickAction != null) {
            guiItem.setAction(clickAction);
        }

        gui.setItem(slot, guiItem);
    }

    private ItemStack buildItemWithPlaceholders(final ConfigurationSection section, final UnaryOperator<String> replacement) {
        final ItemStack base = ItemStackBuilder.getItemStack(section).build();
        final ItemStackBuilder builder = new ItemStackBuilder(base);

        final String displayName = section.getString("display_name");
        if (displayName != null) {
            builder.withName(replacement != null ? replacement.apply(displayName) : displayName);
        }

        final List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            final List<String> replacedLore = new ArrayList<>(lore.size());
            for (final String line : lore) {
                replacedLore.add(replacement != null ? replacement.apply(line) : line);
            }
            builder.withLore(replacedLore);
        }

        return builder.build();
    }

    private String applyPlayerStats(final String line, final Player viewer, final String targetName, final PlayerData data) {
        return line
                .replace("{WINS}", String.valueOf(data.getWins()))
                .replace("{LOSSES}", String.valueOf(data.getLosses()))
                .replace("{PROFIT}", String.valueOf(data.getProfitFormatted()))
                .replace("{WIN_PERCENTAGE}", String.valueOf(data.getWinPercentage()))
                .replace("{TOTAL_LOSSES}", String.valueOf(data.getTotalLossesFormatted()))
                .replace("{TOTAL_GAMBLED}", String.valueOf(data.getTotalGambledFormatted()))
                .replace("{PLAYER}", targetName)
                .replace("{VIEWER}", viewer.getName());
    }
}