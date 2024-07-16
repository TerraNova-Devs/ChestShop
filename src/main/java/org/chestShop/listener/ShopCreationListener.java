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
        if (!(block.getState() instanceof Sign sign) || !isShopSign(event)) {
            return;
        }

        Block attachedBlock = block.getRelative(((org.bukkit.block.data.type.WallSign) block.getBlockData()).getFacing().getOppositeFace());
        if (!(attachedBlock.getState() instanceof Chest chest)) {
            player.sendMessage(ChatUtils.returnRedFade("No chest found behind the sign!"));
            return;
        }

        ItemStack shopItem = chest.getBlockInventory().getItem(0);
        if (shopItem == null) {
            player.sendMessage(ChatUtils.returnRedFade("Chest is empty."));
            return;
        }

        String buyLine = ((TextComponent) event.lines().get(1)).content();
        String sellLine = ((TextComponent) event.lines().get(2)).content();
        int buyPrice = parsePrice(buyLine, "B:");
        int sellPrice = parsePrice(sellLine, "S:");

        if (buyPrice == -1 && sellPrice == -1) {
            player.sendMessage(ChatUtils.returnRedFade("No price specified."));
            return;
        }

        String fullPricesString = buildPriceString(buyPrice, sellPrice);
        updateSign(event, player.getName(), fullPricesString, shopItem.getType().name());
        sign.update();

        saveShopData(sign, buyPrice, sellPrice, shopItem.getType().name(), player.getUniqueId(), attachedBlock);
    }

    private boolean isShopSign(SignChangeEvent event) {
        return ((TextComponent) event.lines().get(0)).content().equalsIgnoreCase("[Shop]");
    }

    private int parsePrice(String line, String prefix) {
        return line.startsWith(prefix) ? Integer.parseInt(line.substring(2)) : -1;
    }

    private String buildPriceString(int buyPrice, int sellPrice) {
        return "B: " + (buyPrice == -1 ? "-" : buyPrice) + " | S: " + (sellPrice == -1 ? "-" : sellPrice);
    }

    private void updateSign(SignChangeEvent event, String playerName, String fullPricesString, String itemType) {
        event.lines().set(0, ChatUtils.stringToComponent("[Shop]"));
        event.lines().set(1, ChatUtils.stringToComponent(playerName));
        event.lines().set(2, ChatUtils.stringToComponent(fullPricesString));
        event.lines().set(3, ChatUtils.stringToComponent(itemType));
    }

    private void saveShopData(Sign sign, int buyPrice, int sellPrice, String shopItem, UUID ownerUUID, Block attachedBlock) {
        PersistentDataContainer data = sign.getPersistentDataContainer();
        data.set(new NamespacedKey(plugin, "buyPrice"), PersistentDataType.INTEGER, buyPrice);
        data.set(new NamespacedKey(plugin, "sellPrice"), PersistentDataType.INTEGER, sellPrice);
        data.set(new NamespacedKey(plugin, "shopItem"), PersistentDataType.STRING, shopItem);
        data.set(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING, ownerUUID.toString());
        data.set(new NamespacedKey(plugin, "chestX"), PersistentDataType.INTEGER, attachedBlock.getX());
        data.set(new NamespacedKey(plugin, "chestY"), PersistentDataType.INTEGER, attachedBlock.getY());
        data.set(new NamespacedKey(plugin, "chestZ"), PersistentDataType.INTEGER, attachedBlock.getZ());
        data.set(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
        sign.setWaxed(true);
        sign.update();
    }
}
