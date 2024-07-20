package org.chestShop.listener;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.chestShop.ChestShop;
import org.chestShop.utils.ChatUtils;

import java.util.List;
import java.util.UUID;

public class ShopProtectionListener implements Listener {

    private final ChestShop plugin;

    public ShopProtectionListener(ChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.HOPPER) {
            Block blockBelow = event.getBlock().getRelative(BlockFace.DOWN);
            Block blockAbove = event.getBlock().getRelative(BlockFace.UP);
            if ((blockBelow.getState() instanceof Chest chestBelow && isShopChest(chestBelow)) ||
                    (blockAbove.getState() instanceof Chest chestAbove && isShopChest(chestAbove))) {
                event.setCancelled(true);
                ChatUtils.sendErrorMessage(event.getPlayer(), "You cannot place hoppers near shop chests.");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof Sign sign) {
            if (isShopSign(sign)) {
                Player player = event.getPlayer();
                if (!isShopOwner(sign, player)) {
                    event.setCancelled(true);
                    ChatUtils.sendErrorMessage(player, "You do not own this shop.");
                }
            }
        } else if (block.getState() instanceof Chest chest) {
            if (isShopChest(chest)) {
                Player player = event.getPlayer();
                if (!isShopOwner(chest, player)) {
                    event.setCancelled(true);
                    ChatUtils.sendErrorMessage(player, "You do not own this shop.");
                }
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        List<Block> blocks = event.blockList();
        for (Block block : blocks) {
            if ((block.getState() instanceof Chest && isShopChest((Chest) block.getState())) || (block.getState() instanceof Sign && isShopSign((Sign) block.getState()))) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntityType() == EntityType.TNT || event.getEntityType() == EntityType.CREEPER) {
            List<Block> blocks = event.blockList();
            for (Block block : blocks) {
                if ((block.getState() instanceof Chest && isShopChest((Chest) block.getState())) || (block.getState() instanceof Sign && isShopSign((Sign) block.getState()))) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (block.getState() instanceof Sign && isShopSign((Sign) block.getState())) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (block.getState() instanceof Sign && isShopSign((Sign) block.getState())) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getDestination().getType() == InventoryType.HOPPER) {
            if (event.getSource().getHolder() instanceof Chest chest && isShopChest(chest)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (event.getVehicle().getType() == EntityType.HOPPER_MINECART) {
            Block block = event.getTo().getBlock().getRelative(BlockFace.DOWN);
            if(block.getState() instanceof Chest chest && isShopChest(chest)) {
                event.getVehicle().remove();
            }
        }
    }

    private boolean isShopSign(Sign sign) {
        PersistentDataContainer data = sign.getPersistentDataContainer();
        return data.has(new NamespacedKey(plugin, "shopItem"));
    }

    private boolean isShopChest(Chest chest) {
        if (chest.getInventory() instanceof DoubleChest doubleChest) {
            Chest leftChest = (Chest) doubleChest.getLeftSide();
            Chest rightChest = (Chest) doubleChest.getRightSide();
            return isShopChestBlock(leftChest.getBlock()) || isShopChestBlock(rightChest.getBlock());
        } else {
            return isShopChestBlock(chest.getBlock());
        }
    }

    private boolean isShopChestBlock(Block block) {
        for (BlockFace face : BlockFace.values()) {
            Block relative = block.getRelative(face);
            if (relative.getState() instanceof Sign sign && isShopSign(sign)) {
                return true;
            }
        }
        return false;
    }

    private boolean isShopOwner(Sign sign, Player player) {
        PersistentDataContainer data = sign.getPersistentDataContainer();
        UUID ownerUUID = UUID.fromString(data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));
        return player.getUniqueId().equals(ownerUUID);
    }

    private boolean isShopOwner(Chest chest, Player player) {
        if (chest.getInventory() instanceof DoubleChest doubleChest) {
            Chest leftChest = (Chest) doubleChest.getLeftSide();
            Chest rightChest = (Chest) doubleChest.getRightSide();
            return isShopOwner(leftChest.getBlock(), player) || isShopOwner(rightChest.getBlock(), player);
        } else {
            return isShopOwner(chest.getBlock(), player);
        }
    }

    private boolean isShopOwner(Block block, Player player) {
        for (BlockFace face : BlockFace.values()) {
            Block relative = block.getRelative(face);
            if (relative.getState() instanceof Sign sign && isShopSign(sign)) {
                return isShopOwner(sign, player);
            }
        }
        return false;
    }
}
