package com.tynker.mc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;


public class SeverManager extends JavaPlugin implements Listener{
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }
    
    void buildRoom(d,x,y,z){
	    org.bukkit.block.Block block = world.getBlockAt(x,y,z);
	    block = block.getRelative(-.5*d,0,-.5*d);
	    for (x = 0; x < d; x++) {
	        for (z = 0; z < d; z++) {
	            block.getRelative(x, 0, z).setType(org.bukkit.Material.GLASS);
	        }
	    }
	    for (x = -1; x < d+1; x++) {
	        for (y = 1; y < 3; y++) {
	            block.getRelative(x, y, -1).setType(org.bukkit.Material.THIN_GLASS);
	            block.getRelative(x, y, d).setType(org.bukkit.Material.THIN_GLASS);
	        }
	    }
	    for (z = 0; z < d; z++) {
	        for (y = 1; y < 3; y++) {
	            block.getRelative(-1, y, z).setType(org.bukkit.Material.IRON_FENCE);
	            block.getRelative(d, y, z).setType(org.bukkit.Material.IRON_FENCE);
	        }
	    }
	}
    
    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
      }
    
}