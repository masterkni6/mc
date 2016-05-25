package com.tynker.mc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.block.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.*;
import org.bukkit.potion.*;
import org.bukkit.material.*;


public class GameManager extends JavaPlugin implements Listener{
  
    private int MAX_WORLDS_NUM = 3; 
    private int worldSize = 200;
    private int playersPerLot = 1;

    private String GAME_WORLD_PREFIX = "world";

    private Location startLotLoc;
    private Location startHubLoc;

    private WorldCreator[] worldCreators;
    private BossBar[] barList;

    private World lobbyWorld;

    private BossBar bb; 

    private List<Player> waitingList = Collections.synchronizedList(new ArrayList<Player>());
    private List<World> lotList = Collections.synchronizedList(new ArrayList<World>());

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        bb = getServer().createBossBar("Next to play:",BarColor.BLUE,BarStyle.SOLID, BarFlag.CREATE_FOG);
        bb.setProgress(0);
        bb.setVisible(true);
        startLotLoc = new Location(getServer().getWorlds().get(0),0,100,0);
        startHubLoc = new Location(getServer().getWorlds().get(0),0,200,0);
        lobbyWorld = getServer().getWorlds().get(0);   

        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            @Override
            public void run(){
                lobbyWorld.setSpawnFlags(false,false);
                List<Entity> entities = lobbyWorld.getEntities();
                for(Entity e : entities){
                    if(!(e instanceof Player)){
                        e.remove();
                    }
                }
                lobbyWorld.spawnEntity(startHubLoc.clone().add(10,2,10),EntityType.ENDER_CRYSTAL);
            }
        },1);

        buildWaitingRoom(0,200,0,20,10,20);

        worldCreators = new WorldCreator[MAX_WORLDS_NUM];
        barList = new BossBar[MAX_WORLDS_NUM];

        WorldBorder worldBorder;
        //World world;
        for(int i = 0;i < MAX_WORLDS_NUM;i++){
            worldCreators[i] = new WorldCreator(GAME_WORLD_PREFIX+i).type(org.bukkit.WorldType.FLAT).generateStructures(false).seed(0);//.generatorSettings("{\"seaLevel\":0}");
            worldBorder = Bukkit.createWorld(worldCreators[i]).getWorldBorder();
            worldBorder.setCenter(0,0);
            worldBorder.setSize(worldSize);
            Bukkit.unloadWorld(GAME_WORLD_PREFIX+i,true);
            Bukkit.createWorld(worldCreators[i]).setSpawnFlags(false, false);
            barList[i] = Bukkit.getServer().createBossBar("Get Ready!", org.bukkit.boss.BarColor.YELLOW, org.bukkit.boss.BarStyle.SOLID);
        }


        final JavaPlugin self = this; 
        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            @Override
            public void run(){

                lobbyWorld.setTime(1000);
                lobbyWorld.setStorm(false);
                lobbyWorld.setThundering(false);

	            synchronized(waitingList){
                    if(waitingList.size() >= playersPerLot){
                        int lotNumber = getFreeLot();

                        World targetWorld = Bukkit.getWorld(GAME_WORLD_PREFIX+lotNumber);
                        List<Player> currentPlayers = new ArrayList<Player>(waitingList.subList(0,playersPerLot));
	                    synchronized(lotList){
                            if(lotList.size() <= lotNumber){
                                lotList.add(targetWorld);
                                buildLot(lotNumber);
                            }else{
                                lotList.set(lotNumber,targetWorld);
                            }
                        }
                        targetWorld.setTime(1000);
                        BossBar thisBar = barList[lotNumber];

                        for(Player p : currentPlayers){
                            p.teleport(targetWorld.getSpawnLocation());
                            p.setCustomName(Integer.toString(lotNumber));
                            //thisBar.addPlayer(player); 
                        }
                        
                        launchGame(currentPlayers, lotNumber);
                        waitingList = new ArrayList<Player>(waitingList.subList(playersPerLot,waitingList.size()));
                        updateWaitingBar();
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
        bb.addPlayer(event.getPlayer());
        sendToWaitingRoom(event.getPlayer());
    }

    @EventHandler
    public void PlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if(p.getCustomName() == null){
            //removePlayer(p);
        }else{
            synchronized(lotList){
                //List<Player> players = lotList.get(Integer.parseInt(p.getCustomName()));
                //Iterator<Player> iter = players.iterator();
                //while (iter.hasNext()) {
                //    Player str = iter.next();
                //    if (str.getUniqueId() == p.getUniqueId()){
                //        iter.remove();
                //    }
                //}
                sendToWaitingRoom(p);
                List<Player> players = p.getWorld().getPlayers();
                if(players.size() == 1){
                    broadcast(players.get(0).getName() + " has won!");            
                    endGame(Integer.parseInt(p.getCustomName()));
                }else if (players.size() == 0){
                    broadcast("There was a draw at lot!");            
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
                    //List<Player> players = lotList.get(Integer.parseInt(p.getCustomName()));
                    //Iterator<Player> iter = players.iterator();
                    //while (iter.hasNext()) {
                    //    Player str = iter.next();
                    //    if (str.getUniqueId() == p.getUniqueId()){
                    //        iter.remove();
                    //    }
                    //}
                    List<Player> players = p.getWorld().getPlayers();
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
                        lobbyWorld.createExplosion(b.getLocation(),2.5f);
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
            }else if(message[1].equals("lot")){
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
                Block blockBeneath = lobbyWorld.getBlockAt(loc2.add(0,-1,0));
                if (blockBeneath.getType().equals(Material.WOOL)){
                    if(DyeColor.RED.equals(getColor(blockBeneath))){
                        lobbyWorld.createExplosion(loc2,2.5f);
                    }
                }
            }
        }
    }

    void launchGame(List<Player> players, int lotNumber){
        Random rand = new Random();
        for(Player player: players){
            player.setCustomName(Integer.toString(lotNumber));
        }
        for(Player p : players){
            p.getInventory().clear();
            p.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.DIAMOND_SPADE));
            p.setGameMode(GameMode.CREATIVE);
            p.teleport(new Location(lotList.get(lotNumber),rand.nextInt(25)+5,108,rand.nextInt(25)+5));
        }
    }   

    void endGame(int lotNumber){
        clearLot(lotNumber);
    }   

    void clearLot(int lotNumber){
        buildLot(lotNumber);
        lotList.set(lotNumber,null);
    }

    void buildLot(int lotNumber){
        buildBlock(lotList.get(lotNumber),0,100,0,60,1,60,Material.OBSIDIAN);
        buildWalls(lotList.get(lotNumber),0,101,0,60,1,60,Material.OBSIDIAN);
        buildBlock(lotList.get(lotNumber),1,101,1,58,1,58,Material.LAVA);

        buildBlock(lotList.get(lotNumber),0,105,0,60,1,60,Material.SNOW_BLOCK);
        //scatteredColoredBlocks(new Location(lobbyWorld,lotNumber*worldSize,105,0),60,30);
        buildWalls(lotList.get(lotNumber),0,106,0,60,2,60,Material.IRON_FENCE);
    }

    void buildWalls(World world,int posX,int posY,int posZ,int lX,int lY,int lZ, Material m){
        for(int x = posX; x <= posX + lX; x++){
            for(int y = posY; y < posY + lY; y++){
                world.getBlockAt(x,y,posZ).setType(m);    
                world.getBlockAt(x,y,posZ+lZ).setType(m);    
            }
        }
        for(int z = posZ; z <= posZ + lZ; z++){
            for(int y = posY; y < posY + lY; y++){
                world.getBlockAt(posX,y,z).setType(m);    
                world.getBlockAt(posX+lX,y,z).setType(m);    
            }
        }
    }

    void buildBlock(World world,int posX,int posY,int posZ,int lX,int lY,int lZ, Material m){
        for(int x = posX; x <= posX + lX; x++){
            for(int y = posY; y < posY + lY; y++){
                for(int z = posZ; z <= posZ + lZ; z++){
                    world.getBlockAt(x,y,z).setType(m);    
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
                lobbyWorld.getBlockAt(x,y,posZ).setType(org.bukkit.Material.GLASS);    
                lobbyWorld.getBlockAt(x,y,posZ+lZ).setType(org.bukkit.Material.GLASS);    
            }
        }
        for(int z = posZ; z < posZ + lZ; z++){
            for(int y = posY; y < posY + lY; y++){
                lobbyWorld.getBlockAt(posX,y,z).setType(org.bukkit.Material.GLASS);    
                lobbyWorld.getBlockAt(posX+lX,y,z).setType(org.bukkit.Material.GLASS);    
            }
        }
        for(int x = posX; x < posX + lX; x++){
            for(int z = posZ; z < posZ + lZ; z++){
                lobbyWorld.getBlockAt(x,posY,z).setType(org.bukkit.Material.GLASS);    
                lobbyWorld.getBlockAt(x,posY+lY,z).setType(org.bukkit.Material.GLASS);    
            }
        }
        lobbyWorld.setSpawnLocation(posX+lX/2,posY+lY/2,posZ+lZ/2);
    }

    void removePlayer(Player p){
        synchronized(waitingList){
            waitingList.remove(p);
            updateWaitingBar();
        }
    }

    void addPlayer(Player p){
        synchronized(waitingList){
            waitingList.add(p);
            updateWaitingBar();
        }
    }

    void sendToWaitingRoom(Player p){
		p.getInventory().clear();
        p.setFoodLevel(100);
		p.setGameMode(GameMode.SURVIVAL);
        p.setCustomName(null);
        p.teleport(startHubLoc.clone().add(5,1,5));
    }

    void addToWaitingList(Player p){
        synchronized(waitingList){
            if(waitingList.contains(p)){
                p.sendMessage(ChatColor.RED + "You have already been added. Please wait while we find you a game.");
            }else{
                addPlayer(p);
                p.sendMessage(ChatColor.GREEN + "You have been added to the waiting list.");
            }
        }
    }

    void updateWaitingBar(){
        bb.setProgress(waitingList.size()/(float)playersPerLot);
        String playerbar = "Next to play: ";
        List<Player> pView = waitingList.subList(0,Math.min(6,waitingList.size()));
        if(pView.size() > 0){
            playerbar += pView.get(0).getName();

            for(int i = 1; i < pView.size(); i++){
                playerbar += ", " + pView.get(i).getName();
            }
        }
        bb.setTitle(playerbar);
    }

    void broadcast(String message){
        for(Player p : Bukkit.getOnlinePlayers()){
            p.sendMessage(message);
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
            tBlock = lobbyWorld.getBlockAt(corner.clone().add(rand.nextInt(size-4)+2,0,rand.nextInt(size-4)+2));
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
