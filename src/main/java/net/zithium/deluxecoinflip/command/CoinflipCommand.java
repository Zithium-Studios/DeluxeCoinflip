/*
 * DeluxeCoinflip Plugin
 * Copyright (c) Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.command;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.api.events.CoinflipCreatedEvent;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.GameManager;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.utility.TextUtil;
import me.mattstudios.mf.annotations.*;
import me.mattstudios.mf.annotations.Optional;
import me.mattstudios.mf.base.CommandBase;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

@Command("coinflip")
public class CoinflipCommand extends CommandBase {

    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final GameManager gameManager;

    public CoinflipCommand(final DeluxeCoinflipPlugin plugin, final List<String> aliases) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.gameManager = plugin.getGameManager();
        super.setAliases(aliases);
    }

    @Default
    public void defaultCommand(final CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Console cannot open the coinflip GUI, use /coinflip help");
            return;
        }

        plugin.getInventoryManager().getGamesGUI().openInventory((Player) sender);
    }

    @SubCommand("reload")
    @Permission("coinflip.reload")
    public void reloadSubCommand(final CommandSender sender) {
        plugin.reload();
        Messages.RELOAD.send(sender);
    }

    @SubCommand("help")
    public void helpSubCommand(final CommandSender sender) {
        Messages.HELP_DEFAULT.send(sender, "{PROVIDERS}", economyManager.getEconomyProviders().values().stream().map(p -> p.getDisplayName().toLowerCase()).collect(Collectors.joining(", ")));
        if (sender.hasPermission("coinflip.admin")) Messages.HELP_ADMIN.send(sender);
    }

    @SubCommand("about")
    public void aboutSubCommand(final CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(TextUtil.color("&e&lDeluxeCoinflip"));
        sender.sendMessage(TextUtil.color("&eVersion: &fv" + plugin.getDescription().getVersion()));
        sender.sendMessage(TextUtil.color("&eAuthor: &fItsLewizzz"));

        if (!TextUtil.isValidDownload()) {
            sender.sendMessage(TextUtil.color("&4Registered to: &cFailed to find licensed owner to this plugin. Contact developer to report possible leak (ItsLewizzz#6023)."));
        } else if (TextUtil.isMCMarket()) {
            sender.sendMessage(TextUtil.color("&4Registered to: &chttps://www.mc-market.org/members/%%__USER__%%/"));
        } else {
            sender.sendMessage(TextUtil.color("&4Registered to: &chttps://www.spigotmc.org/members/%%__USER__%%/"));
        }
        sender.sendMessage("");
    }

    @SubCommand("toggle")
    public void toggleSubCommand(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can toggle broadcast messages");
            return;
        }

        java.util.Optional<PlayerData> playerDataOptional = plugin.getStorageManager().getPlayer(player.getUniqueId());

        if (playerDataOptional.isEmpty()) {
            sender.sendMessage(TextUtil.color("&cYour player data has not loaded yet, please wait a few moments or relog."));
            return;
        }

        PlayerData playerData = playerDataOptional.get();
        if (playerData.isDisplayBroadcastMessages()) {
            Messages.BROADCASTS_TOGGLED_OFF.send(player);
            playerData.setDisplayBroadcastMessages(false);
        } else {
            Messages.BROADCASTS_TOGGLED_ON.send(player);
            playerData.setDisplayBroadcastMessages(true);
        }
    }

    @SubCommand("delete")
    @Alias("remove")
    public void deleteSubCommand(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can remove a coinflip game");
            return;
        }

        UUID uuid = player.getUniqueId();
        if (gameManager.getCoinflipGames().containsKey(uuid)) {
            final CoinflipGame game = gameManager.getCoinflipGames().get(uuid);

            economyManager.getEconomyProvider(game.getProvider()).deposit(player, game.getAmount());
            gameManager.removeCoinflipGame(uuid);
            Messages.DELETED_GAME.send(player);

        } else {
            Messages.GAME_NOT_FOUND.send(player);
        }
    }

    @SubCommand("create")
    @Alias("new")
    @WrongUsage("&c/coinflip create <amount> [economy]")
    public void createSubCommand(final Player player, String input, @Optional String providerInput) {
        if (!gameManager.canStartGame()) { // Refuse to start game if the state is set to false.
            player.sendMessage("");
            return;
        }
        final long amount;
        try {
            amount = Long.parseLong(input.replace(",", ""));
        } catch (Exception ex) {
            Messages.INVALID_AMOUNT.send(player, "{INPUT}", input);
            return;
        }

        if (gameManager.getCoinflipGames().containsKey(player.getUniqueId())) {
            Messages.GAME_ACTIVE.send(player);
            return;
        }

        final FileConfiguration config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();

        if (amount > config.getLong("settings.maximum-bet")) {
            Messages.CREATE_MAXIMUM_AMOUNT.send(player, "{MAX_BET}", TextUtil.numberFormat(config.getLong("settings.maximum-bet")));
            return;
        }

        if (amount < config.getLong("settings.minimum-bet")) {
            Messages.CREATE_MINIMUM_AMOUNT.send(player, "{MIN_BET}", TextUtil.numberFormat(config.getLong("settings.minimum-bet")));
            return;
        }

        final List<EconomyProvider> providers = new ArrayList<>(economyManager.getEconomyProviders().values());
        if (providers.isEmpty()) {
            player.sendMessage(TextUtil.color("&cThere are no economy providers found or not enabled in the configuration. Please contact an administrator."));
            return;
        }

        EconomyProvider provider = null;
        if (providerInput == null) {
            if (providers.size() == 1) {
                provider = providers.get(0);
            } else {
                String defaultProvider = config.getString("settings.providers.default_provider");
                if (defaultProvider != null && !defaultProvider.isEmpty()) {
                    provider = economyManager.getEconomyProviders().get(defaultProvider);
                }
            }
        } else {
            provider = getProviderByName(providerInput);
        }

        if (provider == null) {
            Messages.INVALID_CURRENCY.send(player, "{CURRENCY_TYPES}", economyManager.getEconomyProviders().values().stream().map(p -> p.getDisplayName().toLowerCase()).collect(Collectors.joining(", ")));
            return;
        }

        if (amount <= provider.getBalance(player)) {
            CoinflipGame coinflipGame = new CoinflipGame(player.getUniqueId(), provider.getIdentifier().toUpperCase(), amount);

            final CoinflipCreatedEvent event = new CoinflipCreatedEvent(player, coinflipGame);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            provider.withdraw(player, amount);
            gameManager.addCoinflipGame(player.getUniqueId(), coinflipGame);

            if (config.getBoolean("settings.broadcast-coinflip-creation")) {
                Messages.COINFLIP_CREATED_BROADCAST.broadcast("{PLAYER}", player.getName(), "{CURRENCY}", provider.getDisplayName(), "{AMOUNT}", TextUtil.numberFormat(amount));
            }

            Messages.CREATED_GAME.send(player, "{CURRENCY}", provider.getDisplayName(), "{AMOUNT}", TextUtil.numberFormat(amount));
        } else {
            Messages.INSUFFICIENT_FUNDS.send(player);
        }
    }

    private EconomyProvider getProviderByName(String name) {
        for (EconomyProvider provider : economyManager.getEconomyProviders().values()) {
            if (name.equalsIgnoreCase(provider.getDisplayName())) return provider;
        }
        return null;
    }
}
