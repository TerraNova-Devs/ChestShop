package org.chestShop.listener;

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
            player.sendMessage(ChatUtils.returnRedFade("You do not own this shop!"));
        } else {
            dropEarnings(event, data);
            player.sendMessage(ChatUtils.returnYellowFade("You removed this shop!"));
        }
    }

    private boolean isShopSign(Sign sign) {
        return ((TextComponent) sign.getSide(Side.FRONT).lines().getFirst()).content().equalsIgnoreCase("[Shop]");
    }

    private void dropEarnings(BlockBreakEvent event, PersistentDataContainer data) {
        Integer ironNuggetsCount = data.get(new NamespacedKey(plugin, "ironNuggetsVault"), PersistentDataType.INTEGER);
        if (ironNuggetsCount != null && ironNuggetsCount > 0) {
            ItemStack ironNuggets = new ItemStack(Material.IRON_NUGGET, ironNuggetsCount);
            event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), ironNuggets);
        }
    }
}
