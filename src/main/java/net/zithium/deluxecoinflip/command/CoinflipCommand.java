/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.api.events.CoinflipCreatedEvent;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.GameManager;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.utility.TextUtil;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@CommandAlias("coinflip|cf")
@Description("Main command for using DeluxeCoinflip")
public class CoinflipCommand extends BaseCommand {

    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final GameManager gameManager;

    public CoinflipCommand(final DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.gameManager = plugin.getGameManager();
    }

    @Default
    public void defaultCommand(final CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Console cannot open the coinflip GUI, use /coinflip help");
            return;
        }

        plugin.getInventoryManager().getGamesGUI().openInventory((Player) sender);
    }

    @Subcommand("reload")
    @CommandPermission("coinflip.reload")
    public void reloadSubCommand(final CommandSender sender) {
        plugin.reload();
        Messages.RELOAD.send(sender);
    }

    @Subcommand("help")
    public void helpSubCommand(final CommandSender sender) {
        Messages.HELP_DEFAULT.send(sender, "{PROVIDERS}", economyManager.getEconomyProviders().values().stream().map(p -> p.getDisplayName().toLowerCase()).collect(Collectors.joining(", ")));
        if (sender.hasPermission("coinflip.admin")) Messages.HELP_ADMIN.send(sender);
    }

    @Subcommand("about")
    public void aboutSubCommand(final CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ColorUtil.color("&e&lDeluxeCoinflip"));
        sender.sendMessage(ColorUtil.color("&eVersion: &fv" + plugin.getDescription().getVersion()));
        sender.sendMessage(ColorUtil.color("&eAuthor: &fItzSave"));

        if (!TextUtil.isValidDownload()) {
            sender.sendMessage(ColorUtil.color("&4Registered to: &cFailed to find licensed owner to this plugin. Contact developer to report possible leak (itzsave)."));
        } else if (TextUtil.isBuiltByBit()) {
            sender.sendMessage(ColorUtil.color("&4Registered to: &chttps://builtbybit.com/members/%%__USER__%%/"));
        } else {
            sender.sendMessage(ColorUtil.color("&4Registered to: &chttps://www.spigotmc.org/members/%%__USER__%%/"));
        }
        sender.sendMessage("");
    }

    @Subcommand("toggle")
    public void toggleSubCommand(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can toggle broadcast messages");
            return;
        }

        java.util.Optional<PlayerData> playerDataOptional = plugin.getStorageManager().getPlayer(player.getUniqueId());

        if (playerDataOptional.isEmpty()) {
            sender.sendMessage(ColorUtil.color("&cYour player data has not loaded yet, please wait a few moments or relog."));
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

    @Subcommand("delete|remove")
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

    @Subcommand("create|new")
    @CommandCompletion("* @providers")
    public void createSubCommand(final Player player, String amountInput, @Optional String currencyProvider) {
        final long amount;
        try {
            amount = Long.parseLong(amountInput.replace(",", ""));
        } catch (Exception ex) {
            Messages.INVALID_AMOUNT.send(player, "{INPUT}", amountInput);
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
            Messages.CREATE_MINIMUM_AMOUNT.send(player,"{MIN_BET}", TextUtil.numberFormat(config.getLong("settings.minimum-bet")));
            return;
        }

        final List<EconomyProvider> providers = new ArrayList<>(economyManager.getEconomyProviders().values());
        if (providers.isEmpty()) {
            player.sendMessage(ColorUtil.color("&cThere are no economy providers found or not enabled in the configuration. Please contact an administrator."));
            return;
        }

        EconomyProvider provider;
        if (currencyProvider == null) {
            if (providers.size() == 1) {
                provider = providers.get(0);
            } else {
                String defaultProvider = config.getString("settings.providers.default_provider");
                if (defaultProvider != null && !defaultProvider.isEmpty()) {
                    provider = economyManager.getEconomyProviders().get(defaultProvider);
                } else {
                    provider = null;
                }
            }
        } else {
            provider = getProviderByName(currencyProvider);
        }

        if (provider == null) {
            Messages.INVALID_CURRENCY.send(player,"{CURRENCY_TYPES}", economyManager.getEconomyProviders().values().stream().map(p -> p.getDisplayName().toLowerCase()).collect(Collectors.joining(", ")));
            return;
        }

        if (amount <= provider.getBalance(player)) {
            CoinflipGame coinflipGame = new CoinflipGame(player.getUniqueId(), provider.getIdentifier().toUpperCase(), amount);

            final CoinflipCreatedEvent event = new CoinflipCreatedEvent(player, coinflipGame);
            Bukkit.getPluginManager().callEvent(event);
            if(event.isCancelled()) return;

            provider.withdraw(player, amount);
            gameManager.addCoinflipGame(player.getUniqueId(), coinflipGame);

            if (config.getBoolean("settings.broadcast-coinflip-creation")) {
                Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                    java.util.Optional<PlayerData> playerDataOptional = plugin.getStorageManager().getPlayer(player.getUniqueId());

                    if (playerDataOptional.isPresent()) {
                        PlayerData playerData = playerDataOptional.get();
                        if (playerData.isDisplayBroadcastMessages()) {
                            Messages.COINFLIP_CREATED_BROADCAST.send(onlinePlayer, "{PLAYER}", player.getName(), "{CURRENCY}", provider.getDisplayName(), "{AMOUNT}", TextUtil.numberFormat(amount));
                        }
                    }
                });
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
