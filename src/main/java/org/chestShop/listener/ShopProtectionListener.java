package org.chestShop.listener;

import de.mcterranova.terranovaLib.utils.Chat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.chestShop.ChestShop;

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
                Chat.sendErrorMessage(event.getPlayer(), "You cannot place hoppers near shop chests.");
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
                    Chat.sendErrorMessage(player, "You do not own this shop.");
                }
            }
        }
        else if (block.getState() instanceof Chest chest) {
            boolean hasShopSign;
            if (chest.getInventory() instanceof DoubleChestInventory) {
                DoubleChest doubleChest = (DoubleChest) chest.getInventory().getHolder();
                Chest leftChest = (Chest) doubleChest.getLeftSide();
                Chest rightChest = (Chest) doubleChest.getRightSide();

                hasShopSign = isShopSignAttached(leftChest) || isShopSignAttached(rightChest);
            } else {
                hasShopSign = isShopSignAttached(chest);
            }

            if (!hasShopSign) {
                return;
            }

            Player player = event.getPlayer();
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
                Chat.sendErrorMessage(player, "Du besitzt diesen Shop nicht!");
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

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {
        for (BlockState state : event.getBlocks()) {
            Block block = state.getBlock();
            if (block.getState() instanceof Chest || block.getState() instanceof Sign) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean isOwner(Chest chest, Player player) {
        Sign attachedSignBlock = getAttachedSign(chest.getBlock());
        if (attachedSignBlock == null || !isShopSign(attachedSignBlock)) {
            return false;
        }

        PersistentDataContainer data = attachedSignBlock.getPersistentDataContainer();
        UUID ownerUUID = UUID.fromString(data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));

        return player.getUniqueId().equals(ownerUUID);
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

    private boolean isShopSignAttached(Chest chest) {
        Sign attachedSignBlock = getAttachedSign(chest.getBlock());
        return attachedSignBlock != null && isShopSign(attachedSignBlock);
    }

    private Sign getAttachedSign(Block chestBlock) {
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
                    return sign;
                }
            }
        }

        return null;
    }

    private boolean isShopSign(Sign sign) {
        if (sign == null)
            return false;
        PersistentDataContainer data = sign.getPersistentDataContainer();
        return data.has(new NamespacedKey(plugin, "shopItem"));
    }

    private boolean isShopOwner(Sign sign, Player player) {
        PersistentDataContainer data = sign.getPersistentDataContainer();
        UUID ownerUUID = UUID.fromString(data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));
        return player.getUniqueId().equals(ownerUUID);
    }
}
