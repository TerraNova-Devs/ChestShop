package org.chestShop.helper;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.chestShop.utils.ChatUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

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

    public static String itemStackArrayToBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack[] itemStackArrayFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load item stacks.", e);
        }
    }
}
