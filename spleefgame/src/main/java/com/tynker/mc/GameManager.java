package com.tynker.mc;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockBreakEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;


public class GameManager extends JavaPlugin implements Listener{
   
    private int gameDimension = 60;
    private int playersPerLot = 1;

    private Location startLotLoc;
    private Location startHubLoc;

    private World gameWorld;

    private List<Player> playerList = new ArrayList<Player>();
    private List<List<Player>> lotList = new ArrayList<List<Player>>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        
        startLotLoc = new Location(getServer().getWorlds().get(0),0,100,0);
        startHubLoc = new Location(getServer().getWorlds().get(0),0,200,0);
        gameWorld = getServer().getWorlds().get(0);   

        buildWaitingRoom(0,200,0,20,10,20);

        final JavaPlugin self = this; 
        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            @Override
            public void run(){
	            synchronized(this){
                    if(playerList.size() > playersPerLot-1){
                        System.out.println(playerList);
                        int lotNum = getFreeLot();
                        System.out.println(lotNum);
                        List<Player> currentPlayers = playerList.subList(0,playersPerLot);
                        System.out.println(currentPlayers);

                        if(lotList.size() == 0){
                            lotList.add(currentPlayers);
                        }
                        else{
                            lotList.set(lotNum,currentPlayers);
                        }

                        for(Player player: currentPlayers){
                            player.setCustomName(Integer.toString(lotNum));
                        }
                        launchGame(currentPlayers, lotNum);
                        playerList = playerList.subList(playersPerLot,playerList.size());
                    }
                }
                Bukkit.getScheduler().runTaskLater(self, this,(long)(1000/50));
            }
        }, (long)(1000 / 50)); 
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
	    synchronized(this){
		    playerList.add(event.getPlayer());
            event.getPlayer().teleport(startHubLoc.clone().add(10,5,10));
        }   
    }

    @EventHandler
    public void PlayerQuit(PlayerQuitEvent event) {
	    synchronized(this){
            removePlayer(event.getPlayer());
            endGame(Integer.parseInt(event.getPlayer().getCustomName()));
        }   
    }

    @EventHandler
    public void BlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }




    //to be overwritten by real games
    void launchGame(List<Player> players, int lotNumber){
        System.out.println("Did not implement launchGame in your game!");
    }   

    //to be overwritten by real games
    void endGame(int lotNumber){
        clearLot(lotNumber);
        System.out.println("Did not implement endGame in your game!");
    }   

    void clearLot(int lotNumber){
        lotList.set(lotNumber,null);
    }

    int getFreeLot(){
        int i;
        for(i = 0; i < lotList.size(); i++){
            if(lotList.get(i) == null){
                return i;
            } 
        }
        return i;
    }

    void buildWaitingRoom(int posX,int posY,int posZ,int lX,int lY,int lZ){
        System.out.println("Building Waiting Room");
        for(int x = posX; x < posX + lX; x++){
            for(int y = posY; y < posY + lY; y++){
                gameWorld.getBlockAt(x,y,posZ).setType(org.bukkit.Material.GLASS);    
            }
        }
        for(int x = posX; x < posX + lX; x++){
            for(int y = posY; y < posY + lY; y++){
                gameWorld.getBlockAt(x,y,posZ+lZ).setType(org.bukkit.Material.GLASS);    
            }
        }
        for(int z = posZ; z < posZ + lZ; z++){
            for(int y = posY; y < posY + lY; y++){
                gameWorld.getBlockAt(posX,y,z).setType(org.bukkit.Material.GLASS);    
            }
        }
        for(int z = posZ; z < lZ; z++){
            for(int y = posY; y < posY + lY; y++){
                gameWorld.getBlockAt(posX+lX,y,z).setType(org.bukkit.Material.GLASS);    
            }
        }
        for(int x = posX; x < lX; x++){
            for(int z = posZ; z < posZ + lZ; z++){
                gameWorld.getBlockAt(x,posY,z).setType(org.bukkit.Material.GLASS);    
            }
        }
        for(int x = posX; x < lX; x++){
            for(int z = posZ; z < posZ + lZ; z++){
                gameWorld.getBlockAt(x,posY+lY,z).setType(org.bukkit.Material.GLASS);    
            }
        }
        gameWorld.setSpawnLocation(posX+lX/2,posY+lY/2,posZ+lZ/2);
        System.out.println("Finished Building Waiting Room");
    }

    void removePlayer(Player p){
        for(int i = 0 ; i < playerList.size(); i++){
            if(p.getUniqueId() == playerList.get(i).getUniqueId()){
                playerList.remove(i);
            }
        } 
    }

}
