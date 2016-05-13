	var tynker = require("tynker");
	var Utils = require("utils");
	var g_colors = tynker.g_colors;
	var g_fireworks = tynker.g_fireworks;
	var g_material = tynker.g_material;
	var g_entity = tynker.g_entity;
	var g_treeMap = tynker.g_treeMap;

	"use strict";

	Utils.world = function(p){
	    return Utils.player(p).getWorld();
	}
	var world = org.bukkit.Bukkit.getWorld("world");
	var listeners = [];
	var spawn_points = {};
	/*
	listeners.push(events.blockBreak(function(event) {
	    event.setCancelled(true);
	}));
	listeners.push(events.playerEggThrow(function(event) {
	    event.setHatching(false);
	}));
	listeners.push(events.playerRespawn(function(event) {
	    var cor = event.getPlayer().getCustomName().split("_");
	    event.setRespawnLocation(world.getBlockAt(cor[0],cor[1],cor[2]).getLocation());
	}));
	*/
	function buildRoom(d,x,y,z){
	    var block = world.getBlockAt(x,y,z);
	    block = block.getRelative(-.5*d,0,-.5*d);
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
	buildRoom(10,25,100,25);
	buildRoom(10,-25,100,-25);
	var bluecrystal = world.spawn(new org.bukkit.Location(world,25,102,25),org.bukkit.entity.EnderCrystal.class);
	var redcrystal = world.spawn(new org.bukkit.Location(world,-25,102,-25),org.bukkit.entity.EnderCrystal.class);
	bluecrystal.setInvulnerable(true);
	redcrystal.setInvulnerable(true);
	var gameList = [];
	function createGame(){
	    var gameinfo = gameList.pop();
	    if(!gameinfo){
	        gameinfo = {
	         //"center":
	         //"sp1":
	         //"sp2":   
	        };
	    };
	    return gameinfo;
	}


	command("start", function(params, player) {
	    var gameinfo = createGame();
	    tynker.getNearbyEntitiesByType("player", bluecrystal, 10, 10 , 10).forEach(function(entry){
	        entry.getInventory().addItem(new org.bukkit.item.ItemStack(org.bukkit.Material.SNOW_BALL));
	        entry.setCustomName(gameinfo.center);
	        entry.teleport(gameinfo.sp1);
	})
	    tynker.getNearbyEntitiesByType("player", redcrystal, 10, 10 , 10).forEach(function(entry){
	        entry.getInventory().addItem(new org.bukkit.item.ItemStack(org.bukkit.Material.EGG));
	        entry.setCustomName(gameinfo.center);
	        entry.teleport(gameinfo.sp2);
	})
	});