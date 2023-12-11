package net.zithium.deluxecoinflip.command;

import me.mattstudios.mf.annotations.Command;
import me.mattstudios.mf.annotations.Default;
import me.mattstudios.mf.base.CommandBase;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.storage.PlayerData;
import org.bukkit.entity.Player;

import java.util.Optional;

@Command("cfd")
public class DebugCommand extends CommandBase {

    private final DeluxeCoinflipPlugin plugin;

    public DebugCommand(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
    }


    @Default
    public void debug(Player player) {
        Optional<PlayerData> playerData = plugin.getStorageManager().getPlayer(player.getUniqueId());

        player.sendMessage("Active Game: " + playerData.get().hasActiveGame());
        player.sendMessage("Stats: " + playerData.get().getLosses() + " , " + playerData.get().getWins());
    }
}
