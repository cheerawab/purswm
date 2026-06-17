package org.purpurmc.purpur.api;

import com.grinderwolf.swm.api.SlimeWorldManager;
import com.grinderwolf.swm.api.exceptions.SlimeException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Purswm 主插件類
 * 提供世界管理和遊戲實例 API
 */
public class PurswmPlugin extends JavaPlugin {
    
    private SlimeWorldManager swm;
    private final Map<String, String> worldTemplates = new HashMap<>();
    private final Map<String, InstanceInfo> gameInstances = new HashMap<>();
    
    @Override
    public void onEnable() {
        getLogger().info("=========================================");
        getLogger().info("  Purswm Plugin Enabling");
        getLogger().info("  Version: " + getDescription().getVersion());
        getLogger().info("  Minecraft: 1.21.7");
        getLogger().info("=========================================");
        
        // 初始化 SWM
        try {
            swm = new SlimeWorldManager();
            swm.init();
            getLogger().info("SWM initialized successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize SWM", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // 加載配置
        saveDefaultConfig();
        
        // 註冊事件監聽器
        getServer().getPluginManager().registerEvents(new PurswmListener(this), this);
        
        // 註冊命令
        getCommand("purswm").setExecutor(new PurswmCommand(this));
        
        getLogger().info("Purswm Plugin enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Purswm Plugin disabling...");
        
        // 卸載所有遊戲實例
        for (String instanceId : gameInstances.keySet()) {
            unloadGameInstance(instanceId);
        }
        
        // 卸載所有世界
        unloadAllWorlds();
        
        if (swm != null) {
            try {
                swm.close();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error closing SWM", e);
            }
        }
        
        getLogger().info("Purswm Plugin disabled!");
    }
    
    /**
     * 加載 slime 世界
     */
    public boolean loadWorld(String worldName, Path slimeFile) {
        try {
            // 這裡需要實現具體邏輯
            getLogger().info("Loading world: " + worldName + " from " + slimeFile);
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load world: " + worldName, e);
            return false;
        }
    }
    
    /**
     * 卸載世界
     */
    public boolean unloadWorld(String worldName) {
        try {
            // 這裡需要實現具體邏輯
            if (Bukkit.getWorld(worldName) != null) {
                Bukkit.getWorld(worldName).clearClientViews();
                Bukkit.getServer().unloadWorld(Bukkit.getWorld(worldName), false);
                getLogger().info("Unloaded world: " + worldName);
                return true;
            }
            return false;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to unload world: " + worldName, e);
            return false;
        }
    }
    
    /**
     * 卸載所有世界
     */
    private void unloadAllWorlds() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            try {
                world.clearClientViews();
                Bukkit.getServer().unloadWorld(world, false);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error unloading world: " + world.getName(), e);
            }
        }
    }
    
    /**
     * 創建遊戲實例
     */
    public String createInstance(String templateName, String instanceName) {
        String instanceId = instanceName + "_" + System.currentTimeMillis();
        
        try {
            // 從模板克隆世界
            String templatePath = worldTemplates.get(templateName);
            if (templatePath == null) {
                getLogger().warning("Template not found: " + templateName);
                return null;
            }
            
            // 實例化邏輯
            gameInstances.put(instanceId, new InstanceInfo(
                instanceName,
                templatePath,
                instanceId,
                System.currentTimeMillis()
            ));
            
            getLogger().info("Created instance: " + instanceId + " from template: " + templateName);
            return instanceId;
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to create instance: " + instanceName, e);
            return null;
        }
    }
    
    /**
     * 卸載遊戲實例
     */
    public boolean unloadGameInstance(String instanceId) {
        InstanceInfo info = gameInstances.remove(instanceId);
        if (info != null) {
            getLogger().info("Unloaded instance: " + instanceId);
            return true;
        }
        return false;
    }
    
    /**
     * 加載模板
     */
    public void registerTemplate(String name, Path slimeFile) {
        worldTemplates.put(name, slimeFile.toString());
        getLogger().info("Registered template: " + name);
    }
    
    /**
     * 獲取 SWM 實例
     */
    public SlimeWorldManager getSWM() {
        return swm;
    }
    
    /**
     * 實例信息類
     */
    public static class InstanceInfo {
        private final String name;
        private final String templatePath;
        private final String instanceId;
        private final long createdAt;
        
        public InstanceInfo(String name, String templatePath, String instanceId, long createdAt) {
            this.name = name;
            this.templatePath = templatePath;
            this.instanceId = instanceId;
            this.createdAt = createdAt;
        }
        
        public String getName() { return name; }
        public String getTemplatePath() { return templatePath; }
        public String getInstanceId() { return instanceId; }
        public long getCreatedAt() { return createdAt; }
    }
    
    public Map<String, InstanceInfo> getGameInstances() {
        return gameInstances;
    }
}
