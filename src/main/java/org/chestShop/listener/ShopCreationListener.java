package org.chestShop.listener;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.DoubleChestInventory;
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

        if (isAnotherShopSignAttached(chest)) {
            ChatUtils.sendErrorMessage(player, "Es existiert bereits ein Shop-Schild an dieser Truhe!");
            event.lines().removeFirst();
            sign.update();
            return;
        }

        ItemStack shopItem;
        if (chest.getInventory() instanceof DoubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest) chest.getInventory().getHolder();
            Chest leftChest = (Chest) doubleChest.getLeftSide();

            shopItem = leftChest.getBlockInventory().getItem(0);
        } else {
            shopItem = chest.getBlockInventory().getItem(0);
        }

        if (shopItem == null) {
            ChatUtils.sendErrorMessage(player, "Die Truhe ist leer.");
            event.lines().removeFirst();
            sign.update();
            return;
        }

        String buyLine = ((TextComponent) event.lines().get(1)).content();
        String sellLine = ((TextComponent) event.lines().get(2)).content();
        int quantity = Integer.parseInt(((TextComponent) event.lines().get(3)).content());

        if (quantity < 1) {
            ChatUtils.sendErrorMessage(player, "Die Anzahl muss mindestens 1 sein.");
            event.lines().removeFirst();
            return;
        }

        if (quantity > 64) {
            ChatUtils.sendErrorMessage(player, "Man kann nicht mehr als 64 als Anzahl angeben.");
            event.lines().removeFirst();
            sign.update();
            return;
        }

        int buyPrice = parsePrice(buyLine, "B:");
        int sellPrice = parsePrice(sellLine, "S:");

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

    private boolean isAnotherShopSignAttached(Chest chest) {
        if (chest.getInventory() instanceof DoubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest) chest.getInventory().getHolder();
            Chest leftChest = (Chest) doubleChest.getLeftSide();
            Chest rightChest = (Chest) doubleChest.getRightSide();

            return isShopSignAttached(leftChest) || isShopSignAttached(rightChest);
        } else {
            return isShopSignAttached(chest);
        }
    }

    private boolean isShopSignAttached(Chest chest) {
        Block[] possibleSignPositions = {
                chest.getBlock().getRelative(1, 0, 0),
                chest.getBlock().getRelative(-1, 0, 0),
                chest.getBlock().getRelative(0, 0, 1),
                chest.getBlock().getRelative(0, 0, -1)
        };

        for (Block block : possibleSignPositions) {
            if (block.getState() instanceof Sign sign) {
                if (isShopSign(sign)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isShopSign(Sign sign) {
        PersistentDataContainer data = sign.getPersistentDataContainer();
        return data.has(new NamespacedKey(plugin, "shopItem"));
    }

    private boolean isShopSign(SignChangeEvent event) {
        return ((TextComponent) event.lines().get(0)).content().equalsIgnoreCase("[Shop]");
    }

    private int parsePrice(String line, String prefix) {
        if (line.startsWith(prefix)) {
            String priceString = line.substring(prefix.length()).trim();
            if (!priceString.isEmpty()) {
                try {
                    return Integer.parseInt(priceString);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private String buildPriceString(int buyPrice, int sellPrice) {
        String buyPart = (buyPrice == -1) ? "-" : "B: " + buyPrice;
        String sellPart = (sellPrice == -1) ? "-" : "S: " + sellPrice;
        return buyPart + " | " + sellPart;
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
