package com.tynker.mc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.Integer;
import org.bukkit.block.Block;
import java.util.ArrayList;
import java.util.List;


public class GameManager extends JavaPlugin implements Listener{
   
    private int gameDimension;
    private int playersNum;
    private List<Player> playerList = new ArrayList<Player>();
    private List<List<Player>> lotList = new ArrayList<List<Player>>();
 
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }
    
    public void launchGame(List<Player> players, int lotNumber){

    }   

    public int getFreeLot(){
	
	return 0;
    }

    Bukkit.getScheduler().runTaskTimer(this, new Runnable(){
        @Override
        public void run(){
            if(playerList.size() > playersNum-1){
			    int lotNum = getFreeLot();
			    lotList.add(lotNum, playerList);
			    launchGame(playerList.subList(0,playersNum-1), lotNum);
	            playerList = new Array;
	        }
        }
    }, 0l, (long)(1 * 1000 / 50)); 

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
	    synchronized(this){
		    playerList.add(event.getPlayer());
        }   
    }
}
