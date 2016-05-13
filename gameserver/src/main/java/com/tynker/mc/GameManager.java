package com.tynker.mc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Block;
import java.util.ArrayList;
import java.util.List;


public class GameManager extends JavaPlugin implements Listener{
   
    private int gameDimension;
    private int playersNum;
    private List<Player> playerlist = new ArrayList<Player>();

 
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }
    
    public void launchGame(List<Player> players, int logNumber){

    }   

   public int getFreeLot(){

	return 0;
    }
 
    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
	playerlist.add(event.getPlayer());
	if(playerlist.size() == playersNum){
		launchGame(playerlist, getFreeLot());
	}


    }
    
}
