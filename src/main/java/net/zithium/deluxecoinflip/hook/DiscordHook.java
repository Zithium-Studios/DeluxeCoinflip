package net.zithium.deluxecoinflip.hook;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.ConfigHandler;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.utility.DiscordWebhook;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DiscordHook {

    private static final NumberFormat numberFormat = NumberFormat.getInstance();

    private final DeluxeCoinflipPlugin plugin;
    private final ConfigHandler configHandler;

    public DiscordHook(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;

        configHandler = plugin.getConfigHandler(ConfigType.CONFIG);
    }

    public CompletableFuture<DiscordWebhook> executeWebhook(OfflinePlayer winner, OfflinePlayer loser, String currency, long amount) {

        return CompletableFuture.supplyAsync(() -> {
            final DiscordWebhook webhook = getWebhook(winner, loser, currency, amount);

            try {
                webhook.execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return webhook;
        });

    }

    private DiscordWebhook getWebhook(OfflinePlayer winner, OfflinePlayer loser, String currency, long amount) {
        final FileConfiguration config = configHandler.getConfig();

        final DiscordWebhook webhook = new DiscordWebhook(config.getString("webhook.url"))
                .setUsername(replace(config.getString("webhook.username", "CoinFlip"), winner, loser, currency, amount))
                .setAvatarUrl(replace(config.getString("webhook.avatar", ""), winner, loser, currency, amount))
                .setContent(replace(config.getString("webhook.message.content", ""), winner, loser, currency, amount));

        if (config.getBoolean("webhook.message.embed.enabled", false))
            webhook.addEmbed(getEmbed(winner, loser, currency, amount));

        return webhook;
    }


    private DiscordWebhook.EmbedObject getEmbed(OfflinePlayer winner, OfflinePlayer loser, String currency, long amount) {
        final FileConfiguration config = configHandler.getConfig();

        return new DiscordWebhook.EmbedObject()
                .setTimestamp(config.getBoolean("webhook.message.embed.timestamp", false))
                .setTitle(replace(config.getString("webhook.message.embed.title", ""), winner, loser, currency, amount))
                .setDescription(replace(config.getString("webhook.message.embed.description", ""), winner, loser, currency, amount))
                .setColor(new Color(config.getInt("webhook.message.embed.color.r", 0), config.getInt("webhook.message.embed.color.g", 0), config.getInt("webhook.message.embed.color.b", 0)));

    }


    private String replace(String string, OfflinePlayer winner, OfflinePlayer loser, String currency, long amount) {
        return string
                .replace("%winner%", Objects.requireNonNullElse(winner.getName(), "null"))
                .replace("%loser%", Objects.requireNonNullElse(loser.getName(), "null"))
                .replace("%currency%", Objects.requireNonNullElse(currency, "null"))
                .replace("%amount%", numberFormat.format(amount))
                ;
    }

}
