package org.chestShop;

import org.bukkit.plugin.java.JavaPlugin;
import org.chestShop.commands.GenerateSilverCommand;
import org.chestShop.listener.*;
import org.chestShop.utils.silver.SilverManager;

public final class ChestShop extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        SilverManager.init();

        getServer().getPluginManager().registerEvents(new ShopInventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopDeletionListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopCreationListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestAccessListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopProtectionListener(this), this);

        // Register commands
        this.getCommand("generatesilver").setExecutor(new GenerateSilverCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
