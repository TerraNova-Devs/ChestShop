package org.chestShop.listener;

import de.mcterranova.terranovaLib.utils.Chat;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.chestShop.ChestShop;
import org.chestShop.helper.InventoryHelper;

import java.util.List;

public class ShopInventoryListener implements Listener {

    private final ChestShop plugin;

    public ShopInventoryListener(ChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getInventory())) {
            return;
        }
        if (!player.hasMetadata("shopSign")) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        event.setCancelled(true);

        List<MetadataValue> metadataValues = player.getMetadata("shopSign");
        if (metadataValues.isEmpty()) return;

        Location signLocation = (Location) metadataValues.get(0).value();
        Block signBlock = signLocation.getBlock();
        if (!(signBlock.getState() instanceof Sign sign)) {
            return;
        }

        PersistentDataContainer data = sign.getPersistentDataContainer();

        Material type = clickedItem.getType();
        int slot = event.getRawSlot();

        // Handle known special items (silver vault and chest)
        if (type == Material.RED_WOOL && slot == 10) {
            // Withdraw silver
            handleWithdrawSilver(player, data, sign);
        } else if (type == Material.GREEN_WOOL && slot == 12) {
            // Deposit silver
            handleDepositSilver(player, data, sign);
        } else if (type == Material.CHEST && slot == 16) {
            // Open linked chest
            handleOpenChest(player, data);
        } else if (isGuiItem(clickedItem)) {
            // Handle adjustments (buy/sell price, quantity)
            handleShopAdjustments(event, player, sign, data, slot);
        }

        // Update silver display after any changes
        updateSilverDisplay(data, event.getView().getTopInventory());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (!player.hasMetadata("shopSign")) return;

        Location signLocation = (Location) player.getMetadata("shopSign").get(0).value();
        Block signBlock = signLocation.getBlock();
        if (!(signBlock.getState() instanceof Sign sign)) {
            player.removeMetadata("shopSign", plugin);
            return;
        }

        PersistentDataContainer data = sign.getPersistentDataContainer();
        Inventory inv = event.getInventory();

        ItemStack newItem = inv.getItem(43);
        if (newItem != null && newItem.getType() != Material.HOPPER && newItem.getType() != Material.AIR) {
            Material newMat = newItem.getType();
            data.set(new NamespacedKey(plugin, "shopItem"), PersistentDataType.STRING, newMat.name());

            // Save exact ItemStack if needed
            String itemStackBase64 = InventoryHelper.itemStackArrayToBase64(new ItemStack[]{newItem});
            data.set(new NamespacedKey(plugin, "itemStack"), PersistentDataType.STRING, itemStackBase64);

            // Update the sign to show new item
            int buyPrice = data.getOrDefault(new NamespacedKey(plugin, "buyPrice"), PersistentDataType.INTEGER, 0);
            int sellPrice = data.getOrDefault(new NamespacedKey(plugin, "sellPrice"), PersistentDataType.INTEGER, 0);
            int quantity = data.getOrDefault(new NamespacedKey(plugin, "quantity"), PersistentDataType.INTEGER, 1);
            updateSignText(sign, player.getName(), buyPrice, sellPrice, quantity, newMat);
        }

        player.removeMetadata("shopSign", plugin);
    }

    private boolean isGuiItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        return type == Material.RED_WOOL || type == Material.IRON_NUGGET || type == Material.GREEN_WOOL ||
                type == Material.GRAY_STAINED_GLASS_PANE || type == Material.CHEST ||
                type == Material.PAPER || type == Material.HOPPER;
    }

    private void handleShopAdjustments(InventoryClickEvent event, Player player, Sign sign, PersistentDataContainer data, int slot) {
        int buyPrice = data.getOrDefault(new NamespacedKey(plugin, "buyPrice"), PersistentDataType.INTEGER, 0);
        int sellPrice = data.getOrDefault(new NamespacedKey(plugin, "sellPrice"), PersistentDataType.INTEGER, 0);
        int quantity = data.getOrDefault(new NamespacedKey(plugin, "quantity"), PersistentDataType.INTEGER, 1);
        String shopItemName = data.getOrDefault(new NamespacedKey(plugin, "shopItem"), PersistentDataType.STRING, Material.STONE.name());
        ItemStack shopItem = getShopItem(data);

        // Identify which control was clicked based on slot
        switch (slot) {
            case 37: // Decrease Buy
                if(event.isShiftClick()){
                    buyPrice = Math.max(0, buyPrice - 16);
                } else {
                    buyPrice = Math.max(0, buyPrice - 1);
                }
                break;
            case 39: // Increase Buy
                if(event.isShiftClick()){
                    buyPrice += 16;
                } else {
                    buyPrice += 1;
                }
                break;
            case 28: // Decrease Sell
                if(event.isShiftClick()){
                    sellPrice = Math.max(0, sellPrice - 16);
                } else {
                    sellPrice = Math.max(0, sellPrice - 1);
                }
                break;
            case 30: // Increase Sell
                if(event.isShiftClick()){
                    sellPrice = + 16;
                } else {
                    sellPrice += 1;
                }
                break;
            case 19: // Decrease Quantity
                if(event.isShiftClick()){
                    quantity = Math.max(0, quantity - 16);
                } else {
                    quantity = Math.max(1, quantity - 1);
                }
                break;
            case 21: // Increase Quantity
                if(event.isShiftClick()){
                    quantity += 16;
                } else {
                    quantity += 1;
                }

                if(quantity > 1024)
                    quantity = 1024;
                break;
            case 43:
                shopItemName = event.getCursor().getType().name();
                shopItem = event.getCursor();
                break;
            default:
                // No other relevant slots for adjustments
                return;
        }

        // Save updated values
        data.set(new NamespacedKey(plugin, "buyPrice"), PersistentDataType.INTEGER, buyPrice);
        data.set(new NamespacedKey(plugin, "sellPrice"), PersistentDataType.INTEGER, sellPrice);
        data.set(new NamespacedKey(plugin, "quantity"), PersistentDataType.INTEGER, quantity);
        data.set(new NamespacedKey(plugin, "shopItem"), PersistentDataType.STRING, shopItemName);
        String itemStackBase64 = InventoryHelper.itemStackArrayToBase64(new ItemStack[]{shopItem});
        data.set(new NamespacedKey(plugin, "itemStack"), PersistentDataType.STRING, itemStackBase64);

        // Update the display items in the inventory
        Inventory inv = event.getView().getTopInventory();
        updateManagementInventory(inv, buyPrice, sellPrice, quantity, shopItem);

        // Update the sign lines
        Material mat = Material.getMaterial(shopItemName.toUpperCase());
        if (mat == null) mat = Material.STONE;
        updateSignText(sign, player.getName(), buyPrice, sellPrice, quantity, mat);
    }

    private void updateManagementInventory(Inventory inv, int buyPrice, int sellPrice, int quantity, ItemStack shopItem) {
        // Update buy price display
        ItemStack buyItem = inv.getItem(38);
        if (buyItem != null && buyItem.getType() == Material.IRON_NUGGET) {
            ItemMeta bpMeta = buyItem.getItemMeta();
            bpMeta.displayName(Chat.greenFade("Buy Price: " + buyPrice));
            buyItem.setItemMeta(bpMeta);
        }

        // Sell price display
        ItemStack sellItem = inv.getItem(29);
        if (sellItem != null && sellItem.getType() == Material.IRON_NUGGET) {
            ItemMeta spMeta = sellItem.getItemMeta();
            spMeta.displayName(Chat.redFade("Sell Price: " + sellPrice));
            sellItem.setItemMeta(spMeta);
        }

        // Quantity display
        ItemStack qtyItem = inv.getItem(20);
        if (qtyItem != null && qtyItem.getType() == Material.PAPER) {
            ItemMeta qMeta = qtyItem.getItemMeta();
            qMeta.displayName(Chat.blueFade("Quantity: " + quantity));
            qtyItem.setItemMeta(qMeta);
        }

        // Current Item display
        ItemStack currentItem = inv.getItem(42);
        if (currentItem != null) {
            inv.setItem(42, shopItem);
        }
    }

    private void handleWithdrawSilver(Player player, PersistentDataContainer data, Sign sign) {
        int silverVault = data.getOrDefault(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER, 0);
        int silverToWithdraw = Math.min(16, silverVault);

        if (silverToWithdraw > 0) {
            data.set(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER, silverVault - silverToWithdraw);
            sign.update();
            ItemStack silverItem = OraxenItems.getItemById("terranova_silver").build();
            silverItem.setAmount(silverToWithdraw);
            player.getInventory().addItem(silverItem);
            Chat.sendSuccessMessage(player, "Du hast " + silverToWithdraw + " Silber abgehoben.");
        } else {
            Chat.sendErrorMessage(player, "Nicht genug Silber im Tresor.");
        }
    }

    private void handleDepositSilver(Player player, PersistentDataContainer data, Sign sign) {
        int playerSilver = countCustomItems(player.getInventory(), OraxenItems.getItemById("terranova_silver").build());
        int silverToDeposit = Math.min(16, playerSilver);

        if (silverToDeposit > 0) {
            removeCustomItems(player.getInventory(), OraxenItems.getItemById("terranova_silver").build(), silverToDeposit);
            int silverVault = data.getOrDefault(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER, 0);
            data.set(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER, silverVault + silverToDeposit);
            sign.update();
            Chat.sendSuccessMessage(player, "Du hast " + silverToDeposit + " Silber in den Tresor eingezahlt.");
        } else {
            Chat.sendErrorMessage(player, "Du hast nicht genug Silber zum Einzahlen.");
        }
    }

    private void handleOpenChest(Player player, PersistentDataContainer data) {
        Block chestBlock = getChestBlock(data, player.getWorld());
        if (!(chestBlock.getState() instanceof Chest chest)) {
            Chat.sendErrorMessage(player, "Keine Truhe für diesen Shop gefunden!");
            return;
        }
        player.openInventory(chest.getInventory());
    }

    private void updateSilverDisplay(PersistentDataContainer data, Inventory inventory) {
        int silverCount = data.getOrDefault(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER, 0);
        ItemStack silver = OraxenItems.getItemById("terranova_silver").build();
        ItemMeta silverMeta = silver.getItemMeta();
        silverMeta.displayName(Chat.yellowFade("Silber: " + silverCount));
        silver.setItemMeta(silverMeta);
        inventory.setItem(11, silver);
    }

    private int countCustomItems(Inventory inventory, ItemStack customItem) {
        int count = 0;
        for (ItemStack i : inventory.getContents()) {
            if (i != null && isSameCustomItem(i, customItem)) {
                count += i.getAmount();
            }
        }
        return count;
    }

    private void removeCustomItems(Inventory inventory, ItemStack customItem, int quantity) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null) continue;
            if (isSameCustomItem(stack, customItem)) {
                int stackAmount = stack.getAmount();
                if (stackAmount <= quantity) {
                    inventory.clear(i);
                    quantity -= stackAmount;
                    if (quantity <= 0) break;
                } else {
                    stack.setAmount(stackAmount - quantity);
                    inventory.setItem(i, stack);
                    break;
                }
            }
        }
    }

    private boolean isSameCustomItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }
        if (item1.getType() != item2.getType()) {
            return false;
        }
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();
        if (meta1 == null || meta2 == null) {
            return false;
        }
        // Compare display names, enchants, and flags
        return meta1.getDisplayName().equals(meta2.getDisplayName()) &&
                meta1.getEnchants().equals(meta2.getEnchants()) &&
                meta1.getItemFlags().equals(meta2.getItemFlags());
    }

    private Block getChestBlock(PersistentDataContainer data, World world) {
        int chestX = data.getOrDefault(new NamespacedKey(plugin, "chestX"), PersistentDataType.INTEGER, 0);
        int chestY = data.getOrDefault(new NamespacedKey(plugin, "chestY"), PersistentDataType.INTEGER, 0);
        int chestZ = data.getOrDefault(new NamespacedKey(plugin, "chestZ"), PersistentDataType.INTEGER, 0);
        return world.getBlockAt(chestX, chestY, chestZ);
    }

    private String buildPriceString(int buyPrice, int sellPrice) {
        String buyPart = (buyPrice == -1) ? "-" : "B: " + buyPrice;
        String sellPart = (sellPrice == -1) ? "-" : "S: " + sellPrice;
        return buyPart + " | " + sellPart;
    }

    private void updateSignText(Sign sign, String playerName, int buyPrice, int sellPrice, int quantity, Material material) {
        String fullPricesString = buildPriceString(buyPrice, sellPrice);
        sign.lines().set(0, Chat.stringToComponent(playerName));
        sign.lines().set(1, Chat.stringToComponent(fullPricesString));
        sign.lines().set(2, Chat.stringToComponent(material.name()));
        sign.lines().set(3, Chat.stringToComponent(String.valueOf(quantity)));
        sign.update();
    }

    private ItemStack getShopItem(PersistentDataContainer data) {
        String itemStackBase64 = data.get(new NamespacedKey(plugin, "itemStack"), PersistentDataType.STRING);
        int quantity = data.getOrDefault(new NamespacedKey(plugin, "quantity"), PersistentDataType.INTEGER, 1);
        ItemStack[] items = InventoryHelper.itemStackArrayFromBase64(itemStackBase64);
        items[0].setAmount(quantity);
        return items[0];
    }
}
