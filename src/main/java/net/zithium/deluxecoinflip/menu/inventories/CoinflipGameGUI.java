package net.zithium.deluxecoinflip.menu.inventories;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class CoinflipGameGUI implements InventoryHolder {

    private final Inventory inventory;
    private final DeluxeCoinflipPlugin plugin;


    public CoinflipGameGUI(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.inventory = plugin.getServer().createInventory(this, 9);

    }
    @NotNull
    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
