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

public class GameBuilderGUI {

    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final FileConfiguration config;
    private final String GUI_TITLE;
    private final boolean BROADCAST_CREATION;

    public GameBuilderGUI(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
        this.GUI_TITLE = ColorUtil.color(config.getString("gamebuilder-gui.title"));
        this.BROADCAST_CREATION = config.getBoolean("settings.broadcast-coinflip-creation");
    }

    public void openGameBuilderGUI(Player player, CoinflipGame game) {
        Gui gui = Gui.gui()
                .rows(config.getInt("gamebuilder-gui.rows"))
                .title(Component.text(GUI_TITLE))
                .create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));

        setFillerItems(gui);
        setupCurrencySelector(gui, player, game);
        setupAmountItems(gui, player, game);
        setupCustomAmount(gui, player, game);
        setupCreateGame(gui, player, game);

        plugin.getScheduler().runTaskAtEntity(player, () -> gui.open(player));
    }

    private void setFillerItems(Gui gui) {
        ConfigurationSection section = config.getConfigurationSection("gamebuilder-gui.filler-items");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) continue;

            ItemStack item = ItemStackBuilder.getItemStack(itemSection).build();
            List<Integer> slots = getSlots(itemSection);
            for (int slot : slots) {
                gui.setItem(slot, new GuiItem(item));
            }
        }
    }

    private void setupCurrencySelector(Gui gui, Player player, CoinflipGame game) {
        ConfigurationSection section = config.getConfigurationSection("gamebuilder-gui.currency-select");
        if (section == null || !section.getBoolean("enabled", true)) return;

        int slot = section.getInt("slot");
        ItemStack item = ItemStackBuilder.getItemStack(section)
                .withLore(getCurrencyLore(section, game))
                .build();

        GuiItem guiItem = new GuiItem(item);
        guiItem.setAction(event -> {
            game.setProvider(getNext(game.getProvider()));
            ItemStack updated = new ItemStackBuilder(guiItem.getItemStack())
                    .withLore(getCurrencyLore(section, game))
                    .build();
            guiItem.setItemStack(updated);
            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1L, 0L);
            gui.update();
        });

        gui.setItem(slot, guiItem);
    }

    private void setupAmountItems(Gui gui, Player player, CoinflipGame game) {
        ConfigurationSection section = config.getConfigurationSection("gamebuilder-gui.amount-items");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) continue;

            ItemStack item = ItemStackBuilder.getItemStack(itemSection).build();
            String setAmount = itemSection.getString("set_amount");
            int slot = itemSection.getInt("slot");

            GuiItem guiItem = new GuiItem(item);
            guiItem.setAction(event -> {
                if (setAmount != null) {
                    long delta = Long.parseLong(setAmount.replaceAll("[^0-9]", ""));
                    if (setAmount.startsWith("+")) {
                        game.setAmount(game.getAmount() + delta);
                    } else if (setAmount.startsWith("-")) {
                        game.setAmount(game.getAmount() - delta);
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 5L, 0L);
                    gui.update();
                }
            });

            gui.setItem(slot, guiItem);
        }
    }

    private void setupCustomAmount(Gui gui, Player player, CoinflipGame game) {
        ConfigurationSection section = config.getConfigurationSection("gamebuilder-gui.custom-amount");
        if (section == null) return;

        GuiItem item = new GuiItem(
                ItemStackBuilder.getItemStack(section).build(),
                event -> {
                    gui.close(player);
                    plugin.getListenerCache().put(player.getUniqueId(), game);
                    Messages.ENTER_VALUE_FOR_GAME.send(player,
                            "{MIN_BET}", TextUtil.numberFormat(config.getLong("settings.minimum-bet")),
                            "{MAX_BET}", TextUtil.numberFormat(config.getLong("settings.maximum-bet")));
                });

        gui.setItem(section.getInt("slot"), item);
    }

    private void setupCreateGame(Gui gui, Player player, CoinflipGame game) {
        ConfigurationSection section = config.getConfigurationSection("gamebuilder-gui.create-game");
        if (section == null) return;

        GuiItem item = new GuiItem(ItemStackBuilder.getItemStack(section).build(), event -> {
            EconomyProvider provider = economyManager.getEconomyProvider(game.getProvider());

            if (plugin.getGameManager().getCoinflipGames().containsKey(player.getUniqueId())) {
                handleError(player, event, "gamebuilder-gui.error-game-exists");
                return;
            }

            long amount = game.getAmount();
            long min = config.getLong("settings.minimum-bet");
            long max = config.getLong("settings.maximum-bet");

            if (amount < min || amount > max) {
                handleError(player, event, "gamebuilder-gui.error-limits");
                return;
            }

            if (amount > provider.getBalance(player)) {
                handleError(player, event, "gamebuilder-gui.error-no-funds");
                return;
            }

            plugin.getScheduler().runTaskAtEntity(player, () -> gui.close(player));

            CoinflipCreatedEvent createdEvent = new CoinflipCreatedEvent(player, game);
            Bukkit.getPluginManager().callEvent(createdEvent);
            if (createdEvent.isCancelled()) return;

            provider.withdraw(player, amount);
            plugin.getGameManager().addCoinflipGame(player.getUniqueId(), game.clone());

            String formatted = NumberFormat.getNumberInstance(Locale.US).format(amount);

            if (BROADCAST_CREATION) {
                Messages.COINFLIP_CREATED_BROADCAST.broadcast("{PLAYER}", player.getName(), "{CURRENCY}", provider.getDisplayName(), "{AMOUNT}", formatted);
            }

            Messages.CREATED_GAME.send(player, "{AMOUNT}", formatted, "{CURRENCY}", provider.getDisplayName());
        });

        gui.setItem(section.getInt("slot"), item);
    }

    private List<String> getCurrencyLore(ConfigurationSection section, CoinflipGame game) {
        List<String> lore = new ArrayList<>(section.getStringList("lore-header").stream()
                .map(line -> line.replace("{BET_AMOUNT}", TextUtil.numberFormat(game.getAmount())))
                .toList());

        for (EconomyProvider provider : economyManager.getEconomyProviders().values()) {
            String display = provider.getDisplayName();
            String line = game.getProvider().equalsIgnoreCase(provider.getIdentifier())
                    ? section.getString("currency_lore_selected")
                    : section.getString("currency_lore_unselected");

            lore.add(line.replace("{CURRENCY}", display));
        }

        lore.addAll(section.getStringList("lore-footer"));
        return lore;
    }

    private List<Integer> getSlots(ConfigurationSection section) {
        List<Integer> slots = new ArrayList<>();
        if (section.contains("slots")) {
            for (String s : section.getStringList("slots")) {
                slots.add(Integer.parseInt(s));
            }
        } else if (section.contains("slot")) {
            slots.add(section.getInt("slot"));
        }
        return slots;
    }

    private String getNext(String current) {
        List<String> keys = new ArrayList<>(economyManager.getEconomyProviders().keySet());
        int index = keys.indexOf(current);
        return keys.get((index + 1) % keys.size());
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
        ItemStack original = event.getCurrentItem();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1L, 0L);
        event.getClickedInventory().setItem(event.getSlot(),
                ItemStackBuilder.getItemStack(config.getConfigurationSection(configPath)).build());
        plugin.getScheduler().runTaskLater(() -> event.getClickedInventory().setItem(event.getSlot(), original), 45L);
    }
}
