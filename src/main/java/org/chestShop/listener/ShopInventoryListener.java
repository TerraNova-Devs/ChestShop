package org.chestShop.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.chestShop.ChestShop;
import org.chestShop.utils.ChatUtils;

import java.util.List;

public class ShopInventoryListener implements Listener {

    private final ChestShop plugin;

    public ShopInventoryListener(ChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getCurrentItem() == null || !player.hasMetadata("shopSign")) {
            return;
        }

        List<MetadataValue> metadataValues = player.getMetadata("shopSign");
        if (metadataValues.isEmpty() || event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) {
            event.setCancelled(true);
        }

        Location signLocation = (Location) metadataValues.get(0).value();
        Block signBlock = signLocation.getBlock();

        if (!(signBlock.getState() instanceof Sign sign)) {
            return;
        }

        PersistentDataContainer data = sign.getPersistentDataContainer();
        handleInventoryClick(event, player, data);
        sign.update();
        event.setCancelled(true);
    }

    private void handleInventoryClick(InventoryClickEvent event, Player player, PersistentDataContainer data) {
        if (event.getCurrentItem().getType() == Material.IRON_NUGGET) {
            int ironNuggetsVault = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
            int nuggetsToWithdraw = event.getCurrentItem().getAmount();

            if (nuggetsToWithdraw <= ironNuggetsVault) {
                data.set(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, ironNuggetsVault - nuggetsToWithdraw);
                player.getInventory().addItem(new ItemStack(Material.IRON_NUGGET, nuggetsToWithdraw));
                player.sendMessage(ChatUtils.returnGreenFade("You have withdrawn " + nuggetsToWithdraw + " iron nuggets."));
            } else {
                player.sendMessage(ChatUtils.returnRedFade("Not enough iron nuggets earned."));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (player.hasMetadata("shopSign")) {
            player.removeMetadata("shopSign", plugin);
        }
    }
}
