package com.tynker.mc;

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
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.Integer;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;


public class GameManager extends JavaPlugin implements Listener{
   
    private int stageDimension = 30;
    private int gameTimer = 10;
    private int stageDistance = 50;
    private int playersPerLot = 1;
    private List<Player> waitingList;
    private List<List<Player>> lotList;
    private List<BossBar> promoList;
    private World world;
    private BossBar lobbyBar;
    private List<Player> currentPlayers;
    private int lotNumber;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        world = Bukkit.getWorlds().get(0);
        world.setSpawnFlags(false,false);
        waitingList = new ArrayList<Player>();
        lotList = new ArrayList<List<Player>>();
        lobbyBar = Bukkit.getServer().createBossBar("Waiting for other players.", org.bukkit.boss.BarColor.PURPLE, org.bukkit.boss.BarStyle.SEGMENTED_6);
        lobbyBar.setProgress(0.0);
        final org.bukkit.plugin.Plugin thisPlugin = this;
        selectNewCurrentPlayers();
        buildRoom(24,0,200,0);
        Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
            @Override
            public void run(){
                    if(currentPlayers.size() == playersPerLot){
                        launchGame(currentPlayers, lotNumber);
                        lobbyBar.removeAll();
                        selectNewCurrentPlayers();
                        synchronized(currentPlayers){
                            synchronized(waitingList){
                                for(int c = 0;c < Math.min(playersPerLot,waitingList.size());c++){
                                    currentPlayers.add(waitingList.remove(0));
                                }
                            }
                            lobbyBar.setProgress((double)currentPlayers.size()/playersPerLot);
                            for(Player player: currentPlayers){
                                lobbyBar.addPlayer(player);
                            }
                        }
                    }
                System.out.println("lot " + lotNumber + " list " +currentPlayers);
                Bukkit.getScheduler().runTaskLater(thisPlugin, this, (long)(1000 /50));
            }
        }, (long)(1000 / 50)); 
    }

    public void selectNewCurrentPlayers(){
        lotNumber = getFreeLot();
        currentPlayers = new ArrayList<Player>();
        synchronized(lotList){
            if(lotList.size() <= lotNumber){
                lotList.add(currentPlayers);
            }else{
                lotList.set(lotNumber,currentPlayers);
            }
        }
    }

    public void buildRoom(int d,int x,int y,int z){
        Block block = world.getBlockAt(x,y,z);
        world.getEntitiesByClass(org.bukkit.entity.EnderCrystal.class).forEach(entity->entity.remove());
        world.spawnEntity(block.getLocation().add(0,2,0),org.bukkit.entity.EntityType.ENDER_CRYSTAL);//.setInvulnerable(true); 
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
        for(Player player: players){
            player.setHealth(player.getMaxHealth());
            player.setCustomName(Integer.toString(lotNumber));
        }
        final Location origin = new Location(world, lotNumber*stageDistance , 100 , 0);
        final List<Player> thisPlayers = players;
        final int thisLotNumber = lotNumber; 
        buildStageAt(origin);
        final org.bukkit.plugin.Plugin thisPlugin = this;
        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            @Override
            public void run(){
                Boolean temp = true;
                for(Player player: thisPlayers){
                    player.teleport(origin.add(0,1,0));
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

    public void buildStageAt(Location origin){
        Block block = world.getBlockAt(origin.add(Math.round(-stageDimension/2),0,Math.round(-stageDimension/2)));
        for(int i = 0; i < stageDimension; i++){
            for(int j = 0; j < stageDimension;j++){
                block.getRelative(i,0,j).setType(org.bukkit.Material.SNOW_BLOCK);
            }
        }
    }

    public void endGame(int lotNumber){
        List<Player> players = lotList.get(lotNumber);
        synchronized(lotList){
            lotList.set(lotNumber, null);
            for(Player player: players){
                player.getInventory().clear();
                player.setCustomName(null);
                player.setHealth(player.getMaxHealth());
                player.teleport(world.getBlockAt(0,201,0).getLocation());
            }
        }
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
        player.setCustomName(null);
        player.getInventory().clear();
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.teleport(world.getBlockAt(0,201,0).getLocation());
    }

    @EventHandler
    public void PlayerDeath(PlayerDeathEvent event) {
        event.setKeepInventory(true);
        Player player = event.getEntity();
        player.setHealth(player.getMaxHealth());
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.teleport(player.getLocation().add(0,50,0));
        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            @Override
            public void run(){
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                String customName = player.getCustomName();
                if(customName == null){
                    player.teleport(world.getBlockAt(0,201,0).getLocation());
                }else{
                    player.teleport(world.getBlockAt(Integer.parseInt(customName)*50,101,0).getLocation());
                }
            }
        },(long)(5000 / 50));
    }

    //@EventHandler
    //public void PlayerRespawn(PlayerRespawnEvent event) {
    //    Player player = event.getPlayer();
    //    if(player.getCustomName() == null){
    //        event.setRespawnLocation(world.getBlockAt(0,126,0).getLocation());
    //    }else{
    //        event.setRespawnLocation(world.getBlockAt(Integer.parseInt(event.getPlayer().getCustomName())*50,101,0).getLocation());
    //    }
    //}

    @EventHandler
    public void PlayerQuit(PlayerQuitEvent event) {
	    Player player = event.getPlayer();
        String playerCN = player.getCustomName();
        if(playerCN == null){
            //try{
                waitingList.remove(player);
            //}
            //catch()
        }else{
            List<Player> players = lotList.get(Integer.parseInt(playerCN));
            synchronized(players){
                players.remove(player);    
            }
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
                synchronized(currentPlayers){
                    if(currentPlayers.size() < playersPerLot){
                        currentPlayers.add(player);
                        lobbyBar.addPlayer(player);
                        lobbyBar.setProgress(lobbyBar.getProgress()+(1.0/playersPerLot));    
                    }else{
                        synchronized(waitingList){
                            waitingList.add(player);
                        }
                    }        
                }
            }
        }    
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
