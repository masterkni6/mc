package com.tynker.mc;

import java.util.Arrays;
import org.bukkit.event.EventHandler;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.lang.Integer;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;


public class GameManager extends JavaPlugin implements Listener{
   
    private int gameTimer = 10;
    private int playersPerLot = 2;
    private List<Player> waitingList;
    private Boolean[] worldList;
    private List<BossBar> promoList;
    private BossBar lobbyBar;
    private WorldCreator[] worldCreators;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        waitingList = new ArrayList<Player>();

        //setting up lobby wrold
        World world = Bukkit.getWorld("world");
        world.setSpawnFlags(false,false);
        world.setPVP(false);
        buildRoom(24, world.getBlockAt(0,125,0).getLocation());

        //setting up world list
        worldList = new Boolean[3];
        Arrays.fill(worldList, false);
        worldCreators = new WorldCreator[worldList.length];
        for(int i = 0;i < worldList.length;i++){
            worldCreators[i] = new WorldCreator("world"+i);
            Bukkit.createWorld(worldCreators[i]);
        } 

        //setting up lobby promo message
        lobbyBar = Bukkit.getServer().createBossBar("Please wait for game to start.", org.bukkit.boss.BarColor.PURPLE, org.bukkit.boss.BarStyle.SEGMENTED_6);
        lobbyBar.setProgress(0.0);

        //setting up interval for launching game
        final org.bukkit.plugin.Plugin thisPlugin = this;
        Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
            @Override
            public void run(){
                if(waitingList.size() >= playersPerLot){
                    int lotNumber = getFreeLot();
                    World targetWorld;
                    if(lotNumber >= 0){
                        while(waitingList.size() >= playersPerLot && lotNumber >= 0){
                            synchronized(worldList){
                                worldList[lotNumber] = true;
                            }
                            targetWorld = Bukkit.getWorld("world"+lotNumber);
                            System.out.println("launching game in "+ lotNumber + " " + targetWorld);
                            synchronized(waitingList){
                                for(int c = 0;c < playersPerLot;c++){
                                    waitingList.remove(0).teleport(targetWorld.getHighestBlockAt(0,0).getLocation());
                                }
                            }
                            launchGame(lotNumber);
                            lotNumber = getFreeLot();
                        }
                        lobbyBar.removeAll();
                        synchronized(waitingList){
                            lobbyBar.setProgress((double)waitingList.size()/playersPerLot);
                            for(Player player: waitingList){
                                lobbyBar.addPlayer(player);
                            }
                        }
                    }
                }
            Bukkit.getScheduler().runTaskLater(thisPlugin, this, (long)(1000 /50));
            }
        }, (long)(1000 / 50)); 
    }

    public void buildRoom(int d, Location location){
        World world = location.getWorld();
        Block block = world.getBlockAt(location.clone().add(Math.round(-d/2),0,Math.round(-d/2)));
        world.getEntitiesByClass(org.bukkit.entity.EnderCrystal.class).forEach(entity->entity.remove());
        world.spawnEntity(location.add(0,2,0),org.bukkit.entity.EntityType.ENDER_CRYSTAL);
        int x,y,z;
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

    public void launchGame(int lotNumber){
        final int thisLotNumber = lotNumber;
        final org.bukkit.plugin.Plugin thisPlugin = this;
        Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
            @Override
            public void run(){
                Boolean temp = true;
                for(Player player: Bukkit.getWorld("world"+thisLotNumber).getPlayers()){
                    player.setHealth(player.getMaxHealth());
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack((temp)?org.bukkit.Material.EGG:org.bukkit.Material.SNOW_BALL)); 
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD)); 
                    temp = !temp;
                }
                Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
                    @Override
                    public void run(){
                        endGame(thisLotNumber);
                    }
                }, (long)(gameTimer*1000/50));
            }
        },(long)(5000 / 50));
    }   

    //       @EventHandler
//    public void PlayerCommand(PlayerCommandPreprocessEvent
// event) {
//        if(event.getMessage().equals("/buy")){
//        event.getPlayer().teleport(Bukkit.getWorld("world").getBlockAt(0,200,0).getLocation());
//        Bukkit.unloadWorld("new_world", false);
//        WorldCreator nw = new WorldCreator("new_world").environment(Environment.NORMAL).generateStructures(false).seed(0);//.generator(new WorldChunkGenerator());
//        Bukkit.createWorld(nw);
//        event.getPlayer().teleport(Bukkit.getWorld("new_world").getBlockAt(0,200,0).getLocation());
//    }
//}

    public void endGame(int lotNumber){
        final int thisLotNumber = lotNumber;
        final World thisWorld = Bukkit.getWorld("world"+lotNumber);
        final List<Player> players = thisWorld.getPlayers();        
        final org.bukkit.plugin.Plugin thisPlugin = this;
        players.forEach(player->player.setCustomName(null));
        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            @Override
            public void run(){
                players.forEach(player->{
                    player.getInventory().clear();
                    player.setHealth(player.getMaxHealth());
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    player.teleport(Bukkit.getWorld("world").getBlockAt(0,126,-5).getLocation());
                });
                //Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
                //    @Override
                //    public void run(){
                        Bukkit.unloadWorld(thisWorld, false);
                        Bukkit.createWorld(worldCreators[thisLotNumber]);
                        synchronized(worldList){
                            worldList[thisLotNumber] = false;
                        }                   
                //    }
                //}, (long)(1000/50));
            }
        }, (long)(5000/50));
    }

    public int getFreeLot(){
        int i;
        synchronized(worldList){
            for(i = 0; i < worldList.length; i++){
                if(worldList[i] == false){
                    return i;
                } 
            }
        }
        return -1;
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
	    Player player = event.getPlayer();
        player.setCustomName(null);
        player.getInventory().clear();
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.teleport(Bukkit.getWorld("world").getBlockAt(0,126,-5).getLocation());
    }

    @EventHandler
    public void PlayerDeath(PlayerDeathEvent event) {
        event.setKeepInventory(true);
        Player player = event.getEntity();
        player.setHealth(player.getMaxHealth());
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.teleport(player.getLocation().add(0,50,0));
    }

    //@EventHandler
    //public void PlayerQuit(PlayerQuitEvent event) {
	//    Player player = event.getPlayer();
    //    String playerCN = player.getCustomName();
    //    if(playerCN == null){
    //        //try{
    //            waitingList.remove(player);
    //        //}
    //        //catch()
    //    }else{
    //        List<Player> players = lotList.get(Integer.parseInt(playerCN));
    //        synchronized(players){
    //            players.remove(player);    
    //        }
    //    }
    //}

    @EventHandler
    public void BlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void PlayerEggThrow(PlayerEggThrowEvent event) {
        event.setHatching(false);
    }

    @EventHandler
    public void EntityDamageEntity(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof org.bukkit.entity.EnderCrystal){
		    Player player = (Player)event.getDamager();
            if(player.getCustomName() == null){
                player.setCustomName("waiting");
                lobbyBar.addPlayer(player);
                synchronized(waitingList){
                    waitingList.add(player);
                    if(waitingList.size() <= playersPerLot)lobbyBar.setProgress((double)waitingList.size()/playersPerLot);    
                }        
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void ProjectileHit(ProjectileHitEvent event){
        Projectile projectile = event.getEntity();
        Player player = (Player)projectile.getShooter();
        if(player.getCustomName() != null){ 
            if(projectile instanceof org.bukkit.entity.Snowball){
                paint(org.bukkit.DyeColor.BLUE, projectile.getWorld().getBlockAt(projectile.getLocation()), 2);
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SNOW_BALL)); 
            }else if(projectile instanceof org.bukkit.entity.Egg){
                paint(org.bukkit.DyeColor.ORANGE, projectile.getWorld().getBlockAt(projectile.getLocation()), 2);
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
