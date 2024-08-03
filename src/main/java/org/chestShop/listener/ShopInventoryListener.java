package org.chestShop.listener;

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
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.chestShop.ChestShop;
import org.chestShop.utils.ChatUtils;
import org.chestShop.utils.silver.SilverManager;

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
        if (metadataValues.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        // Ensure GUI items cannot be moved
        int slot = event.getRawSlot();
        if (slot < event.getView().getTopInventory().getSize() && isGuiItem(event.getCurrentItem())) {
            handleInventoryClick(event, player, metadataValues.get(0).value());
        }
    }

    private boolean isGuiItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        return type == Material.RED_WOOL || type == Material.IRON_NUGGET || type == Material.GREEN_WOOL || type == Material.GRAY_STAINED_GLASS_PANE || type == Material.CHEST;
    }

    private void handleInventoryClick(InventoryClickEvent event, Player player, Object metadataValue) {
        Location signLocation = (Location) metadataValue;
        Block signBlock = signLocation.getBlock();

        if (!(signBlock.getState() instanceof Sign sign)) {
            return;
        }

        PersistentDataContainer data = sign.getPersistentDataContainer();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem.getType() == Material.RED_WOOL) {
            int silverVault = data.getOrDefault(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER, 0);
            int silverToWithdraw = Math.min(16, silverVault);

            if (silverToWithdraw > 0) {
                data.set(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER, silverVault - silverToWithdraw);
                sign.update();
                ItemStack silverItem = SilverManager.get().placeholder();
                silverItem.setAmount(silverToWithdraw);
                player.getInventory().addItem(silverItem);
                ChatUtils.sendSuccessMessage(player, "Du hast " + silverToWithdraw + " Silber abgehoben.");
            } else {
                ChatUtils.sendErrorMessage(player, "Nicht genug Silber im Tresor.");
            }
        } else if (clickedItem.getType() == Material.GREEN_WOOL) {
            int playerSilver = countCustomItems(player.getInventory(), SilverManager.get().placeholder());
            int silverToDeposit = Math.min(16, playerSilver);

            if (silverToDeposit > 0) {
                removeCustomItems(player.getInventory(), SilverManager.get().placeholder(), silverToDeposit);
                int silverVault = data.getOrDefault(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER, 0);
                data.set(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER, silverVault + silverToDeposit);
                sign.update();
                ChatUtils.sendSuccessMessage(player, "Du hast " + silverToDeposit + " Silber in den Tresor eingezahlt.");
            } else {
                ChatUtils.sendErrorMessage(player, "Du hast nicht genug Silber zum Einzahlen.");
            }
        } else if (clickedItem.getType() == Material.CHEST){
            Block chestBlock = getChestBlock(data, player.getWorld());
            if (!(chestBlock.getState() instanceof Chest chest)) {
                ChatUtils.sendErrorMessage(player, "Keine Truhe für diesen Shop gefunden!");
                return;
            }
            player.openInventory(chest.getInventory());
        }

        // Update the silver display in the GUI
        updateSilverDisplay(data, event.getInventory());
    }

    private void updateSilverDisplay(PersistentDataContainer data, Inventory inventory) {
        int silverCount = data.getOrDefault(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER, 0);
        ItemStack silver = SilverManager.get().placeholder();
        ItemMeta silverMeta = silver.getItemMeta();
        silverMeta.displayName(ChatUtils.returnYellowFade("Silber: " + silverCount));
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
                } else {
                    stack.setAmount(stackAmount - quantity);
                    inventory.setItem(i, stack);
                    return;
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
        return meta1.getDisplayName().equals(meta2.getDisplayName()) && meta1.getEnchants().equals(meta2.getEnchants()) && meta1.getItemFlags().equals(meta2.getItemFlags());
    }

    private Block getChestBlock(PersistentDataContainer data, World world) {
        int chestX = data.getOrDefault(new NamespacedKey(plugin, "chestX"), PersistentDataType.INTEGER, 0);
        int chestY = data.getOrDefault(new NamespacedKey(plugin, "chestY"), PersistentDataType.INTEGER, 0);
        int chestZ = data.getOrDefault(new NamespacedKey(plugin, "chestZ"), PersistentDataType.INTEGER, 0);
        return world.getBlockAt(chestX, chestY, chestZ);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (player.hasMetadata("shopSign")) {
            player.removeMetadata("shopSign", plugin);
        }
    }
}
