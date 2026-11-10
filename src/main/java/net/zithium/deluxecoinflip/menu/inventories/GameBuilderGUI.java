/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.menu.inventories;

import com.tcoded.folialib.impl.PlatformScheduler;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
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
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameBuilderGUI {

    private final DeluxeCoinflipPlugin plugin;
    private final PlatformScheduler scheduler;
    private final EconomyManager economyManager;
    private final Set<UUID> suppressReturn = ConcurrentHashMap.newKeySet();

    public GameBuilderGUI(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = DeluxeCoinflipPlugin.scheduler();
        this.economyManager = plugin.getEconomyManager();
    }

    public void openGameBuilderGUI(Player player, CoinflipGame game) {
        FileConfiguration cfg = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();

        Gui gui = Gui.gui()
                .rows(cfg.getInt("gamebuilder-gui.rows"))
                .title(Component.text(TextUtil.color(cfg.getString("gamebuilder-gui.title", "&lFLIPPING COIN..."))))
                .create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));

        gui.setCloseGuiAction(event -> {
            Player eventPlayer = (Player) event.getPlayer();
            if (!suppressReturn.remove(eventPlayer.getUniqueId())) {
                plugin.getInventoryManager().getGamesGUI().openInventory(eventPlayer);
            }
        });

        setFillerItems(gui, cfg);
        setupCurrencySelector(gui, player, game, cfg);
        setupAmountItems(gui, player, game, cfg);

        setupCustomAmount(gui, player, game, cfg);

        setupCreateGame(gui, player, game, cfg);

        scheduler.runAtEntity(player, task -> gui.open(player));
    }

    private void setFillerItems(Gui gui, FileConfiguration cfg) {
        ConfigurationSection section = cfg.getConfigurationSection("gamebuilder-gui.filler-items");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }

            ItemStack item = ItemStackBuilder.getItemStack(itemSection).build();
            List<Integer> slots = getSlots(itemSection);
            for (int slot : slots) {
                gui.setItem(slot, new GuiItem(item));
            }
        }
    }

    private GuiItem createCurrencyGuiItem(Gui gui, ConfigurationSection section, Player player, CoinflipGame game) {
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
            playConfiguredSound(player, "gamebuilder-gui.sounds.currency_select_click", Sound.BLOCK_TRIPWIRE_CLICK_ON);
            gui.update();
        });

        return guiItem;
    }

    private void setupCurrencySelector(Gui gui, Player player, CoinflipGame game, FileConfiguration cfg) {
        ConfigurationSection section = cfg.getConfigurationSection("gamebuilder-gui.currency-select");
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }

        int slot = section.getInt("slot");
        GuiItem guiItem = createCurrencyGuiItem(gui, section, player, game);
        gui.setItem(slot, guiItem);
    }

    private void setupAmountItems(Gui gui, Player player, CoinflipGame game, FileConfiguration cfg) {
        ConfigurationSection section = cfg.getConfigurationSection("gamebuilder-gui.amount-items");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }

            ItemStack item = ItemStackBuilder.getItemStack(itemSection).build();
            String setAmount = itemSection.getString("set_amount");
            int slot = itemSection.getInt("slot");

            GuiItem guiItem = new GuiItem(item);
            guiItem.setAction(event -> {
                if (setAmount != null) {
                    String digits = setAmount.replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        long delta = Long.parseLong(digits);
                        if (setAmount.startsWith("+")) {
                            game.setAmount(game.getAmount() + delta);
                        } else if (setAmount.startsWith("-")) {
                            game.setAmount(game.getAmount() - delta);
                        }
                    }

                    playConfiguredSound(player, "gamebuilder-gui.sounds.amount_adjust_click", Sound.BLOCK_TRIPWIRE_CLICK_ON);

                    ConfigurationSection currencySection = cfg.getConfigurationSection("gamebuilder-gui.currency-select");
                    if (currencySection != null && currencySection.getBoolean("enabled", true)) {
                        int currencySlot = currencySection.getInt("slot");
                        GuiItem updatedCurrency = createCurrencyGuiItem(gui, currencySection, player, game);
                        gui.setItem(currencySlot, updatedCurrency);
                    }

                    gui.update();
                }
            });

            gui.setItem(slot, guiItem);
        }
    }

    private void setupCustomAmount(Gui gui, Player player, CoinflipGame game, FileConfiguration cfg) {
        ConfigurationSection section = cfg.getConfigurationSection("gamebuilder-gui.custom-amount");
        if (section == null) {
            return;
        }

        if (!section.getBoolean("enabled", true)) {
            return;
        }

        GuiItem item = new GuiItem(
                ItemStackBuilder.getItemStack(section).build(),
                event -> {
                    suppressReturn.add(player.getUniqueId());
                    scheduler.runAtEntity(player, task -> gui.close(player));
                    plugin.getListenerCache().put(player.getUniqueId(), game);
                    Messages.ENTER_VALUE_FOR_GAME.send(
                            player,
                            "{MIN_BET}", TextUtil.numberFormat(cfg.getLong("settings.minimum-bet")),
                            "{MAX_BET}", TextUtil.numberFormat(cfg.getLong("settings.maximum-bet"))
                    );
                });

        gui.setItem(section.getInt("slot"), item);
    }

    private void setupCreateGame(Gui gui, Player player, CoinflipGame game, FileConfiguration cfg) {
        ConfigurationSection section = cfg.getConfigurationSection("gamebuilder-gui.create-game");
        if (section == null) {
            return;
        }

        GuiItem item = new GuiItem(ItemStackBuilder.getItemStack(section).build(), event -> {
            EconomyProvider provider = economyManager.getEconomyProvider(game.getProvider());

            if (plugin.getGameManager().getCoinflipGames().containsKey(player.getUniqueId())) {
                handleError(player, event, cfg, "gamebuilder-gui.error-game-exists");
                return;
            }

            long amount = game.getAmount();
            long min = cfg.getLong("settings.minimum-bet");
            long max = cfg.getLong("settings.maximum-bet");

            if (amount < min || amount > max) {
                handleError(player, event, cfg, "gamebuilder-gui.error-limits");
                return;
            }

            if (amount > (long) provider.getBalance(player)) {
                handleError(player, event, cfg, "gamebuilder-gui.error-no-funds");
                return;
            }

            suppressReturn.add(player.getUniqueId());
            scheduler.runAtEntity(player, task -> gui.close(player));

            CoinflipCreatedEvent createdEvent = new CoinflipCreatedEvent(player, game);
            Bukkit.getPluginManager().callEvent(createdEvent);
            if (createdEvent.isCancelled()) {
                return;
            }

            provider.withdraw(player, amount);
            plugin.getGameManager().addCoinflipGame(player.getUniqueId(), game.clone());

            String formatted = NumberFormat.getNumberInstance(Locale.US).format(amount);

            if (cfg.getBoolean("settings.broadcast-coinflip-creation")) {
                Messages.COINFLIP_CREATED_BROADCAST.broadcast(
                        "{PLAYER}", player.getName(),
                        "{CURRENCY}", provider.getDisplayName(),
                        "{AMOUNT}", formatted
                );
            }

            Messages.CREATED_GAME.send(
                    player,
                    "{AMOUNT}", formatted,
                    "{CURRENCY}", provider.getDisplayName()
            );
        });

        gui.setItem(section.getInt("slot"), item);
    }

    private List<String> getCurrencyLore(ConfigurationSection section, CoinflipGame game) {
        List<String> lore = new ArrayList<>();

        for (String line : section.getStringList("lore-header")) {
            if (line != null) {
                lore.add(line.replace("{BET_AMOUNT}", TextUtil.numberFormat(game.getAmount())));
            }
        }

        for (EconomyProvider provider : economyManager.getEconomyProviders().values()) {
            String display = provider.getDisplayName();
            boolean selected = game.getProvider().equalsIgnoreCase(provider.getIdentifier());
            String template = selected
                    ? section.getString("currency_lore_selected")
                    : section.getString("currency_lore_unselected");

            if (template != null) {
                lore.add(template.replace("{CURRENCY}", display));
            }
        }

        List<String> footer = section.getStringList("lore-footer");
        if (!footer.isEmpty()) {
            lore.addAll(footer);
        }

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
    private void handleError(Player player, InventoryClickEvent event, FileConfiguration cfg, String configPath) {
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }

        ItemStack original = event.getCurrentItem();
        playConfiguredSound(player, "gamebuilder-gui.sounds.error", Sound.BLOCK_NOTE_BLOCK_PLING);

        ConfigurationSection errorSection = cfg.getConfigurationSection(configPath);
        if (errorSection != null) {
            clicked.setItem(event.getSlot(), ItemStackBuilder.getItemStack(errorSection).build());
            scheduler.runLater(task -> {
                if (event.getSlot() >= 0 && event.getSlot() < clicked.getSize()) {
                    clicked.setItem(event.getSlot(), original);
                }
            }, 45L);
        }
    }

    private void playConfiguredSound(Player player, String path, Sound def) {
        FileConfiguration cfg = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
        ConfigurationSection s = cfg.getConfigurationSection(path);

        if (s == null || !s.getBoolean("enabled", true)) {
            return;
        }

        String name = s.getString("name", def.name());
        Sound chosen;
        try {
            chosen = Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            chosen = def;
        }

        float vol = (float) s.getDouble("volume", (float) 1.0);
        float pitch = (float) s.getDouble("pitch", (float) 0.0);
        player.playSound(player.getLocation(), chosen, vol, pitch);
    }
}
