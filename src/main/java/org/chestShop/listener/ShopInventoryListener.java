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

        // Ensure GUI items cannot be moved
        int slot = event.getRawSlot();
        if (slot < event.getView().getTopInventory().getSize() && isGuiItem(event.getCurrentItem())) {
            event.setCancelled(true);
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
            int ironNuggetsVault = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
            int nuggetsToWithdraw = Math.min(16, ironNuggetsVault);

            if (nuggetsToWithdraw > 0) {
                data.set(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, ironNuggetsVault - nuggetsToWithdraw);
                sign.update();
                player.getInventory().addItem(new ItemStack(Material.IRON_NUGGET, nuggetsToWithdraw));
                ChatUtils.sendSuccessMessage(player, "Du hast " + nuggetsToWithdraw + " Eisennuggets abgehoben.");
            } else {
                ChatUtils.sendErrorMessage(player, "Nicht genug Eisennuggets im Tresor.");
            }
        } else if (clickedItem.getType() == Material.GREEN_WOOL) {
            int playerNuggets = countItems(player.getInventory(), new ItemStack(Material.IRON_NUGGET));
            int nuggetsToDeposit = Math.min(16, playerNuggets);

            if (nuggetsToDeposit > 0) {
                removeItems(player.getInventory(), new ItemStack(Material.IRON_NUGGET), nuggetsToDeposit);
                int ironNuggetsVault = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
                data.set(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, ironNuggetsVault + nuggetsToDeposit);
                sign.update();
                ChatUtils.sendSuccessMessage(player, "Du hast " + nuggetsToDeposit + " Eisennuggets in den Tresor eingezahlt.");
            } else {
                ChatUtils.sendErrorMessage(player, "Du hast nicht genug Eisennuggets zum Einzahlen.");
            }
        } else if (clickedItem.getType() == Material.CHEST){
            Block chestBlock = getChestBlock(data, player.getWorld());
            if (!(chestBlock.getState() instanceof Chest chest)) {
                ChatUtils.sendErrorMessage(player, "Keine Truhe für diesen Shop gefunden!");
                return;
            }
            player.openInventory(chest.getInventory());
        }

        // Update the iron nugget display in the GUI
        updateIronNuggetDisplay(data, event.getInventory());
    }

    private void updateIronNuggetDisplay(PersistentDataContainer data, Inventory inventory) {
        int ironNuggetCount = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
        ItemStack ironNuggets = new ItemStack(Material.IRON_NUGGET);
        ItemMeta ironNuggetsMeta = ironNuggets.getItemMeta();
        ironNuggetsMeta.displayName(ChatUtils.returnYellowFade("Eisennuggets: " + ironNuggetCount));
        ironNuggets.setItemMeta(ironNuggetsMeta);
        inventory.setItem(11, ironNuggets);
    }

    private int countItems(Inventory inventory, ItemStack item) {
        int count = 0;
        for (ItemStack i : inventory.getContents()) {
            if (i != null && i.getType() == item.getType()) {
                count += i.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Inventory inventory, ItemStack item, int quantity) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null) continue;
            if (stack.getType() == item.getType()) {
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
