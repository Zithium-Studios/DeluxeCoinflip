/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.menu.inventories;

import net.kyori.adventure.text.Component;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.api.events.CoinflipCreatedEvent;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import net.zithium.deluxecoinflip.utility.TextUtil;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GameBuilderGUI {

    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final FileConfiguration config;

    private final String GUI_TITLE;

    private final boolean BROADCAST_CREATION;

    public GameBuilderGUI(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
        this.GUI_TITLE = ColorUtil.color(config.getString("gamebuilder-gui.title"));
        this.BROADCAST_CREATION = config.getBoolean("settings.broadcast-coinflip-creation");
    }

    public void openGameBuilderGUI(Player player, CoinflipGame game) {

        Gui gui = Gui.gui().rows(config.getInt("gamebuilder-gui.rows")).title(Component.text(GUI_TITLE)).create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));

        ConfigurationSection fillerItemsSection = config.getConfigurationSection("gamebuilder-gui.filler-items");
        if (fillerItemsSection != null) {
            for (String entry : fillerItemsSection.getKeys(false)) {
                ItemStackBuilder builder = ItemStackBuilder.getItemStack(config.getConfigurationSection(fillerItemsSection.getCurrentPath() + "." + entry));

                if (config.contains(fillerItemsSection.getCurrentPath() + "." + entry + ".slots")) {
                    for (String slot : config.getStringList(fillerItemsSection.getCurrentPath() + "." + entry + ".slots")) {
                        gui.setItem(Integer.parseInt(slot), new GuiItem(builder.build()));
                    }
                } else if (config.contains(fillerItemsSection.getCurrentPath() + "." + entry + ".slot")) {
                    int slot = config.getInt(fillerItemsSection.getCurrentPath() + "." + entry + ".slot");
                    gui.setItem(slot, new GuiItem(builder.build()));
                }
            }
        }

        GuiItem currencySelectItem = new GuiItem(ItemStackBuilder.getItemStack(config.getConfigurationSection("gamebuilder-gui.currency-select"))
                .withLore(getCurrencyLore(config.getConfigurationSection("gamebuilder-gui.currency-select"), game))
                .build());
        gui.setItem(config.getInt("gamebuilder-gui.currency-select.slot"), currencySelectItem);
        currencySelectItem.setAction(event -> {
            String nextProvider = getNext(game.getProvider());
            game.setProvider(nextProvider);
            currencySelectItem.setItemStack(new ItemStackBuilder(currencySelectItem.getItemStack()).withLore(getCurrencyLore(config.getConfigurationSection("gamebuilder-gui.currency-select"), game)).build());
            gui.update();
            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1L, 0L);
        });

        ConfigurationSection amountItemsSection = config.getConfigurationSection("gamebuilder-gui.amount-items");
        if (amountItemsSection != null) {
            for (String entry : amountItemsSection.getKeys(false)) {
                GuiItem guiItem = new GuiItem(ItemStackBuilder.getItemStack(config.getConfigurationSection(amountItemsSection.getCurrentPath() + "." + entry)).build());

                String setAmount = config.getString(amountItemsSection.getCurrentPath() + "." + entry + ".set_amount");
                guiItem.setAction(event -> {
                    if (setAmount.startsWith("+")) {
                        game.setAmount(game.getAmount() + Long.parseLong(setAmount.replace("+", "")));
                        player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 5L, 0L);
                    } else if (setAmount.startsWith("-")) {
                        game.setAmount(game.getAmount() - Long.parseLong(setAmount.replace("-", "")));
                        player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1L, 0L);
                    }
                    currencySelectItem.setItemStack(new ItemStackBuilder(currencySelectItem.getItemStack()).withLore(getCurrencyLore(config.getConfigurationSection("gamebuilder-gui.currency-select"), game)).build());
                    gui.update();
                });
                gui.setItem(config.getInt(amountItemsSection.getCurrentPath() + "." + entry + ".slot"), guiItem);
            }
        }

        gui.setItem(config.getInt("gamebuilder-gui.custom-amount.slot"), new GuiItem(ItemStackBuilder.getItemStack(config.getConfigurationSection("gamebuilder-gui.custom-amount"))
                .build(), event -> {
            gui.close(player);
            plugin.getListenerCache().put(player.getUniqueId(), game);
            Messages.ENTER_VALUE_FOR_GAME.send(player, "{MIN_BET}", TextUtil.numberFormat(config.getLong("settings.minimum-bet")), "{MAX_BET}", TextUtil.numberFormat(config.getLong("settings.maximum-bet")));
        }));

        gui.setItem(config.getInt("gamebuilder-gui.create-game.slot"), new GuiItem(ItemStackBuilder.getItemStack(config.getConfigurationSection("gamebuilder-gui.create-game"))
                .build(), event -> {
            EconomyProvider provider = economyManager.getEconomyProvider(game.getProvider());


            if (plugin.getGameManager().getCoinflipGames().containsKey(player.getUniqueId())) {
                handleError(player, event, "gamebuilder-gui.error-game-exists");
                return;
            }

            long gameAmount = game.getAmount();
            long minBet = config.getLong("settings.minimum-bet");
            long maxBet = config.getLong("settings.maximum-bet");

            if (gameAmount < minBet || gameAmount > maxBet) {
                handleError(player, event, "gamebuilder-gui.error-limits");
                return;
            }

            // Ensure the user has the correct balance for the game.
            if (gameAmount > provider.getBalance(player)) {
                handleError(player, event, "gamebuilder-gui.error-no-funds");
                return;
            }

            plugin.getScheduler().runTaskAtEntity(player, () -> gui.close(player));

            final CoinflipCreatedEvent createdEvent = new CoinflipCreatedEvent(player, game);
            Bukkit.getPluginManager().callEvent(createdEvent);
            if (createdEvent.isCancelled()) return;

            provider.withdraw(player, game.getAmount());
            plugin.getGameManager().addCoinflipGame(player.getUniqueId(), game.clone());

            String amountFormatted = NumberFormat.getNumberInstance(Locale.US).format(game.getAmount());

            if (BROADCAST_CREATION) {
                Messages.COINFLIP_CREATED_BROADCAST.broadcast("{PLAYER}", player.getName(), "{CURRENCY}", provider.getDisplayName(), "{AMOUNT}", amountFormatted);
            }

            Messages.CREATED_GAME.send(player, "{AMOUNT}", amountFormatted, "{CURRENCY}", provider.getDisplayName());
        }));

        plugin.getScheduler().runTaskAtEntity(player, () -> gui.open(player));
    }

    private List<String> getCurrencyLore(ConfigurationSection section, CoinflipGame game) {
        List<String> lore = section.getStringList("lore-header").stream().map(line -> line.replace("{BET_AMOUNT}", TextUtil.numberFormat(game.getAmount()))).collect(Collectors.toList());

        for (EconomyProvider provider : economyManager.getEconomyProviders().values()) {
            if (game.getProvider().equals(provider.getIdentifier().toUpperCase())) {
                lore.add(section.getString("currency_lore_selected").replace("{CURRENCY}", provider.getDisplayName()));
            } else {
                lore.add(section.getString("currency_lore_unselected").replace("{CURRENCY}", provider.getDisplayName()));
            }
        }
        lore.addAll(section.getStringList("lore-footer"));
        return lore;
    }

    private String getNext(String provider) {
        List<String> providers = new ArrayList<>(economyManager.getEconomyProviders().keySet());
        int i = providers.indexOf(provider);
        try {
            return providers.get(i + 1);
        } catch (IndexOutOfBoundsException e) {
            return economyManager.getEconomyProviders().keySet().stream().findFirst().get();
        }
    }

    /**
     * Plays an error sound, temporarily changes the clicked item to an error indicator,
     * and restores it after a delay.
     *
     * @param player     The player interacting with the GUI.
     * @param event      The inventory click event.
     * @param configPath The configuration path for the error item.
     */
    private void handleError(Player player, InventoryClickEvent event, String configPath) {
        ItemStack previousItem = event.getCurrentItem();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1L, 0L);
        event.getClickedInventory().setItem(event.getSlot(),
                ItemStackBuilder.getItemStack(config.getConfigurationSection(configPath)).build());
        plugin.getScheduler().runTaskLater(() -> event.getClickedInventory().setItem(event.getSlot(), previousItem), 45L);
    }
}
