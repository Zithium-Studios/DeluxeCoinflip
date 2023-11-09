/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.economy;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.economy.provider.impl.TokenEnchantProvider;
import net.zithium.deluxecoinflip.economy.provider.impl.TokenManagerProvider;
import net.zithium.deluxecoinflip.economy.provider.impl.VaultProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class EconomyManager {

    private final DeluxeCoinflipPlugin plugin;
    private final Map<String, EconomyProvider> economyProviders;

    public EconomyManager(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyProviders = new LinkedHashMap<>();
    }

    /**
     * Load economies
     */
    public void onEnable() {
        if (!economyProviders.isEmpty()) economyProviders.clear();

        ConfigurationSection section = plugin.getConfigHandler(ConfigType.CONFIG).getConfig().getConfigurationSection("settings.providers");
        Logger logger = plugin.getLogger();
        if (section == null) {
            logger.severe("There are no enabled providers set in the config. Plugin will now disable..");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        Stream.of(
                new VaultProvider(),
                new TokenEnchantProvider(),
                new TokenManagerProvider()
        ).forEach(provider -> registerEconomyProvider(provider, provider.getIdentifier()));

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (EconomyProvider provider : new ArrayList<>(economyProviders.values())) {
                ConfigurationSection providerSection = section.getConfigurationSection(provider.getIdentifier().toUpperCase());
                if (providerSection != null) {
                    if (!providerSection.getBoolean("enabled")) {
                        economyProviders.remove(provider.getIdentifier().toUpperCase());
                        continue;
                    }

                    if (providerSection.contains("display_currency_name")) {
                        provider.setCurrencyDisplayName(providerSection.getString("display_currency_name"));
                    }

                    provider.onEnable();
                }
            }

            logger.info("Found and using " + String.join(", ", economyProviders.keySet()) + " economy provider(s).");
        });
    }

    /**
     * Register an Economy
     *
     * @param provider       The {@link EconomyProvider}
     * @param requiredPlugin The required plugin in order to work
     */
    public void registerEconomyProvider(EconomyProvider provider, String requiredPlugin) {
        if (requiredPlugin != null) {
            if (plugin.getServer().getPluginManager().getPlugin(requiredPlugin) != null) {
                economyProviders.put(provider.getIdentifier().toUpperCase(), provider);
                plugin.getLogger().info("Registered economy provider '" + provider.getIdentifier() + "' using " + requiredPlugin + " plugin.");
            }
        } else {
            economyProviders.put(provider.getIdentifier().toUpperCase(), provider);
            plugin.getLogger().info("Registered economy provider '" + provider.getIdentifier());
        }
    }

    /**
     * Fetch an EconomyProvider (if registered and loaded)
     *
     * @param identifier The identifier
     * @return The EconomyProvider if found, otherwise null
     */
    public EconomyProvider getEconomyProvider(String identifier) {
        return economyProviders.get(identifier);
    }

    /**
     * Get all loaded economies
     *
     * @return Map of String for economy identifer and {@link EconomyProvider} object
     */
    public Map<String, EconomyProvider> getEconomyProviders() {
        return Collections.unmodifiableMap(economyProviders);
    }
}
