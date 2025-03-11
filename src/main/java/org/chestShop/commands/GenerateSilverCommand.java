package org.chestShop.commands;

import com.nexomc.nexo.api.NexoItems;
import de.mcterranova.terranovaLib.utils.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GenerateSilverCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chestshop.generatesilver")) {
            Chat.sendErrorMessage(player, "You do not have permission to use this command.");
            return true;
        }

        // Give the player Silver items
        ItemStack silverItem = NexoItems.itemFromId("terranova_silver").build();
        player.getInventory().addItem(silverItem);
        Chat.sendSuccessMessage(player, "You have been given Silver.");

        return true;
    }
}
