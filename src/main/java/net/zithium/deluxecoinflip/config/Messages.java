/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.config;

import net.zithium.deluxecoinflip.utility.TextUtil;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;

public enum Messages {

    PREFIX("general.prefix"),
    RELOAD("general.reload"),
    NO_PERMISSION("general.no-permission"),
    HELP_DEFAULT("general.help_default"),
    HELP_ADMIN("general.help_admin"),

    BROADCASTS_TOGGLED_ON("coinflip.toggle_broadcasts_on"),
    BROADCASTS_TOGGLED_OFF("coinflip.toggle_broadcasts_off"),
    GAME_NOT_FOUND("coinflip.game_not_found"),
    CREATED_GAME("coinflip.created_coinflip"),
    DELETED_GAME("coinflip.deleted_coinflip"),
    INSUFFICIENT_FUNDS("coinflip.insufficient-funds"),
    CREATE_MINIMUM_AMOUNT("coinflip.minimum-amount"),
    CREATE_MAXIMUM_AMOUNT("coinflip.maximum-amount"),
    GAME_ACTIVE("coinflip.coinflip-active"),
    PLAYER_CHALLENGE("coinflip.player-challenged-you"),
    COINFLIP_BROADCAST("coinflip.broadcast-coinflip"),
    COINFLIP_CREATED_BROADCAST("coinflip.broadcast-created-coinflip"),

    ERROR_GAME_UNAVAILABLE("coinflip.game-unavailable"),
    ERROR_COINFLIP_SELF("coinflip.cant-coinflip-self"),
    ENTER_VALUE_FOR_GAME("coinflip.enter-value"),
    CHAT_CANCELLED("coinflip.chat-cancelled"),
    INVALID_CURRENCY("coinflip.invalid-currency"),
    INVALID_AMOUNT("coinflip.invalid-amount"),

    GAME_SUMMARY_LOSS("coinflip.summary-loss"),
    GAME_SUMMARY_WIN("coinflip.summary-win");

    private static FileConfiguration config;
    private final String path;

    private static final boolean papiAvailable;
    private static final Method papiMethod;

    static {
        boolean available = false;
        Method method = null;

        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            method = papiClass.getMethod("setPlaceholders", Player.class, String.class);
            available = true;
        } catch (Throwable ignored) {
            // PlaceholderAPI ain't installed it doesn't throw an error :D
        }

        papiAvailable = available;
        papiMethod = method;
    }

    Messages(String path) {
        this.path = path;
    }

    public static void setConfiguration(FileConfiguration c) {
        config = c;
    }

    public void broadcast(Object... replacements) {
        Bukkit.getOnlinePlayers().forEach(player -> send(player, replacements));
    }

    public void send(CommandSender receiver, Object... replacements) {
        Object value = config.get(this.path);

        String message;
        if (value == null) {
            message = "DeluxeCoinflip: message not found (" + this.path + ")";
        } else {
            message = value instanceof List ? TextUtil.fromList((List<?>) value) : value.toString();
        }

        if (message != null && !message.isEmpty()) {
            message = replace(message, receiver, replacements);
            receiver.sendMessage(ColorUtil.color(message));
        }
    }

    private String replace(String message, CommandSender receiver, Object... replacements) {
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 >= replacements.length) break;

            String key = String.valueOf(replacements[i]);
            String value = String.valueOf(replacements[i + 1]);

            if (receiver instanceof Player && papiAvailable && papiMethod != null) {
                try {
                    Object result = papiMethod.invoke(null, receiver, value);
                    if (result instanceof String) {
                        value = (String) result;
                    }
                } catch (Throwable ignored) {}
            }

            message = message.replace(key, value);
        }

        String prefix = config.getString(PREFIX.getPath());
        message = message.replace("{PREFIX}", prefix != null ? prefix : "");

        if (receiver instanceof Player && papiAvailable && papiMethod != null) {
            try {
                Object result = papiMethod.invoke(null, receiver, message);
                if (result instanceof String) {
                    message = (String) result;
                }
            } catch (Throwable ignored) {}
        }

        return message;
    }

    public String getPath() {
        return this.path;
    }

    private String getConfiguration() {
        return "%%__NONCE__%%";
    }
}
