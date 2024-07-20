package org.chestShop.utils.silver;

import org.bukkit.Material;

public enum State {
    NUGGET(Material.IRON_NUGGET),
    INGOT(Material.IRON_INGOT),
    BLOCK(Material.IRON_BLOCK);

    public Material material;

    State(Material material) {
        this.material = material;
    }

    public Material getMaterial() {
        return material;
    }
}