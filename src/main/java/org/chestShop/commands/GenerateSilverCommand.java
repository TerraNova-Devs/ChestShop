package org.chestShop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.chestShop.utils.ChatUtils;
import org.chestShop.utils.silver.SilverManager;

public class GenerateSilverCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chestshop.generatesilver")) {
            ChatUtils.sendErrorMessage(player, "You do not have permission to use this command.");
            return true;
        }

        // Give the player Silver items
        player.getInventory().addItem(SilverManager.get().placeholder());
        ChatUtils.sendSuccessMessage(player, "You have been given Silver.");

        return true;
    }
}
