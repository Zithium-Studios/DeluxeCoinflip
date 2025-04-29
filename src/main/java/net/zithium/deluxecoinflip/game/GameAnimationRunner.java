package net.zithium.deluxecoinflip.game;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.utility.ItemStackBuilder;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GameAnimationRunner {

    private final DeluxeCoinflipPlugin plugin;
    private final FileConfiguration config;

    public GameAnimationRunner(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();
    }


    public void runAnimation(OfflinePlayer winner, OfflinePlayer loser, CoinflipGame game, Gui gui) {
        final WrappedScheduler scheduler = plugin.getScheduler();

        boolean isWinnerGamePlayer = winner.equals(game.getOfflinePlayer());

        ItemStack winnerItem = new ItemStackBuilder(
                isWinnerGamePlayer ? game.getCachedHead() : new ItemStack(Material.PLAYER_HEAD))
                .withName(ColorUtil.color("<yellow>" + winner.getName()))
                .setSkullOwner(winner)
                .build();

        ItemStack loserItem = new ItemStackBuilder(
                isWinnerGamePlayer ? new ItemStack(Material.PLAYER_HEAD) : game.getCachedHead())
                .withName(ColorUtil.color("<yellow>" + loser.getName()))
                .setSkullOwner(loser)
                .build();

        GuiItem winnerHead = new GuiItem(winnerItem);
        GuiItem loserHead = new GuiItem(loserItem);

        Player winnerPlayer = Bukkit.getPlayer(winner.getUniqueId());
        Player loserPlayer = Bukkit.getPlayer(loser.getUniqueId());

        if (winnerPlayer != null) {
            scheduler.runTaskAtEntity(winnerPlayer, () -> {
                gui.open(winnerPlayer);
                plugin.getInventoryManager().getCoinflipGUI().startAnimation(scheduler, gui, winnerHead, loserHead, winner, loser, game, winnerPlayer, winnerPlayer.getLocation(), true);
            });
        }

        if (loserPlayer != null) {
            scheduler.runTaskAtEntity(loserPlayer, () -> {
                gui.open(loserPlayer);
                plugin.getInventoryManager().getCoinflipGUI().startAnimation(scheduler, gui, winnerHead, loserHead, winner, loser, game, loserPlayer, loserPlayer.getLocation(), false);
            });
        }
    }



}
