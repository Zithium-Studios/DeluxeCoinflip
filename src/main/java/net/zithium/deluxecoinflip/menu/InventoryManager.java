package net.zithium.deluxecoinflip.menu;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.menu.inventories.CoinflipGUI;
import net.zithium.deluxecoinflip.menu.inventories.GameBuilderGUI;
import net.zithium.deluxecoinflip.menu.inventories.GamesGUI;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class InventoryManager {

    private GamesGUI gamesGUI;
    private CoinflipGUI coinflipGUI;
    private GameBuilderGUI gameBuilderGUI;

    public void load(DeluxeCoinflipPlugin plugin) {
        gamesGUI = new GamesGUI(plugin);
        coinflipGUI = new CoinflipGUI(plugin);
        gameBuilderGUI = new GameBuilderGUI(plugin);
    }

    public GamesGUI getGamesGUI() {
        return gamesGUI;
    }

    public CoinflipGUI getCoinflipGUI() {
        return coinflipGUI;
    }

    public GameBuilderGUI getGameBuilderGUI() {
        return gameBuilderGUI;
    }

    public static ItemStack applyDupeProtection(ItemStack item, DeluxeCoinflipPlugin plugin) {
        if (item == null) return null;
        var meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(
                plugin.getKey("dcf.dupeprotection"),
                PersistentDataType.BYTE,
                (byte) 1
        );
        item.setItemMeta(meta);
        return item;
    }
}
