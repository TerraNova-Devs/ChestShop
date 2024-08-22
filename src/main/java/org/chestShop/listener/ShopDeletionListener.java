package org.chestShop.listener;

import io.th0rgal.oraxen.api.OraxenItems;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.chestShop.ChestShop;
import org.chestShop.utils.ChatUtils;
import org.chestShop.utils.silver.SilverManager;

import java.util.UUID;

public class ShopDeletionListener implements Listener {
    private final ChestShop plugin;

    public ShopDeletionListener(ChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign sign) || !isShopSign(sign)) {
            return;
        }

        Player player = event.getPlayer();
        PersistentDataContainer data = sign.getPersistentDataContainer();
        UUID ownerUUID = UUID.fromString(data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));
        if (!player.getUniqueId().equals(ownerUUID)) {
            event.setCancelled(true);
            ChatUtils.sendErrorMessage(player, "Du besitzt diesen Shop nicht!");
        } else {
            dropEarnings(event, data);
            ChatUtils.sendSuccessMessage(player, "Du hast diesen Shop entfernt!");
        }
    }

    private boolean isShopSign(Sign sign) {
        PersistentDataContainer data = sign.getPersistentDataContainer();
        return data.has(new NamespacedKey(plugin, "shopItem"));
    }

    private void dropEarnings(BlockBreakEvent event, PersistentDataContainer data) {
        Integer silverCount = data.get(new NamespacedKey(plugin, "silverVault"), PersistentDataType.INTEGER);
        if (silverCount != null && silverCount > 0) {
            ItemStack silver = OraxenItems.getItemById("terranova_silver").build();
            silver.setAmount(silverCount);
            event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), silver);
        }
    }
}
