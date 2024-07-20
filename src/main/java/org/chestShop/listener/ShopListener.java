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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.chestShop.ChestShop;
import org.chestShop.helper.InventoryHelper;
import org.chestShop.utils.ChatUtils;

import java.util.UUID;

public class ShopListener implements Listener {

    private final ChestShop plugin;

    public ShopListener(ChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign) || !isShopSign(sign)) {
            return;
        }

        Player player = event.getPlayer();
        PersistentDataContainer data = sign.getPersistentDataContainer();
        boolean isOwner = isOwner(player, data);

        Block chestBlock = getChestBlock(data, player.getWorld());
        if (!(chestBlock.getState() instanceof Chest chest)) {
            ChatUtils.sendErrorMessage(player, "Keine Truhe für diesen Shop gefunden!");
            return;
        }

        if (isOwner) {
            handleOwnerInteract(event, player, sign, data, chest);
        } else {
            handleCustomerInteract(event, player, sign, data, chest);
        }
    }

    private boolean isShopSign(Sign sign) {
        PersistentDataContainer data = sign.getPersistentDataContainer();
        return data.has(new NamespacedKey(plugin, "shopItem"));
    }

    private boolean isOwner(Player player, PersistentDataContainer data) {
        UUID ownerUUID = UUID.fromString(data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));
        return player.getUniqueId().equals(ownerUUID);
    }

    private Block getChestBlock(PersistentDataContainer data, World world) {
        int chestX = data.getOrDefault(new NamespacedKey(plugin, "chestX"), PersistentDataType.INTEGER, 0);
        int chestY = data.getOrDefault(new NamespacedKey(plugin, "chestY"), PersistentDataType.INTEGER, 0);
        int chestZ = data.getOrDefault(new NamespacedKey(plugin, "chestZ"), PersistentDataType.INTEGER, 0);
        return world.getBlockAt(chestX, chestY, chestZ);
    }

    private void handleOwnerInteract(PlayerInteractEvent event, Player player, Sign sign, PersistentDataContainer data, Chest chest) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Inventory shopInventory = InventoryHelper.createStyledInventory(27, "Eigener Shop");
            addVaultToInventory(data, shopInventory);
            addChestContentsToInventory(shopInventory);
            player.openInventory(shopInventory);
            player.setMetadata("shopSign", new FixedMetadataValue(plugin, sign.getLocation()));
        }
    }

    private void addVaultToInventory(PersistentDataContainer data, Inventory shopInventory) {
        int ironNuggetCount = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);

        ItemStack withdrawItem = new ItemStack(Material.RED_WOOL);
        ItemMeta withdrawMeta = withdrawItem.getItemMeta();
        withdrawMeta.displayName(ChatUtils.returnRedFade("Bis zu 16 Eisennuggets abheben"));
        withdrawItem.setItemMeta(withdrawMeta);
        shopInventory.setItem(10, withdrawItem);

        ItemStack ironNuggets = new ItemStack(Material.IRON_NUGGET);
        ItemMeta ironNuggetsMeta = ironNuggets.getItemMeta();
        ironNuggetsMeta.displayName(ChatUtils.returnYellowFade("Eisennuggets: " + ironNuggetCount));
        ironNuggets.setItemMeta(ironNuggetsMeta);
        shopInventory.setItem(11, ironNuggets);

        ItemStack depositItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta depositMeta = depositItem.getItemMeta();
        depositMeta.displayName(ChatUtils.returnGreenFade("Bis zu 16 Eisennuggets einzahlen"));
        depositItem.setItemMeta(depositMeta);
        shopInventory.setItem(12, depositItem);
    }

    private void addChestContentsToInventory(Inventory shopInventory) {
        ItemStack chestContents = new ItemStack(Material.CHEST);
        ItemMeta meta = chestContents.getItemMeta();
        meta.displayName(ChatUtils.returnGreenFade("Truheninhalte verwalten"));
        chestContents.setItemMeta(meta);
        shopInventory.setItem(16, chestContents);
    }

    private void handleCustomerInteract(PlayerInteractEvent event, Player player, Sign sign, PersistentDataContainer data, Chest chest) {
        ItemStack shopItem = getShopItem(data).clone();
        int buyPrice = data.getOrDefault(new NamespacedKey(plugin, "buyPrice"), PersistentDataType.INTEGER, 0);
        int sellPrice = data.getOrDefault(new NamespacedKey(plugin, "sellPrice"), PersistentDataType.INTEGER, 0);
        int quantity = data.getOrDefault(new NamespacedKey(plugin, "quantity"), PersistentDataType.INTEGER, 1);
        ItemStack paymentItem = new ItemStack(Material.IRON_NUGGET, buyPrice);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            shopItem.setAmount(quantity);
            if (!containsMatchingItem(chest.getInventory(), shopItem)) {
                ChatUtils.sendErrorMessage(player, "Die Truhe enthält nicht genug Waren.");
                return;
            }

            chest.getInventory().removeItem(shopItem);

            if (!player.getInventory().containsAtLeast(paymentItem, paymentItem.getAmount())) {
                ChatUtils.sendErrorMessage(player, "Du hast nicht genug Zahlungsartikel.");
                chest.getInventory().addItem(shopItem);
                return;
            }

            player.getInventory().removeItem(paymentItem);
            if (!player.getInventory().addItem(shopItem).isEmpty()) {
                ChatUtils.sendErrorMessage(player, "Der Artikel konnte nicht deinem Inventar hinzugefügt werden. Bitte überprüfe deinen Inventarplatz.");
                chest.getInventory().addItem(shopItem);
            } else {
                ChatUtils.sendSuccessMessage(player, "Du hast " + quantity + "x " + shopItem.getType().name() + " für " + buyPrice + " Eisennuggets gekauft.");
                updateEarnings(data, buyPrice);
                sign.update();  // Ensures sign is updated after earnings change
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (sellPrice <= 0) {
                ChatUtils.sendErrorMessage(player, "Dieser Shop kauft keine Artikel.");
                return;
            }

            shopItem.setAmount(quantity);
            int itemCount = countItems(player.getInventory(), shopItem);
            if (itemCount < quantity) {
                ChatUtils.sendErrorMessage(player, "Du hast nicht genug Artikel zu verkaufen.");
                return;
            }

            // Check if the chest has enough space to store the sold items
            if (!hasEnoughSpace(chest.getInventory(), shopItem)) {
                ChatUtils.sendErrorMessage(player, "Die Truhe des Shops ist voll.");
                return;
            }

            // Check if the shop has enough iron nuggets to pay the player
            int ironNuggetVault = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
            if (ironNuggetVault < sellPrice) {
                ChatUtils.sendErrorMessage(player, "Der Shop hat nicht genug Eisennuggets, um dich zu bezahlen.");
                return;
            }

            removeItems(player.getInventory(), shopItem, quantity);
            player.getInventory().addItem(new ItemStack(Material.IRON_NUGGET, sellPrice));
            addItemsToChest(chest.getInventory(), shopItem, quantity);
            data.set(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, ironNuggetVault - sellPrice);
            ChatUtils.sendSuccessMessage(player, "Du hast " + quantity + "x " + shopItem.getType().name() + " für " + sellPrice + " Eisennuggets verkauft.");
            sign.update();  // Ensures sign is updated after earnings change
        }
    }

    private ItemStack getShopItem(PersistentDataContainer data) {
        String itemStackBase64 = data.get(new NamespacedKey(plugin, "itemStack"), PersistentDataType.STRING);
        int quantity = data.getOrDefault(new NamespacedKey(plugin, "quantity"), PersistentDataType.INTEGER, 1);
        ItemStack[] items = InventoryHelper.itemStackArrayFromBase64(itemStackBase64);
        items[0].setAmount(quantity);
        return items[0];
    }

    private boolean containsMatchingItem(Inventory inventory, ItemStack shopItem) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.isSimilar(shopItem) && inventory.containsAtLeast(item, shopItem.getAmount())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEnoughSpace(Inventory inventory, ItemStack shopItem) {
        int space = 0;
        for (ItemStack item : inventory.getStorageContents()) {
            if (item == null) {
                space += shopItem.getMaxStackSize();
            } else if (item.isSimilar(shopItem)) {
                space += item.getMaxStackSize() - item.getAmount();
            }
            if (space >= shopItem.getAmount()) {
                return true;
            }
        }
        return false;
    }

    private void addItemsToChest(Inventory chestInventory, ItemStack shopItem, int quantity) {
        ItemStack singleItem = shopItem.clone();
        singleItem.setAmount(1);

        for (int i = 0; i < quantity; i++) {
            chestInventory.addItem(singleItem.clone());
        }
    }

    private void updateEarnings(PersistentDataContainer data, int buyPrice) {
        int currentEarnings = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
        data.set(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, currentEarnings + buyPrice);
    }

    private int countItems(Inventory inventory, ItemStack item) {
        int count = 0;
        for (ItemStack i : inventory.getContents()) {
            if (i != null && i.isSimilar(item)) {
                count += i.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Inventory inventory, ItemStack item, int quantity) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null) continue;
            if (stack.isSimilar(item)) {
                int stackAmount = stack.getAmount();
                if (stackAmount <= quantity) {
                    inventory.clear(i);
                    quantity -= stackAmount;
                } else {
                    stack.setAmount(stackAmount - quantity);
                    inventory.setItem(i, stack);
                    return;
                }
            }
        }
    }
}
