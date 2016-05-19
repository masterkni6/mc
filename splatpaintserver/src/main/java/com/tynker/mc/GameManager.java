package com.tynker.mc;

import org.bukkit.event.EventHandler;
import org.bukkit.World;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.Integer;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;


public class GameManager extends JavaPlugin implements Listener{
   
    private int playersPerLot = 1;
    private List<Player> playerList;
    private List<List<Player>> lotList;
    private World world;

    @Override
    public void onEnable() {
        world = Bukkit.getWorlds().get(0);
        playerList = new ArrayList<Player>();
        lotList = new ArrayList<List<Player>>();
        getServer().getPluginManager().registerEvents(this, this);
        buildRoom(24,0,125,0);
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

    public void launchGame(List<Player> players, int lotNumber){
        System.out.println(players + " are joining lot " + lotNumber);
        final Location origin = new Location(world, lotNumber*50 , 100 , 0);
        final List<Player> currentPlayers = players; 
        buildStageAt(origin);
        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            @Override
            public void run(){
                Boolean temp = true;
                for(Player player: currentPlayers){
                    player.teleport(origin.add(0,1,0));
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack((temp)?org.bukkit.Material.EGG:org.bukkit.Material.SNOW_BALL)); 
                    temp = !temp;
                }
            }
        },(long)(5000 / 50));
    }   

    public void buildStageAt(Location origin){
        Block block = world.getBlockAt(origin.add(-15,0,-15));
        for(int i = 0; i < 30; i++){
            for(int j = 0; j < 30;j++){
                block.getRelative(i,0,j).setType(org.bukkit.Material.SNOW_BLOCK);
            }
        }
    }

    public void endGame(int lotNumber){
        lotList.set(lotNumber, null);
    }

    public int getFreeLot(){
        int i;
        for(i = 0; i < lotList.size(); i++){
            if(lotList.get(i) == null){
                return i;
            } 
        }
        return i;
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
	    Player player = event.getPlayer();
        synchronized(this){
		    playerList.add(player);
        }   
        player.setCustomName(null);
        player.getInventory().clear();
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.teleport(world.getBlockAt(0,126,0).getLocation());
    }

    @EventHandler
    public void PlayerRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(world.getBlockAt(Integer.parseInt(event.getPlayer().getCustomName())*50,101,0).getLocation());
    }

    @EventHandler
    public void PlayerQuit(PlayerQuitEvent event) {
	    Player player = event.getPlayer();
        String playerCN = player.getCustomName();
        if(playerCN == null){
            playerList.remove(player);
        }
    }

    @EventHandler
    public void BlockBreak(BlockBreakEvent event) {
		    event.setCancelled(true);
    }

    @EventHandler
    public void PlayerEggThrow(PlayerEggThrowEvent event) {
		    event.setHatching(false);
    }

    @EventHandler
    public void PlayerDeath(PlayerDeathEvent event) {
		    event.setKeepInventory(true);
    }

    @EventHandler
    public void ProjectileHit(ProjectileHitEvent event){
        Projectile projectile = event.getEntity();
        Player player = (Player)projectile.getShooter();
        if(player.getCustomName() != null){
            if(projectile instanceof org.bukkit.entity.Snowball){
                paint(org.bukkit.DyeColor.BLUE, world.getBlockAt(projectile.getLocation()), 2);
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SNOW_BALL)); 
            }else if(projectile instanceof org.bukkit.entity.Egg){
                paint(org.bukkit.DyeColor.ORANGE, world.getBlockAt(projectile.getLocation()), 2);
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.EGG)); 
            }
        }
    }

    public void paint(org.bukkit.DyeColor color, Block block, int size){
        Block t_block;
        for (int x = (0 - size); x < size; x ++) {
            for (int y = (0 - size); y < size; y ++) {
                for (int z = (0 - size); z < size; z ++) {
                    t_block = block.getRelative(x,y,z);
                    if(!t_block.isEmpty() && !t_block.isLiquid()){
                        t_block.setType(org.bukkit.Material.WOOL);
                        org.bukkit.block.BlockState b_state = t_block.getState();
                        ((org.bukkit.material.Wool)b_state.getData()).setColor(color);
                        b_state.update();
                    }                
                }
            }        
        }
    }
}
