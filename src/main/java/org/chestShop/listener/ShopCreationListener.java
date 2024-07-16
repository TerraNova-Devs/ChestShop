package org.chestShop.listener;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.chestShop.ChestShop;
import org.chestShop.utils.ChatUtils;

import java.util.List;
import java.util.UUID;

public class ShopCreationListener implements Listener {

    private final ChestShop plugin;

    public ShopCreationListener(ChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getState() instanceof Sign sign) {
            if (((TextComponent) event.lines().get(0)).content().equalsIgnoreCase("[Shop]")) {
                Block attachedBlock = block.getRelative(((org.bukkit.block.data.type.WallSign) block.getBlockData()).getFacing().getOppositeFace());
                if (!(attachedBlock.getState() instanceof Chest)) {
                    player.sendMessage(ChatUtils.returnRedFade("No chest found behind the sign!"));
                    return;
                }

                ItemStack shopItem = ((Chest)attachedBlock.getState()).getBlockInventory().getItem(0);
                if(shopItem == null){
                    player.sendMessage(ChatUtils.returnRedFade("Chest is empty."));
                    return;
                }

                String buyLine = ((TextComponent) event.lines().get(1)).content();
                String sellLine = ((TextComponent) event.lines().get(2)).content();
                int buyPrice = -1;
                int sellPrice = -1;
                if (buyLine.startsWith("B:")) {
                    buyPrice = Integer.parseInt(buyLine.substring(2));
                }
                if (sellLine.startsWith("S:")) {
                    sellPrice = Integer.parseInt(sellLine.substring(2));
                }

                String fullPricesString = "";
                if(buyPrice == -1 && sellPrice == -1) {
                    player.sendMessage(ChatUtils.returnRedFade("No price specified."));
                    return;
                }
                if(buyPrice == -1){
                    fullPricesString += "B: -";
                } else if(buyPrice >= 0){
                    fullPricesString += "B: " + buyPrice;
                }
                fullPricesString += " | ";
                if(sellPrice == -1){
                    fullPricesString += "S: -";
                } else if(sellPrice >= 0){
                    fullPricesString += "S: " + sellPrice;
                }

                event.lines().set(0, ChatUtils.stringToComponent("[Shop]"));
                event.lines().set(1, ChatUtils.stringToComponent(player.getName()));
                event.lines().set(2, ChatUtils.stringToComponent(fullPricesString));
                event.lines().set(3, ChatUtils.stringToComponent(shopItem.getType().name()));
                sign.update();

                // Save shop data to the sign
                PersistentDataContainer data = sign.getPersistentDataContainer();
                data.set(new NamespacedKey(plugin, "buyPrice"), PersistentDataType.INTEGER, buyPrice);
                data.set(new NamespacedKey(plugin, "sellPrice"), PersistentDataType.INTEGER, sellPrice);
                data.set(new NamespacedKey(plugin, "shopItem"), PersistentDataType.STRING, shopItem.getType().name());

                // Save the owner UUID to the sign
                UUID ownerUUID = player.getUniqueId();
                data.set(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING, ownerUUID.toString());

                // Save the location of the chest to the sign
                if (attachedBlock.getState() instanceof Chest) {
                    data.set(new NamespacedKey(plugin, "chestX"), PersistentDataType.INTEGER, attachedBlock.getX());
                    data.set(new NamespacedKey(plugin, "chestY"), PersistentDataType.INTEGER, attachedBlock.getY());
                    data.set(new NamespacedKey(plugin, "chestZ"), PersistentDataType.INTEGER, attachedBlock.getZ());
                }

                // Initialize the earnings counter
                data.set(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
                sign.setWaxed(true);
                sign.update();
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            if (((TextComponent) sign.getSide(Side.FRONT).lines().getFirst()).content().equalsIgnoreCase("[Shop]")) {
                Player player = event.getPlayer();
                PersistentDataContainer data = sign.getPersistentDataContainer();
                UUID ownerUUID = UUID.fromString(data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));
                if (!player.getUniqueId().equals(ownerUUID)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatUtils.returnRedFade("You do not own this shop!"));
                } else {
                    Integer ironNuggetsCount = data.get(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER);
                    if(ironNuggetsCount != null && ironNuggetsCount > 0){
                        ItemStack ironNuggets = new ItemStack(Material.IRON_NUGGET, ironNuggetsCount);
                        event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), ironNuggets);
                    }
                    player.sendMessage(ChatUtils.returnYellowFade("You removed this shop!"));
                }
            }
        }
    }
}
