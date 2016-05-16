package com.tynker.mc;

import org.bukkit.event.EventHandler;
import org.bukkit.World;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.Integer;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;


public class GameManager extends JavaPlugin implements Listener{
   
    private int gameDimension = 50;
    private int playersPerLot = 2;
    private List<Player> playerList;
    private List<List<Player>> lotList;
    private World world;

    @Override
    public void onEnable() {
        world = Bukkit.getWorlds().get(0);
        //System.out.println(gameDimension);
        playerList = new ArrayList<Player>();
        lotList = new ArrayList<List<Player>>();
        //lotList.add(new List<PLayer>());
        getServer().getPluginManager().registerEvents(this, this);
        buildRoom(24,0,125,0);
        //world.setSpawnLocation(0,126,0);
        //System.out.println(world.getSpawnLocation());
        final org.bukkit.plugin.Plugin thisPlugin = this;
        Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
            @Override
            public void run(){
                    System.out.println(playerList);
                    if(playerList.size() > playersPerLot-1){
			        int lotNum = getFreeLot();
                    System.out.println("lot "+lotNum);
                    List<Player> currentPlayers = playerList.subList(0,playersPerLot);
                    System.out.println("cp " + currentPlayers);
                    if(lotList.size()-1 < lotNum){
			            lotList.add(currentPlayers);
                    }else{
			            lotList.set(lotNum, currentPlayers);
                    }
                    for(Player player: currentPlayers){
                        player.setCustomName(Integer.toString(lotNum));
                    System.out.println(player+" cn "+player.getCustomName());
                    }
			        launchGame(currentPlayers, lotNum);
	                playerList = playerList.subList(playersPerLot, playerList.size());
	            }
                Bukkit.getScheduler().runTaskLater(thisPlugin, this, (long)(1000 /50));
            }
        }, (long)(1000 / 50)); 
    }

    public void buildRoom(int d,int x,int y,int z){
        Block block = world.getBlockAt(x,y,z);
        block = block.getRelative(Math.round(-d/2),0,Math.round(-d/2));
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


    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    //to be overwritten by real games
    public void launchGame(List<Player> players, int lotNumber){
        System.out.println(players + " are joining lot " + lotNumber);
    }   

    //to be overwritten by real games
    public void endGame(List<Player> players, int lotNumber){
        System.out.println("Did not implement endGame in your game!");
    }   

    public void endGame(int lotNumber){
        lotList.set(lotNumber, null);
    }

    public int getFreeLot(){
        //List<Player> playerList;
        int i;
        for(i = 0; i < lotList.size(); i++){
            //playerList = lotList.get(i);
            if(lotList.get(i) == null){
                return i;
            } 
        }
        //lotList.add(new ArrayList<Player>());
        return i;//lotList.size()-1;
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
	    Player player = event.getPlayer();
        synchronized(this){
		    playerList.add(player);
        }   
        player.setCustomName(null);
        player.teleport(world.getBlockAt(0,126,0).getLocation());
    }

    @EventHandler
    public void PlayerQuit(PlayerQuitEvent event) {
	    Player player = event.getPlayer();
        String playerCN = player.getCustomName();
        if(playerCN == null){
            playerList.remove(player);
        }
        //lotList.set(Integer.parseInt(player.getCustomName()), null);
        //lotList.add(0,new ArrayList<PLayer>());
    }

    @EventHandler
    public void BlockBreak(BlockBreakEvent event) {
		    event.setCancelled(true);
    }
}
