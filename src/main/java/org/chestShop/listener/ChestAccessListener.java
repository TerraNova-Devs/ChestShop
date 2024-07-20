package org.chestShop.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.chestShop.ChestShop;
import org.chestShop.utils.ChatUtils;

import java.util.UUID;

public class ChestAccessListener implements Listener {

    private final ChestShop plugin;

    public ChestAccessListener(ChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Chest)) {
            return;
        }

        Chest chest = (Chest) block.getState();
        Player player = event.getPlayer();

        // Check if a sign is attached to either part of the chest
        boolean hasAttachedSign = isSignAttached(chest);

        if (chest.getInventory() instanceof DoubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest) chest.getInventory().getHolder();
            Chest leftChest = (Chest) doubleChest.getLeftSide();
            Chest rightChest = (Chest) doubleChest.getRightSide();

            hasAttachedSign = isSignAttached(leftChest) || isSignAttached(rightChest);
        }

        // If no sign is attached, allow access
        if (!hasAttachedSign) {
            return;
        }

        // If a sign is attached, check ownership
        boolean isOwner = isOwner(chest, player);

        if (chest.getInventory() instanceof DoubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest) chest.getInventory().getHolder();
            Chest leftChest = (Chest) doubleChest.getLeftSide();
            Chest rightChest = (Chest) doubleChest.getRightSide();

            if (!isOwner) {
                isOwner = isOwner(leftChest, player) || isOwner(rightChest, player);
            }
        }

        if (!isOwner) {
            event.setCancelled(true);
            ChatUtils.sendErrorMessage(player, "Du besitzt diesen Shop nicht!");
        }
    }

    private boolean isOwner(Chest chest, Player player) {
        Block attachedSignBlock = getAttachedSign(chest.getBlock());
        if (attachedSignBlock == null || !(attachedSignBlock.getState() instanceof Sign sign) || !isShopSign(sign)) {
            return false;
        }

        PersistentDataContainer data = sign.getPersistentDataContainer();
        UUID ownerUUID = UUID.fromString(data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));

        return player.getUniqueId().equals(ownerUUID);
    }

    private boolean isSignAttached(Chest chest) {
        Block attachedSignBlock = getAttachedSign(chest.getBlock());
        return attachedSignBlock != null && attachedSignBlock.getState() instanceof Sign;
    }

    private Block getAttachedSign(Block chestBlock) {
        Block[] possibleSignPositions = {
                chestBlock.getRelative(1, 0, 0),
                chestBlock.getRelative(-1, 0, 0),
                chestBlock.getRelative(0, 0, 1),
                chestBlock.getRelative(0, 0, -1)
        };

        for (Block block : possibleSignPositions) {
            if (block.getState() instanceof Sign sign && sign.getBlockData() instanceof org.bukkit.block.data.type.WallSign) {
                org.bukkit.block.data.type.WallSign wallSign = (org.bukkit.block.data.type.WallSign) sign.getBlockData();
                Block attached = block.getRelative(wallSign.getFacing().getOppositeFace());
                if (attached.equals(chestBlock)) {
                    return block;
                }
            }
        }

        return null;
    }

    private boolean isShopSign(Sign sign) {
        PersistentDataContainer data = sign.getPersistentDataContainer();
        return data.has(new NamespacedKey(plugin, "shopItem"));
    }
}
