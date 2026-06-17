package org.purpurmc.purpur.api;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.logging.Level;

/**
 * Purswm 事件監聽器
 */
public class PurswmListener implements Listener {
    
    private final PurswmPlugin plugin;
    
    public PurswmListener(PurswmPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getLogger().info("Player joined: " + event.getPlayer().getName());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getLogger().info("Player quit: " + event.getPlayer().getName());
    }
    
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.getLogger().log(Level.INFO, "World loaded via Purswm: " + event.getWorld().getName());
    }
    
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        plugin.getLogger().log(Level.INFO, "World unloaded via Purswm: " + event.getWorld().getName());
    }
}
