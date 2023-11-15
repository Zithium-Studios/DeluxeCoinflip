/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.listener;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

public class PlayerChatListener implements Listener {

    private final DeluxeCoinflipPlugin plugin;

    public PlayerChatListener(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (plugin.getListenerCache().getIfPresent(uuid) == null) return;

        CoinflipGame game = plugin.getListenerCache().getIfPresent(uuid);
        if (event.getMessage().trim().equalsIgnoreCase("cancel")) {
            event.setCancelled(true);
            plugin.getListenerCache().invalidate(uuid);
            Messages.CHAT_CANCELLED.send(player);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getInventoryManager().getGameBuilderGUI().openGameBuilderGUI(player, game));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(event.getMessage().replace(",", "").replaceAll("[^\\d]", ""));
        } catch (Exception e) {
            event.setCancelled(true);
            Messages.INVALID_AMOUNT.send(player, "{INPUT}", event.getMessage().replace(",", ""));
            return;
        }

        FileConfiguration config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
        String maximumBetFormatted = NumberFormat.getNumberInstance(Locale.US).format(config.getInt("settings.maximum-bet"));
        String minimumBetFormatted = NumberFormat.getNumberInstance(Locale.US).format(config.getInt("settings.minimum-bet"));
        if (amount > config.getLong("settings.maximum-bet")) {
            event.setCancelled(true);
            Messages.CREATE_MAXIMUM_AMOUNT.send(player, "{MAX_BET}", maximumBetFormatted);
            return;
        }

        if (amount < config.getLong("settings.minimum-bet")) {
            event.setCancelled(true);
            Messages.CREATE_MINIMUM_AMOUNT.send(player, "{MIN_BET}", minimumBetFormatted);
            return;
        }

        event.setCancelled(true);
        plugin.getListenerCache().invalidate(uuid);
        game.setAmount(amount);
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getInventoryManager().getGameBuilderGUI().openGameBuilderGUI(player, game));
    }

}
