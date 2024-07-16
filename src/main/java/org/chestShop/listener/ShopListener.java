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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.chestShop.ChestShop;
import org.chestShop.helper.InventoryHelper;
import org.chestShop.utils.ChatUtils;

import java.util.HashMap;
import java.util.List;
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
            player.sendMessage(ChatUtils.returnRedFade("No chest found for this shop!"));
            return;
        }

        if (isOwner) {
            handleOwnerInteract(event, player, sign, data, chest);
        } else {
            handleCustomerInteract(event, player, sign, data, chest);
        }
    }

    private boolean isShopSign(Sign sign) {
        return ((TextComponent) sign.getSide(Side.FRONT).lines().getFirst()).content().equalsIgnoreCase("[Shop]");
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
            Inventory shopInventory = InventoryHelper.createStyledInventory(27, "Owner Shop");
            addIronNuggetsToInventory(data, shopInventory);
            addChestContentsToInventory(shopInventory);
            player.openInventory(shopInventory);
            player.setMetadata("shopSign", new FixedMetadataValue(plugin, sign.getLocation()));
        }
    }

    private void addIronNuggetsToInventory(PersistentDataContainer data, Inventory shopInventory) {
        int ironNuggetCount = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
        ItemStack ironNuggets = (ironNuggetCount <= 0) ? new ItemStack(Material.BARRIER, 1) : new ItemStack(Material.IRON_NUGGET, ironNuggetCount);
        ItemMeta meta = ironNuggets.getItemMeta();
        meta.displayName(ChatUtils.returnYellowFade("Earned Iron Nuggets"));
        ironNuggets.setItemMeta(meta);
        shopInventory.setItem(10, ironNuggets);
    }

    private void addChestContentsToInventory(Inventory shopInventory) {
        ItemStack chestContents = new ItemStack(Material.CHEST);
        ItemMeta meta = chestContents.getItemMeta();
        meta.displayName(ChatUtils.returnGreenFade("Manage Chest Contents"));
        chestContents.setItemMeta(meta);
        shopInventory.setItem(16, chestContents);
    }

    private void handleCustomerInteract(PlayerInteractEvent event, Player player, Sign sign, PersistentDataContainer data, Chest chest) {
        ItemStack shopItem = getShopItem(data);
        int buyPrice = data.getOrDefault(new NamespacedKey(plugin, "buyPrice"), PersistentDataType.INTEGER, 0);
        ItemStack paymentItem = new ItemStack(Material.IRON_NUGGET, buyPrice);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!chest.getInventory().containsAtLeast(shopItem, 1)) {
                player.sendMessage(ChatUtils.returnRedFade("The chest does not contain the shop item."));
                return;
            }

            chest.getInventory().removeItem(shopItem);
            if (!player.getInventory().containsAtLeast(paymentItem, paymentItem.getAmount())) {
                player.sendMessage(ChatUtils.returnRedFade("You do not have enough payment items."));
                chest.getInventory().addItem(shopItem);
                return;
            }

            player.getInventory().removeItem(paymentItem);
            if (!player.getInventory().addItem(shopItem).isEmpty()) {
                player.sendMessage(ChatUtils.returnRedFade("Failed to add the item to your inventory. Please check your inventory space."));
                chest.getInventory().addItem(shopItem);
            } else {
                player.sendMessage(ChatUtils.returnGreenFade("You bought an item for " + buyPrice + " iron nuggets."));
                updateEarnings(data, buyPrice);
                sign.update();  // Ensures sign is updated after earnings change
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Sell Logic
        }
    }

    private ItemStack getShopItem(PersistentDataContainer data) {
        return new ItemStack(Material.valueOf(data.get(new NamespacedKey(plugin, "shopItem"), PersistentDataType.STRING)));
    }

    private void updateEarnings(PersistentDataContainer data, int buyPrice) {
        int currentEarnings = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
        data.set(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, currentEarnings + buyPrice);
    }
}
