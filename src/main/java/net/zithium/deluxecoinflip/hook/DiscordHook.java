package net.zithium.deluxecoinflip.hook;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.ConfigHandler;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.utility.DiscordIntegration;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DiscordHook {

    private static final NumberFormat numberFormat = NumberFormat.getInstance();

    private final ConfigHandler configHandler;

    public DiscordHook(DeluxeCoinflipPlugin plugin) {
        configHandler = plugin.getConfigHandler(ConfigType.CONFIG);
    }

    public CompletableFuture<DiscordIntegration> executeWebhook(OfflinePlayer winner, OfflinePlayer loser, String currency, long amount, double tax) {

        return CompletableFuture.supplyAsync(() -> {
            final DiscordIntegration webhook = getDiscordIntegration(winner, loser, currency, amount, tax);

            try {
                webhook.execute();
            } catch (IOException e) {
                throw new DiscordIntegration.WebhookExecutionException("An error occurred while executing webhook", e);
            }

            return webhook;
        });

    }

    private DiscordIntegration getDiscordIntegration(OfflinePlayer winner, OfflinePlayer loser, String currency, long amount, double tax) {
        final FileConfiguration config = configHandler.getConfig();

        DiscordIntegration webhook;

        if (config.getBoolean("discord.bot.enabled", false))
            webhook = new DiscordIntegration(config.getString("discord.bot.token"), config.getString("discord.bot.channel"));
        else
            webhook = new DiscordIntegration(config.getString("discord.webhook.url"));


        webhook.setUsername(replace(config.getString("discord.webhook.username", "CoinFlip"), winner, loser, currency, amount, tax))
                .setAvatarUrl(replace(config.getString("discord.webhook.avatar", ""), winner, loser, currency, amount, tax))
                .setContent(replace(config.getString("discord.message.content", ""), winner, loser, currency, amount, tax));

        if (config.getBoolean("discord.message.embed.enabled", false))
            webhook.addEmbed(getEmbed(winner, loser, currency, amount, tax));

        webhook.debug(config.getBoolean("discord.debug", false));

        return webhook;
    }


    private DiscordIntegration.EmbedObject getEmbed(OfflinePlayer winner, OfflinePlayer loser, String currency, long amount, double tax) {
        final FileConfiguration config = configHandler.getConfig();

        return new DiscordIntegration.EmbedObject()
                .setTimestamp(config.getBoolean("discord.message.embed.timestamp", false))
                .setTitle(replace(config.getString("discord.message.embed.title", ""), winner, loser, currency, amount, tax))
                .setDescription(replace(config.getString("discord.message.embed.description", ""), winner, loser, currency, amount, tax))
                .setColor(new Color(config.getInt("discord.message.embed.color.r", 0), config.getInt("discord.message.embed.color.g", 0), config.getInt("discord.message.embed.color.b", 0)));

    }


    private String replace(String string, OfflinePlayer winner, OfflinePlayer loser, String currency, long amount, double tax) {
        return string
                .replace("%tax%", tax + "")
                .replace("%winner%", Objects.requireNonNullElse(winner.getName(), "null"))
                .replace("%loser%", Objects.requireNonNullElse(loser.getName(), "null"))
                .replace("%currency%", Objects.requireNonNullElse(currency, "null"))
                .replace("%amount%", numberFormat.format(amount))
                ;
    }

}
