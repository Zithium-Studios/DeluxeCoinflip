package net.zithium.deluxecoinflip.menu;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.CompletableFuture;

public class DupeProtection implements Listener {

    private final DeluxeCoinflipPlugin plugin;

    public DupeProtection(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void cleanPlayerInventoryAsync(Player player) {
        CompletableFuture.runAsync(() -> {
            boolean changed = false;
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (isProtected(item)) {
                    contents[i] = null;
                    changed = true;
                }
            }
            if (changed) {
                Bukkit.getScheduler().runTask(plugin, () -> player.getInventory().setContents(contents));
            }
        });
    }

    private boolean isProtected(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        var container = meta.getPersistentDataContainer();
        return container.has(plugin.getKey("dcf.dupeprotection"), PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        // Only cancel the drop event if the dropped item is flagged.
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (isProtected(droppedItem)) {
            event.setCancelled(true);
        }
        cleanPlayerInventoryAsync(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        // Only cancel the interact event if the item being used is flagged.
        ItemStack interactedItem = event.getItem();
        if (interactedItem != null && isProtected(interactedItem)) {
            event.setCancelled(true);
        }
        cleanPlayerInventoryAsync(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        cleanPlayerInventoryAsync(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            cleanPlayerInventoryAsync(player);
        }
    }
}
