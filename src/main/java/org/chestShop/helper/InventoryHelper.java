package org.chestShop.helper;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.chestShop.utils.ChatUtils;

public class InventoryHelper {

    public static Inventory createStyledInventory(int size, String title) {
        Inventory inv = Bukkit.createInventory(null, size, ChatUtils.stringToComponent(title));
        styleInventory(inv);
        return inv;
    }

    private static void styleInventory(Inventory inv) {
        ItemStack glassPane = createGlassPane();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isBorderSlot(inv.getSize(), i)) {
                inv.setItem(i, glassPane);
            }
        }
    }

    private static ItemStack createGlassPane() {
        ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glassPane.getItemMeta();
        meta.displayName(ChatUtils.stringToComponent(""));
        glassPane.setItemMeta(meta);
        return glassPane;
    }

    private static boolean isBorderSlot(int size, int slot) {
        int rowLength = 9;
        return slot < rowLength || slot >= size - rowLength || slot % rowLength == 0 || slot % rowLength == rowLength - 1;
    }
}
