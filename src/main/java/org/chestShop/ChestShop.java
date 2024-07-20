package org.chestShop;

import org.bukkit.plugin.java.JavaPlugin;
import org.chestShop.listener.*;

public final class ChestShop extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new ShopInventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopDeletionListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopCreationListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestAccessListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestProtectionListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
