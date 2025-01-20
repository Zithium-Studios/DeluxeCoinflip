package net.zithium.deluxecoinflip.economy.provider.impl;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import me.clip.placeholderapi.PlaceholderAPI;

public class CustomCurrencyProvider extends EconomyProvider {

    private final String rawBalancePlaceholder;
    private final String withdrawCommandTemplate;
    private final String depositCommandTemplate;

    public CustomCurrencyProvider(String identifier, DeluxeCoinflipPlugin plugin) {
        super(identifier);
        this.rawBalancePlaceholder = plugin.getConfig().getString("settings.providers.CUSTOM_CURRENCY.raw_balance_placeholder", "%vault_eco_Balance_fixed%");
        this.withdrawCommandTemplate = plugin.getConfig().getString("settings.providers.CUSTOM_CURRENCY.commands.withdraw", "eco take {player} {amount}");
        this.depositCommandTemplate = plugin.getConfig().getString("settings.providers.CUSTOM_CURRENCY.commands.deposit", "eco give {player} {amount}");
    }

    @Override
    public void onEnable() {
        // Any setup needed when the provider is enabled
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        String balanceString = PlaceholderAPI.setPlaceholders(player, rawBalancePlaceholder);
        try {
            return Double.parseDouble(balanceString);
        } catch (NumberFormatException e) {
            Bukkit.getServer().getLogger().info("There was an error while fetching the balance for " + player.getName() + ": " + e.getMessage());
            return 0; // Returning zero if there's an issue with fetching placeholder. Should prevent the game from proceeding.
        }
    }

    @Override
    public void withdraw(OfflinePlayer player, double amount) {
        String command = withdrawCommandTemplate.replace("{player}", player.getName()).replace("{amount}", String.valueOf(amount));
        executeCommand(command);
    }

    @Override
    public void deposit(OfflinePlayer player, double amount) {
        String command = depositCommandTemplate.replace("{player}", player.getName()).replace("{amount}", String.valueOf(amount));
        executeCommand(command);
    }

    private void executeCommand(String command) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        Bukkit.dispatchCommand(console, command);
    }
}
