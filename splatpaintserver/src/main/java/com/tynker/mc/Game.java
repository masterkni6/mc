package com.tynker.mc;

import org.bukkit.boss.BossBar;
import org.bukkit.WorldCreator;

public class Game{
    private BossBar thisBossBar;
    private Boolean occupied;
    private WorldCreator thisWorldCreator;

    public Game(BossBar bb, WorldCreator wc){
        occupied = false;
        thisBossBar = bb;
        thisWorldCreator = wc;
    }

    public Boolean isOccupied(){
        return occupied;
    }

    public BossBar getBossBar(){
        return thisBossBar;
    }

    public WorldCreator getWorldCreator(){
        return thisWorldCreator;
    }
}
