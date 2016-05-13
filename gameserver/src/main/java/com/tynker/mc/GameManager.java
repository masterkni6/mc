package com.tynker.mc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Block;


public class GameManager extends JavaPlugin implements Listener{
   
    private int gameDimension;
    private List<>;

 
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }
    
    
    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
    }
    
}
