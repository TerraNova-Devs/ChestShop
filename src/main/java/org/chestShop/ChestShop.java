package org.chestShop;

import org.bukkit.plugin.java.JavaPlugin;
import org.chestShop.listener.ShopCreationListener;
import org.chestShop.listener.ShopListener;

public final class ChestShop extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new ShopCreationListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
