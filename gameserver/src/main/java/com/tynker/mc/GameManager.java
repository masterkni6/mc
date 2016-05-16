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
    private int playersPerLot;
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

    //to be inherited by real games
    public void launchGame(List<Player> players, int lotNumber){
    }   

    public int getFreeLot(){
        List<Player> playerList;
        for(int i = 0; i < lotList.size(); i++){
            playerList = lotList[i];
            if(!playerList.size()){
                return i;
            } 
        }
        lotList.add(new ArrayList<Player>);
        return lotList.size()-1;
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
        playerList.add(event.getPlayer());
    }
    
}
