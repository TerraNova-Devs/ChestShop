package org.chestShop.listener;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.chestShop.ChestShop;
import org.chestShop.helper.InventoryHelper;
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
            ChatUtils.sendErrorMessage(player, "Keine Truhe hinter dem Schild gefunden!");
            event.lines().removeFirst();
            sign.update();
            return;
        }

        ItemStack shopItem = chest.getBlockInventory().getItem(0);
        if (shopItem == null) {
            ChatUtils.sendErrorMessage(player, "Die Truhe ist leer.");
            event.lines().removeFirst();
            sign.update();
            return;
        }

        String buyLine = ((TextComponent) event.lines().get(1)).content();
        String sellLine = ((TextComponent) event.lines().get(2)).content();
        int quantity = Integer.parseInt(((TextComponent) event.lines().get(3)).content());
        if(quantity < 1){
            ChatUtils.sendErrorMessage(player, "Die Anzahl muss mindestens 1 sein.");
            event.lines().removeFirst();
            return;
        }
        if(quantity > 64){
            ChatUtils.sendErrorMessage(player, "Man kann nicht mehr als 64 als Anzahl angeben.");
            event.lines().removeFirst();
            sign.update();
            return;
        }
        int buyPrice = parsePrice(buyLine, "B:");
        int sellPrice = parsePrice(sellLine, "S:");

        if(buyPrice < 0 || sellPrice < 0){

            return;
        }

        if (buyPrice == -1 && sellPrice == -1) {
            ChatUtils.sendErrorMessage(player, "Kein Preis angegeben.");
            event.lines().removeFirst();
            sign.update();
            return;
        }

        String fullPricesString = buildPriceString(buyPrice, sellPrice);
        updateSign(event, player.getName(), fullPricesString, shopItem.getType().name(), quantity);
        sign.update();

        saveShopData(sign, buyPrice, sellPrice, quantity, shopItem, player.getUniqueId(), attachedBlock);
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

    private void updateSign(SignChangeEvent event, String playerName, String fullPricesString, String itemType, int quantity) {
        event.lines().set(0, ChatUtils.stringToComponent(playerName));
        event.lines().set(1, ChatUtils.stringToComponent(fullPricesString));
        event.lines().set(2, ChatUtils.stringToComponent(itemType));
        event.lines().set(3, ChatUtils.stringToComponent(quantity + ""));
    }

    private void saveShopData(Sign sign, int buyPrice, int sellPrice, int quantity, ItemStack shopItem, UUID ownerUUID, Block attachedBlock) {
        PersistentDataContainer data = sign.getPersistentDataContainer();
        data.set(new NamespacedKey(plugin, "buyPrice"), PersistentDataType.INTEGER, buyPrice);
        data.set(new NamespacedKey(plugin, "sellPrice"), PersistentDataType.INTEGER, sellPrice);
        data.set(new NamespacedKey(plugin, "quantity"), PersistentDataType.INTEGER, quantity);
        data.set(new NamespacedKey(plugin, "shopItem"), PersistentDataType.STRING, shopItem.getType().name());

        // Serialize ItemStack to base64
        String itemStackBase64 = InventoryHelper.itemStackArrayToBase64(new ItemStack[]{shopItem});
        data.set(new NamespacedKey(plugin, "itemStack"), PersistentDataType.STRING, itemStackBase64);

        data.set(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING, ownerUUID.toString());
        data.set(new NamespacedKey(plugin, "chestX"), PersistentDataType.INTEGER, attachedBlock.getX());
        data.set(new NamespacedKey(plugin, "chestY"), PersistentDataType.INTEGER, attachedBlock.getY());
        data.set(new NamespacedKey(plugin, "chestZ"), PersistentDataType.INTEGER, attachedBlock.getZ());
        data.set(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER, 0);
        sign.setWaxed(true);
        sign.update();
    }
}
