package com.tynker.mc;

import org.bukkit.event.block.BlockBreakEvent;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import org.bukkit.block.Block;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.material.Wool;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Random;
import java.util.Collections;



public class GameManager extends JavaPlugin implements Listener{
   
    private int gameDimension = 200;
    private int playersPerLot = 2;

    private Location startLotLoc;
    private Location startHubLoc;

    private World gameWorld;

    private List<Player> playerList = Collections.synchronizedList(new ArrayList<Player>());
    private List<List<Player>> lotList = Collections.synchronizedList(new ArrayList<List<Player>>());

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        
        startLotLoc = new Location(getServer().getWorlds().get(0),0,100,0);
        startHubLoc = new Location(getServer().getWorlds().get(0),0,200,0);
        gameWorld = getServer().getWorlds().get(0);   

        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            @Override
            public void run(){
                gameWorld.setSpawnFlags(false,false);
                List<Entity> entities = gameWorld.getEntities();
                for(Entity e : entities){
                    if(!(e instanceof Player)){
                        e.remove();
                    }
                }
                gameWorld.spawnEntity(startHubLoc.clone().add(10,2,10), EntityType.ENDER_CRYSTAL);
            }
        },1);

        buildWaitingRoom(0,200,0,20,10,20);

        final JavaPlugin self = this; 
        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            @Override
            public void run(){

                gameWorld.setTime(1000);

	            synchronized(playerList){
                    if(playerList.size() > playersPerLot-1){
                        int lotNum = getFreeLot();
                        List<Player> currentPlayers = new ArrayList<Player>(playerList.subList(0,playersPerLot));
	                    synchronized(lotList){
                            if(lotList.size() <= lotNum){
                                lotList.add(currentPlayers);
                            }
                            else{
                                lotList.set(lotNum,currentPlayers);
                            }
                        }
                        for(Player player: currentPlayers){
                            player.setCustomName(Integer.toString(lotNum));
                        }
                        launchGame(currentPlayers, lotNum);
                        playerList = new ArrayList<Player>(playerList.subList(playersPerLot,playerList.size()));
                    }
                }
                Bukkit.getScheduler().runTaskLater(self, this,(long)(1000/50));
            }
        }, (long)(5)*(1000 / 50)); 
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
        sendToWaitingRoom(event.getPlayer());
    }

    @EventHandler
    public void PlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if(p.getCustomName() == null){
            //removePlayer(p);
        }else{
            synchronized(lotList){
                List<Player> players = lotList.get(Integer.parseInt(p.getCustomName()));
                Iterator<Player> iter = players.iterator();
                while (iter.hasNext()) {
                    Player str = iter.next();
                    if (str.getUniqueId() == p.getUniqueId()){
                        iter.remove();
                    }
                }
                if(players.size() == 1){
                    broadcast(players.get(0).getName() + " has won at lot " + Integer.parseInt(p.getCustomName()) + "!");            
                    endGame(Integer.parseInt(p.getCustomName()));
                }else if (players.size() == 0){
                    broadcast("There was a draw at lot " + Integer.parseInt(p.getCustomName()) + "!");            
                    endGame(Integer.parseInt(p.getCustomName()));
                }
            }
        }   
    }

    @EventHandler
    public void PlayerRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(startHubLoc.clone().add(5,1,5));
        sendToWaitingRoom(event.getPlayer());
    }

    @EventHandler
    public void PlayerQuit(PlayerQuitEvent event) {
            Player p = event.getPlayer();
            if(event.getPlayer().getCustomName() == null){
                removePlayer(p);
            }else{
	            synchronized(lotList){
                    List<Player> players = lotList.get(Integer.parseInt(p.getCustomName()));
                    Iterator<Player> iter = players.iterator();
                    while (iter.hasNext()) {
                        Player str = iter.next();
                        if (str.getUniqueId() == p.getUniqueId()){
                            iter.remove();
                        }
                    }
                    if(players.size() == 0){
                        endGame(Integer.parseInt(p.getCustomName()));
                    }
                }
            }   
    }

    @EventHandler
    public void BlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        Block b = event.getBlock();
        if(!p.getGameMode().equals(org.bukkit.GameMode.CREATIVE)){
            if(p.getCustomName() == null){
                event.setCancelled(true);
            }else if(!b.getType().equals(Material.SNOW_BLOCK)){
                if(b.getType().equals(Material.WOOL)){
                    if(DyeColor.RED.equals(getColor(b))){
                        gameWorld.createExplosion(b.getLocation(),2.5f);
                    }else if(DyeColor.BLUE.equals(getColor(b))){
                        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,20*5,255));
                    }else{
                        event.setCancelled(true);
                    } 
                }else{
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void EntityDamageEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Entity damager;
        if(entity.getType().equals(EntityType.ENDER_CRYSTAL)){
            if((damager = event.getDamager()) instanceof Player){
                addToWaitingList((Player) damager);
            }
        }
        event.setCancelled(true);
    }
    
    @EventHandler
    public void FoodLevel(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void PlayerCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        String[] message = event.getMessage().split(" ");
        if(message[0].equals("/set")){
            if(message[1].equals("playersPerLot")){
                playersPerLot = Integer.parseInt(message[2]);
            }else if(message[1].equals("setLot")){
                List<Player> players = new ArrayList<Player>();
                players.add(p);
                launchGame(players, Integer.parseInt(message[2]));
            }
            
        }
    }

    @EventHandler
    public void PlayerMoveToAnotherLocation(PlayerMoveEvent event){
        Player player = event.getPlayer();
        Location loc = player.getLocation().clone();
        Location loc1 = event.getFrom().clone();
        Location loc2 = event.getTo().clone();
        if(!locEquals(loc1,loc2)){
            if (player.getCustomName() != null) {
                Block blockBeneath = gameWorld.getBlockAt(loc2.add(0,-1,0));
                if (blockBeneath.getType().equals(Material.WOOL)){
                    if(DyeColor.RED.equals(getColor(blockBeneath))){
                        gameWorld.createExplosion(loc2,2.5f);
                    }
                }
            }
        }
    }

    void launchGame(List<Player> players, int lotNumber){
        buildLot(lotNumber);
        Random rand = new Random();

        for(Player p : players){
            p.getInventory().clear();
            p.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.DIAMOND_SPADE));
            p.teleport(new Location(gameWorld,lotNumber*gameDimension+rand.nextInt(25)+2,108,rand.nextInt(25)+2));
        }
    }   

    void endGame(int lotNumber){
        clearLot(lotNumber);
    }   

    void clearLot(int lotNumber){
        lotList.set(lotNumber,null);
    }

    void buildLot(int lotNumber){
        buildBlock(lotNumber*gameDimension,100,0,60,1,60,Material.OBSIDIAN);
        buildWalls(lotNumber*gameDimension,101,0,60,1,60,Material.OBSIDIAN);
        buildBlock(lotNumber*gameDimension+1,101,1,58,1,58,Material.LAVA);

        buildBlock(lotNumber*gameDimension,105,0,60,1,60,Material.SNOW_BLOCK);
        scatteredColoredBlocks(new Location(gameWorld,lotNumber*gameDimension,105,0),60,30);
        buildWalls(lotNumber*gameDimension,106,0,60,2,60,Material.IRON_FENCE);
    }

    void buildWalls(int posX,int posY,int posZ,int lX,int lY,int lZ, Material m){
        for(int x = posX; x <= posX + lX; x++){
            for(int y = posY; y < posY + lY; y++){
                gameWorld.getBlockAt(x,y,posZ).setType(m);    
                gameWorld.getBlockAt(x,y,posZ+lZ).setType(m);    
            }
        }
        for(int z = posZ; z <= posZ + lZ; z++){
            for(int y = posY; y < posY + lY; y++){
                gameWorld.getBlockAt(posX,y,z).setType(m);    
                gameWorld.getBlockAt(posX+lX,y,z).setType(m);    
            }
        }
    }

    void buildBlock(int posX,int posY,int posZ,int lX,int lY,int lZ, Material m){
        for(int x = posX; x <= posX + lX; x++){
            for(int y = posY; y < posY + lY; y++){
                for(int z = posZ; z <= posZ + lZ; z++){
                    gameWorld.getBlockAt(x,y,z).setType(m);    
                }
            }
        }
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
        for(int x = posX; x < posX + lX; x++){
            for(int y = posY; y < posY + lY; y++){
                gameWorld.getBlockAt(x,y,posZ).setType(org.bukkit.Material.GLASS);    
                gameWorld.getBlockAt(x,y,posZ+lZ).setType(org.bukkit.Material.GLASS);    
            }
        }
        for(int z = posZ; z < posZ + lZ; z++){
            for(int y = posY; y < posY + lY; y++){
                gameWorld.getBlockAt(posX,y,z).setType(org.bukkit.Material.GLASS);    
                gameWorld.getBlockAt(posX+lX,y,z).setType(org.bukkit.Material.GLASS);    
            }
        }
        for(int x = posX; x < posX + lX; x++){
            for(int z = posZ; z < posZ + lZ; z++){
                gameWorld.getBlockAt(x,posY,z).setType(org.bukkit.Material.GLASS);    
                gameWorld.getBlockAt(x,posY+lY,z).setType(org.bukkit.Material.GLASS);    
            }
        }
        gameWorld.setSpawnLocation(posX+lX/2,posY+lY/2,posZ+lZ/2);
    }

    void removePlayer(Player p){
        synchronized(playerList){
            playerList.remove(p);
        }
    }

    void sendToWaitingRoom(Player p){
		p.getInventory().clear();
		p.setGameMode(GameMode.SURVIVAL);
        p.setCustomName(null);
        p.teleport(startHubLoc.clone().add(5,1,5));
    }

    void addToWaitingList(Player p){
        synchronized(playerList){
            if(playerList.contains(p)){
                p.sendMessage(ChatColor.RED + "You have already been added. Please wait we find you a game.");
            }else{
                playerList.add(p);
                p.sendMessage(ChatColor.GREEN + "You have been added to the waiting list.");
            }
        }
    }

    void broadcast(String message){
        for(Player p : Bukkit.getOnlinePlayers()){
            //if(p.hasPermission("permission.node")){
                p.sendMessage(message);
            //}
 
        }
    }

    boolean locEquals(Location loc1, Location loc2 ,boolean strict){
        if(strict){
            return loc1.equals(loc2);
        }else{
           return ((loc1.getBlockX() == loc2.getBlockX()) && (loc1.getBlockY() == loc2.getBlockY()) && (loc1.getBlockZ() == loc2.getBlockZ())); 
        }
    }

    boolean locEquals(Location loc1, Location loc2){
        return locEquals(loc1,loc2,false);
    }
    
    DyeColor getColor(Block b){
        org.bukkit.block.BlockState bState = b.getState();
        org.bukkit.material.Wool wool = (org.bukkit.material.Wool)bState.getData();
        return wool.getColor();
    }

    void setColor(Block b, DyeColor c){
        org.bukkit.block.BlockState bState = b.getState();
        org.bukkit.material.Wool wool = (org.bukkit.material.Wool)bState.getData();
        wool.setColor(c);
        bState.setData(wool);
        bState.update();
    }

    void scatteredColoredBlocks(Location corner, int size, int amount){
        Random rand = new Random();
        Block tBlock;
        for(int i = 0; i < amount; i++){
            tBlock = gameWorld.getBlockAt(corner.clone().add(rand.nextInt(size-4)+2,0,rand.nextInt(size-4)+2));
            if(rand.nextInt(2) == 0){
                tBlock.setType(Material.WOOL);
                setColor(tBlock,DyeColor.BLUE);
            }else{
                tBlock.setType(Material.WOOL);
                setColor(tBlock,DyeColor.RED);
            } 
        } 
    }

}
