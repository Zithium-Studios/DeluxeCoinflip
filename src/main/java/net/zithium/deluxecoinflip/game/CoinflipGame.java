/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.game;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class CoinflipGame {

    private final UUID uuid;
    private OfflinePlayer player;
    private String provider;
    private long amount;
    private ItemStack cachedHead;

    public CoinflipGame(UUID uuid, String provider, long amount) {
        this.uuid = uuid;
        this.provider = provider;
        this.amount = amount;
        this.cachedHead = new ItemStack(Material.PLAYER_HEAD);

        DeluxeCoinflipPlugin.getInstance().getScheduler().runTaskAsynchronously(() -> {
            this.player = Bukkit.getOfflinePlayer(uuid);
            this.cachedHead = new ItemStackBuilder(Material.PLAYER_HEAD).setSkullOwner(player).build();
        });
    }

    public CoinflipGame(UUID uuid, String provider, long amount, OfflinePlayer player, ItemStack cachedHead) {
        this.uuid = uuid;
        this.provider = provider;
        this.amount = amount;
        this.cachedHead = cachedHead;
        this.player = player;
    }

    public UUID getPlayerUUID() {
        return uuid;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
        if (this.amount < 0) this.amount = 0;
    }

    public OfflinePlayer getOfflinePlayer() {
        return player;
    }

    public ItemStack getCachedHead() {
        return cachedHead.clone();
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public CoinflipGame clone() {
        return new CoinflipGame(uuid, provider, amount, player, cachedHead);
    }
}
