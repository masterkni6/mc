package com.tynker.mc;

import java.util.Arrays;
import org.bukkit.event.EventHandler;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
//import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.WorldBorder;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;

public class GameManager extends JavaPlugin implements Listener{
   
    private int gameTimer = 10;
    private int playersPerLot = 6;
    private int minPlayersPerLot = 2;
    private int worldNum = 3;
    private int lobbyNum = 3; 
    private int borderRadius = 20;
    private Boolean[] worldList;
    private Boolean[] lobbyList;
    private BossBar[] gameBarList;
    private BossBar[] lobbyBarList;
    private BukkitTask[] countDownList;
    private WorldCreator[] worldCreators;
    private org.bukkit.plugin.Plugin thisPlugin = this;
    private File srcDir;
    private File[] worldFolders;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        File thisDir;

        //setting up lobby worlds
        lobbyList = new Boolean[lobbyNum];
        lobbyBarList = new BossBar[lobbyNum];
        Arrays.fill(lobbyList, false);
        Arrays.fill(lobbyBarList, Bukkit.getServer().createBossBar("", org.bukkit.boss.BarColor.PURPLE, org.bukkit.boss.BarStyle.SEGMENTED_6));
        countDownList = new BukkitTask[lobbyNum];
        //World world;
        try{
            srcDir = new File("lobby_t");
            for(int i = 0;i<lobbyNum;i++){
                thisDir = new File("lobby"+i);
                if(thisDir.exists() && thisDir.isDirectory())FileUtils.deleteDirectory(thisDir);
                FileUtils.copyDirectory(srcDir, thisDir);
                Bukkit.createWorld(new WorldCreator("lobby"+i)).setSpawnFlags(false,false);
                //lobbyBarList[i] = Bukkit.getServer().createBossBar("", org.bukkit.boss.BarColor.PURPLE, org.bukkit.boss.BarStyle.SEGMENTED_6);
            }
        } catch(IOException e){
        }

        //setting up game worlds
        worldList = new Boolean[worldNum];
        Arrays.fill(worldList, false);
        gameBarList = new BossBar[worldNum];
        Arrays.fill(gameBarList, Bukkit.getServer().createBossBar("", org.bukkit.boss.BarColor.YELLOW, org.bukkit.boss.BarStyle.SOLID));
        worldFolders = new File[worldNum];
        worldCreators = new WorldCreator[worldNum];
        //World tempWorld = Bukkit.createWorld(new WorldCreator("world_t"));
        //WorldBorder tempWorldBorder = tempWorld.getWorldBorder();
        //tempWorldBorder.setCenter(0,0);
        //tempWorldBorder.setSize(borderRadius*2);
        //Bukkit.unloadWorld(tempWorld, true);  
        try{
            srcDir = new File("world_t");
            for(int i = 0;i < worldNum;i++){
                thisDir = new File("world"+i);
                //worldFolders[i] = thisDir;
                if(thisDir.exists() && thisDir.isDirectory())FileUtils.deleteDirectory(thisDir);
                FileUtils.copyDirectory(srcDir, thisDir);
                worldCreators[i] = new WorldCreator("world"+i);
                Bukkit.createWorld(worldCreators[i]).setSpawnFlags(false,false);
                //gameBarList[i] = Bukkit.getServer().createBossBar("", org.bukkit.boss.BarColor.YELLOW, org.bukkit.boss.BarStyle.SOLID);
            }
        } catch(IOException e){
        }

    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    public void launchGame(int lobbyNumber, int worldNumber){
        final int thisWorldNumber = worldNumber;
        final World thisWorld = Bukkit.getWorld("world"+worldNumber);
        final List<Player> players = Bukkit.getWorld("lobby"+lobbyNumber).getPlayers();
        final BossBar thisBar = gameBarList[worldNumber];
        lobbyBarList[lobbyNumber].removeAll(); 
        
        //send players to game world
        players.forEach(player->{
            player.setCustomName("gaming");
            player.teleport(thisWorld.getHighestBlockAt(0,0).getLocation());
            thisBar.addPlayer(player); 
        });
     
        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            BukkitTask thisInterval = null;
            @Override
            public void run(){

                //schedule game to start after 2nd counter   
                thisInterval = Bukkit.getScheduler().runTaskTimer(thisPlugin, new Runnable(){
                    int counter = 5;
                    Boolean temp = true;
                    @Override
                    public void run(){
                        if(counter == 0){
                            thisInterval.cancel();
                            thisBar.setTitle("Start!");

                            //set up each player
                            players.forEach(player->{
                                player.getInventory().addItem(new org.bukkit.inventory.ItemStack((temp)?org.bukkit.Material.EGG:org.bukkit.Material.SNOW_BALL)); 
                                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD)); 
                                temp = !temp;
                            });     

                            //game ends on timer.
                            Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
                                @Override
                                public void run(){
                                    endGame(thisWorldNumber);
                                }
                            }, (long)(gameTimer*20));
                        } else {
                            thisBar.setTitle("Get ready! " + counter + ".");
                            counter--;
                        }
                    }
                },0,20);
            }
        },0);
    }

    public void endGame(int worldNumber){
        final int thisWorldNumber = worldNumber;
        final World thisWorld = Bukkit.getWorld("world"+worldNumber);
        final List<Player> players = thisWorld.getPlayers();        
        
        //stop game for all players and calsulate scores.
        players.forEach(player->player.setCustomName(null));
        calculateScore(worldNumber, 0, 10);

        //sets a delay for teleporting players back to hub world
        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            @Override
            public void run(){
                gameBarList[worldNumber].removeAll();

                players.forEach(player->{
                    player.getInventory().clear();
                    player.setHealth(player.getMaxHealth());
                    //player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                    player.teleport(Bukkit.getWorld("void").getSpawnLocation());
                    
                    //TO DO: TELEPORT BACK TO HUB WORLD!!!!

                });
                
                //sets a delay for unloading and reloading the game world after all players teleported out
                Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
                    @Override
                    public void run(){
                        Bukkit.unloadWorld(thisWorld, false);
                        File destDir = worldFolders[thisWorldNumber];
                        try{
                            FileUtils.deleteDirectory(destDir);
                            FileUtils.copyDirectory(srcDir, destDir);
                        } catch(IOException e){
                        }
                        Bukkit.createWorld(worldCreators[thisWorldNumber]).setSpawnFlags(false, false);
                        synchronized(worldList){
                            worldList[thisWorldNumber] = false;
                        }                   
                    }
                }, (long)(60));
            
            }
        }, (long)(200));
    }

    @EventHandler
    public void PlayerCommand(PlayerCommandPreprocessEvent event) {
        switch(event.getMessage()){
            //case "/setppl1":
            //    playersPerLot = 1;
            //    break;
            //case "/setppl2":
            //    playersPerLot = 2;
            //    break;
            default:
                break;
        }
    }

    //prevent players from moving out of world border even in spectator mode
    //@EventHandler
    //public void PlayerMove(PlayerMoveEvent event) {
    //    Location to = event.getTo();
    //    if("gaming".equals(event.getPlayer().getCustomName()) && (Math.abs(to.getX()) > borderRadius || Math.abs(to.getZ()) > borderRadius))event.setCancelled(true);
    //}

    public int getFreeSlot(Boolean[] ary){
        int i;

        for(i = 0; i < ary.length; i++){
            if(ary[i] == false){
                return i;
            } 
        }

        return -1;
    }

    
    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
	    Player player = event.getPlayer();
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.getInventory().clear();
        player.setHealth(player.getMaxHealth());
        player.setCustomName(null);
        int lobbyNumber, playerSize;
        //double ratio;
        //World targetWorld;
        //BossBar lobbyBar;
        synchronized(lobbyList){
            lobbyNumber = getFreeSlot(lobbyList);
            if(lobbyNumber < 0){
                player.kickPlayer("haha.");
            } else {
                World targetWorld = Bukkit.getWorld("lobby"+lobbyNumber);
                BossBar lobbyBar = lobbyBarList[lobbyNumber];
                
                //adding player to targeted game world
                lobbyBar.addPlayer(player);
                player.setLevel(lobbyNumber);
                playerSize = targetWorld.getPlayers().size()+1;
                player.teleport(targetWorld.getBlockAt(7,45,-10).getLocation());
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);

                //initialize lobby world if player is first to join 
                if(playerSize == 1){
                    countDownList[lobbyNumber] = Bukkit.getScheduler().runTaskTimer(this, new Runnable(){
                    int counter = 10;
                    int worldNumber, playerSize;
                        @Override
                        public void run(){
                            if(counter > 0){
                                playerSize = targetWorld.getPlayers().size();
                                lobbyBar.setProgress((double)playerSize/playersPerLot);
                                if(playerSize < playersPerLot){
                                    synchronized(lobbyList){
                                        lobbyList[lobbyNumber] = false;
                                    }
                                } else {
                                    synchronized(lobbyList){
                                        lobbyList[lobbyNumber] = true;
                                    }
                                } 
                                if(playerSize < minPlayersPerLot){

                                    //stop count down & reset counter if # of players drops below minPlayersPerLot
                                    lobbyBar.setTitle("Waiting for " + (minPlayersPerLot - playerSize) + " more player(s) to start the game");
                                    counter = 10;
                                } else {

                                    //start count down if # of players reach minPlayersPerLot
                                    lobbyBar.setTitle("Game will start in " + counter + " seconds.");
                                    counter--;
                                }
                            } else {
                                
                                //attempt to join free game world when counter reaches zero
                                synchronized(worldList){
                                    worldNumber = getFreeSlot(worldList);
                                    if(worldNumber >= 0){
                                        countDownList[lobbyNumber].cancel();
                                        worldList[worldNumber] = true;
                                        launchGame(lobbyNumber, worldNumber);
                                    }
                                }
                            }
                        }
                    }, 0, 20);
                }
            }
        }
    }

    //@EventHandler
    //public void FoodLevelChange(FoodLevelChangeEvent event) {
    //    event.setCancelled(true);
    //}

    @EventHandler
    public void PlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        player.setHealth(1.0);
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.teleport(player.getLocation().add(0,10,0));
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
    public void EntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity(); 
        if(entity.getCustomName() == null){
            if(event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID) entity.teleport(entity.getWorld().getBlockAt(7,45,-10).getLocation());
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
    
    public void calculateScore(int worldNumber, int low, int high){
        World world = Bukkit.getWorld("world"+worldNumber);
        Block block;
        int redScore = 0;
        int blueScore = 0;
        for(int x = -borderRadius;x < borderRadius;x++){
            for(int z = -borderRadius;z < borderRadius;z++){
                for(int y = low;y < high;y++){
                    block = world.getBlockAt(x,y,z);
                    if(block.getType() == org.bukkit.Material.WOOL){
                        org.bukkit.DyeColor color = ((org.bukkit.material.Wool)block.getState().getData()).getColor();
                        if(color == org.bukkit.DyeColor.BLUE){
                            blueScore++;
                        } else if(color == org.bukkit.DyeColor.ORANGE){
                            redScore++;
                        };
                    }
                }
            }
        }
        String endMessage = "Orange Team Score : " + redScore + ". Blue Team Score : " + blueScore + ". ";    
        if(redScore == blueScore){
            endMessage = endMessage + "Tied Game.";
        } else if(redScore > blueScore){
            endMessage = endMessage + "Orange Team Won.";
        } else {
            endMessage = endMessage + "Blue Team Won.";
        }
        gameBarList[worldNumber].setTitle(endMessage);    
    }
}
