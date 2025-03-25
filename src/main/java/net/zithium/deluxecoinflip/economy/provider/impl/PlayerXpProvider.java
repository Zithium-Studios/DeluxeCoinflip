package net.zithium.deluxecoinflip.economy.provider.impl;

import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlayerXpProvider extends EconomyProvider {

    public PlayerXpProvider() {
        super("XP");
    }

    @Override
    public void onEnable() {

    }

    @Override
    public double getBalance(OfflinePlayer player) {
        Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId()); // just to in case if the offline player object is not up to date
        if (onlinePlayer == null) return -1;

        return onlinePlayer.getTotalExperience();
    }

    @Override
    public void withdraw(OfflinePlayer player, double amount) {
        Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId()); // just to in case if the offline player object is not up to date
        if (onlinePlayer == null) return;

        onlinePlayer.giveExp((int) -amount);
    }

    @Override
    public void deposit(OfflinePlayer player, double amount) {
        Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId()); // just to in case if the offline player object is not up to date
        if (onlinePlayer == null) return;

        onlinePlayer.giveExp((int) amount);
    }

}
