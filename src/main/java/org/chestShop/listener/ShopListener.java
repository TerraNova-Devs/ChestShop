package org.chestShop.listener;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.enchantments.Enchantment;
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
        if (block != null && (block.getState() instanceof Sign)) {
            Sign sign = (Sign) block.getState();
            if (((TextComponent) sign.getSide(Side.FRONT).lines().getFirst()).content().equalsIgnoreCase("[Shop]")) {
                Player player = event.getPlayer();
                PersistentDataContainer data = sign.getPersistentDataContainer();

                UUID ownerUUID = UUID.fromString(data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));
                boolean isOwner = player.getUniqueId().equals(ownerUUID);

                int chestX = data.getOrDefault(new NamespacedKey(plugin, "chestX"), PersistentDataType.INTEGER, 0);
                int chestY = data.getOrDefault(new NamespacedKey(plugin, "chestY"), PersistentDataType.INTEGER, 0);
                int chestZ = data.getOrDefault(new NamespacedKey(plugin, "chestZ"), PersistentDataType.INTEGER, 0);

                Block chestBlock = player.getWorld().getBlockAt(chestX, chestY, chestZ);
                if (chestBlock.getState() instanceof Chest) {
                    Chest chest = (Chest) chestBlock.getState();
                    Inventory chestInventory = chest.getInventory();

                    int buyPrice = data.getOrDefault(new NamespacedKey(plugin, "buyPrice"), PersistentDataType.INTEGER, 0);
                    int sellPrice = data.getOrDefault(new NamespacedKey(plugin, "sellPrice"), PersistentDataType.INTEGER, 0);

                    if (isOwner) {
                        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            Inventory shopInventory = InventoryHelper.createStyledInventory(27, "Owner Shop");
                            int ironNuggetCount = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
                            ItemStack ironNuggets;
                            if (ironNuggetCount <= 0) {
                                ironNuggets = new ItemStack(Material.BARRIER, 1);
                            } else {
                                ironNuggets = new ItemStack(Material.IRON_NUGGET, ironNuggetCount);
                            }
                            ItemMeta meta = ironNuggets.getItemMeta();
                            meta.displayName(ChatUtils.returnYellowFade("Earned Iron Nuggets"));
                            ironNuggets.setItemMeta(meta);
                            shopInventory.setItem(10, ironNuggets);

                            ItemStack chestContents = new ItemStack(Material.CHEST);
                            meta = chestContents.getItemMeta();
                            meta.displayName(ChatUtils.returnGreenFade("Manage Chest Contents"));
                            chestContents.setItemMeta(meta);
                            shopInventory.setItem(16, chestContents);
                            player.openInventory(shopInventory);
                            player.setMetadata("shopSign", new FixedMetadataValue(plugin, sign.getLocation()));
                        }
                    } else {
                        ItemStack shopItem = ItemStack.of(Material.getMaterial(data.get(new NamespacedKey(plugin, "shopItem"), PersistentDataType.STRING)), 1);

                        ItemStack paymentItem = new ItemStack(Material.IRON_NUGGET, buyPrice);
                        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            if (chest.getInventory().containsAtLeast(shopItem, 1)) {
                                chest.getInventory().removeItem(shopItem);
                                if (player.getInventory().containsAtLeast(paymentItem, paymentItem.getAmount())) {
                                    player.getInventory().removeItem(paymentItem);

                                    HashMap<Integer, ItemStack> remainingItems = player.getInventory().addItem(shopItem);
                                    if (remainingItems.isEmpty()) {
                                        player.sendMessage(ChatUtils.returnGreenFade("You bought an item for " + buyPrice + " iron nuggets."));
                                        int currentEarnings = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
                                        data.set(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, currentEarnings + buyPrice);
                                        sign.update();
                                    } else {
                                        player.sendMessage(ChatUtils.returnRedFade("Failed to add the item to your inventory. Please check your inventory space."));
                                        chest.getInventory().addItem(shopItem);
                                    }
                                } else {
                                    player.sendMessage(ChatUtils.returnRedFade("You do not have enough payment items."));
                                    chest.getInventory().addItem(shopItem);
                                }
                            } else {
                                player.sendMessage(ChatUtils.returnRedFade("The chest does not contain the shop item."));
                            }
                        } else if(event.getAction() == Action.LEFT_CLICK_BLOCK){
                            // Sell Logic
                        }
                    }
                } else {
                    player.sendMessage(ChatUtils.returnRedFade("No chest found for this shop!"));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem == null || !player.hasMetadata("shopSign")) {
            return;
        }

        List<MetadataValue> metadataValues = player.getMetadata("shopSign");
        if (metadataValues.isEmpty()) {
            return;
        }

        if(event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        Location signLocation = (Location) metadataValues.get(0).value();
        Block signBlock = signLocation.getBlock();

        if (signBlock.getState() instanceof Sign sign) {
            PersistentDataContainer data = sign.getPersistentDataContainer();
            ItemStack shopItem = ItemStack.of(Material.getMaterial(data.get(new NamespacedKey(plugin, "shopItem"), PersistentDataType.STRING)), 1);

            UUID ownerUUID = UUID.fromString(data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));
            boolean isOwner = player.getUniqueId().equals(ownerUUID);

            if (isOwner && currentItem.getType() == Material.IRON_NUGGET) {
                int ironNuggetsVault = data.getOrDefault(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, 0);
                int nuggetsToWithdraw = currentItem.getAmount();

                if (nuggetsToWithdraw <= ironNuggetsVault) {
                    data.set(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER, ironNuggetsVault - nuggetsToWithdraw);
                    sign.update();
                    player.getInventory().addItem(new ItemStack(Material.IRON_NUGGET, nuggetsToWithdraw));
                    player.sendMessage(ChatUtils.returnGreenFade("You have withdrawn " + nuggetsToWithdraw + " iron nuggets."));
                } else {
                    player.sendMessage(ChatUtils.returnRedFade("Not enough iron nuggets earned."));
                }


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
