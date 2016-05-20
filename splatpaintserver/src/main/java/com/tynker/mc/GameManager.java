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
   
    //private int stageDimension = 30;
    private int gameTimer = 10;
    //private int stageDistance = 50;
    private int playersPerLot = 1;
    private List<Player> waitingList;
    private Boolean[] worldList;
    private List<BossBar> promoList;
    //private World world;
    private BossBar lobbyBar;
    //private List<Player> currentPlayers;
    //private int lotNumber;
    //private World new_world;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        waitingList = new ArrayList<Player>();

        //setting up lobby wrold
        World world = Bukkit.getWorld("world");
        world.setSpawnFlags(false,false);
        world.setSpawnLocation(10,126,0);
        buildRoom(24, world.getBlockAt(0,125,0).getLocation());

        //setting up world list
        worldList = new Boolean[10];
        Arrays.fill(worldList, false);
        for(int i = 0;i < 10;i++){
            WorldCreator worldCreator = new WorldCreator("world"+i);//.environment(Environment.NORMAL).generateStructures(false).seed(0).generator(new WorldChunkGenerator());
            Bukkit.createWorld(worldCreator);
        } 

        //setting up lobby promo message
        lobbyBar = Bukkit.getServer().createBossBar("Waiting for other players.", org.bukkit.boss.BarColor.PURPLE, org.bukkit.boss.BarStyle.SEGMENTED_6);
        lobbyBar.setProgress(0.0);

        //setting up interval for launching game
        final org.bukkit.plugin.Plugin thisPlugin = this;
        Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
            @Override
            public void run(){
                if(waitingList.size() >= playersPerLot){
                    int lotNumber = getFreeLot();
                    if(lotNumber >= 0){
                        while(waitingList.size() >= playersPerLot && lotNumber >= 0){
                            synchronized(worldList){
                                worldList[lotNumber] = true;
                            }
                            World targetWorld = Bukkit.getWorld("world"+lotNumber);
                            synchronized(waitingList){
                                for(int c = 0;c < playersPerLot;c++){
                                    waitingList.remove(0).teleport(targetWorld.getBlockAt(0,201,0).getLocation());
                                }
                            }
                            launchGame(lotNumber);
                            lotNumber = getFreeLot();
                        }
                        lobbyBar.removeAll();
                        lobbyBar.setProgress((double)waitingList.size()/playersPerLot);
                        for(Player player: waitingList){
                            lobbyBar.addPlayer(player);
                        }
                    }
                }
            Bukkit.getScheduler().runTaskLater(thisPlugin, this, (long)(1000 /50));
            }
        }, (long)(1000 / 50)); 
    }

    //public void selectNewCurrentPlayers(){
    //    int i;
    //    for(i = 0; i < lotList.size(); i++){
    //            if(lotList.get(i) == null){
    //            lotNumber = i;
    //        } 
    //    }
    //    return i;
    //   lotNumber = getFreeLot();
    //    currentPlayers = new ArrayList<Player>();
    //    synchronized(lotList){
    //        if(lotList.size() <= lotNumber){
    //            lotList.add();
    //        }else{
    //            lotList.set(lotNumber,currentPlayers);
    //        }
    //    }
    //}

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
        final List<Player> thisPlayers = Bukkit.getWorld("world"+lotNumber).getPlayers();
        final int thisLotNumber = lotNumber;
        final org.bukkit.plugin.Plugin thisPlugin = this;
        Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
            @Override
            public void run(){
                Boolean temp = true;
                for(Player player: thisPlayers){
                    player.setHealth(player.getMaxHealth());
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack((temp)?org.bukkit.Material.EGG:org.bukkit.Material.SNOW_BALL)); 
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

    //public void buildStageAt(Location origin){
    //    Block block = world.getBlockAt(origin.add(Math.round(-stageDimension/2),0,Math.round(-stageDimension/2)));
    //    for(int i = 0; i < stageDimension; i++){
    //        for(int j = 0; j < stageDimension;j++){
    //            block.getRelative(i,0,j).setType(org.bukkit.Material.SNOW_BLOCK);
    //        }
    //    }
    //}
//       @EventHandler
//    public void PlayerCommand(PlayerCommandPreprocessEvent
// event) {
//        if(event.getMessage().equals("/buy")){
//        event.getPlayer().teleport(Bukkit.getWorld("world").getBlockAt(0,200,0).getLocation());
//        Bukkit.unloadWorld("new_world", false);
//        WorldCreator nw = new WorldCreator("new_world");//.environment(Environment.NORMAL).generateStructures(false).seed(0).generator(new WorldChunkGenerator());
//        Bukkit.createWorld(nw);
//        event.getPlayer().teleport(Bukkit.getWorld("new_world").getBlockAt(0,200,0).getLocation());
//    }
//}

    public void endGame(int lotNumber){
        String worldName = "world"+lotNumber;
        for(Player player: Bukkit.getWorld(worldName).getPlayers()){
            player.getInventory().clear();
            //player.setCustomName(null);
            player.setHealth(player.getMaxHealth());
            player.teleport(Bukkit.getWorld("world").getBlockAt(0,201,0).getLocation());
        }
        Bukkit.unloadWorld(worldName, false);
        WorldCreator wc = new WorldCreator(worldName);//.environment(Environment.NORMAL).generateStructures(false).seed(0).generator(new WorldChunkGenerator());
        Bukkit.createWorld(wc);
        synchronized(worldList){
            worldList[lotNumber] = false;
        }
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
    }
        //lotList.add(new ArrayList<Player>());
        return -1;
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
	    Player player = event.getPlayer();
        //player.setCustomName(null);
        player.getInventory().clear();
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        //player.teleport(Bukkit.getWorld("lobby").getBlockAt(0,201,0).getLocation());
    }

    //@EventHandler
    //public void PlayerDeath(PlayerDeathEvent event) {
    //    event.setKeepInventory(true);
    //    Player player = event.getEntity();
    //    player.setHealth(player.getMaxHealth());
    //    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
    //    player.teleport(player.getLocation().add(0,50,0));
    //    Bukkit.getScheduler().runTaskLater(this, new Runnable(){
    //        @Override
    //        public void run(){
    //            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
    //            String customName = player.getCustomName();
    //            if(customName == null){
    //                player.teleport(world.getBlockAt(0,201,0).getLocation());
    //            }else{
    //                player.teleport(world.getBlockAt(Integer.parseInt(customName)*50,101,0).getLocation());
    //            }
    //        }
    //    },(long)(5000 / 50));
    //}

    //@EventHandler
    //public void PlayerRespawn(PlayerRespawnEvent event) {
    //    Player player = event.getPlayer();
    //    if(player.getCustomName() == null){
    //        event.setRespawnLocation(world.getBlockAt(0,126,0).getLocation());
    //    }else{
    //        event.setRespawnLocation(world.getBlockAt(Integer.parseInt(event.getPlayer().getCustomName())*50,101,0).getLocation());
    //    }
    //}

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

    //@EventHandler
    //public void PlayerDeath(PlayerDeathEvent event) {
    //    event.setKeepInventory(true);
    //}

    //@EventHandler
    //public void PlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    //    if(event.getEntity() instanceof org.bukkit.entity.EnderCrystal){
	//	    Player player = (Player)event.getDamager();
    //        if(player.getCustomName() == null){
    //            synchronized(currentPlayers){
    //                if(currentPlayers.size() < playersPerLot){
    //                    currentPlayers.add(player);
    //                    lobbyBar.addPlayer(player);
    //                    lobbyBar.setProgress(lobbyBar.getProgress()+(1.0/playersPerLot));    
    //                }else{
    //                    synchronized(waitingList){
    //                        waitingList.add(player);
    //                    }
    //                }        
    //            }
    //        }
    //    }    
    //}

    @EventHandler
    public void EntityDamageEntity(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof org.bukkit.entity.EnderCrystal){
		    Player player = (Player)event.getDamager();
            if(player.getCustomName() == null){
                synchronized(waitingList){
                        waitingList.add(player);
                        lobbyBar.addPlayer(player);
                        lobbyBar.setProgress(lobbyBar.getProgress()+(1.0/playersPerLot));    
                    }        
                }
            event.setCancelled(true);
            }
    }

    @EventHandler
    public void ProjectileHit(ProjectileHitEvent event){
        Projectile projectile = event.getEntity();
        if(projectile instanceof org.bukkit.entity.Snowball){
            paint(org.bukkit.DyeColor.BLUE, projectile.getWorld().getBlockAt(projectile.getLocation()), 2);
            ((Player)projectile.getShooter()).getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SNOW_BALL)); 
        }else if(projectile instanceof org.bukkit.entity.Egg){
            paint(org.bukkit.DyeColor.ORANGE, projectile.getWorld().getBlockAt(projectile.getLocation()), 2);
            ((Player)projectile.getShooter()).getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.EGG)); 
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
