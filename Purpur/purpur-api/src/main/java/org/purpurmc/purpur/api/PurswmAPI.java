package org.purpurmc.purpur.api;

import org.purpurmc.purpur.api.WorldAPI;
import org.purpurmc.purpur.api.InstanceAPI;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Purswm 主 API 類
 * 提供所有外部插件需要調用的方法
 */
public class PurswmAPI {
    
    private final PurswmPlugin plugin;
    private final WorldAPI worldAPI;
    private final InstanceAPI instanceAPI;
    
    public PurswmAPI(PurswmPlugin plugin) {
        this.plugin = plugin;
        this.worldAPI = new WorldAPI(plugin);
        this.instanceAPI = new InstanceAPI(plugin);
    }
    
    /**
     * 獲取世界管理 API
     */
    public WorldAPI getWorldAPI() {
        return worldAPI;
    }
    
    /**
     * 獲取實例管理 API
     */
    public InstanceAPI getInstanceAPI() {
        return instanceAPI;
    }
    
    /**
     * 加載 slime 世界
     */
    public boolean loadWorld(String worldName, java.nio.file.Path slimeFile) {
        return worldAPI.loadWorld(worldName, slimeFile);
    }
    
    /**
     * 卸載世界
     */
    public boolean unloadWorld(String worldName) {
        return worldAPI.unloadWorld(worldName);
    }
    
    /**
     * 創建遊戲實例
     */
    public String createInstance(String templateName, String instanceName) {
        return instanceAPI.createInstance(templateName, instanceName);
    }
    
    /**
     * 销毁實例
     */
    public boolean destroyInstance(String instanceId) {
        return instanceAPI.destroyInstance(instanceId);
    }
    
    /**
     * 註冊世界模板
     */
    public void registerTemplate(String name, java.nio.file.Path slimeFile) {
        plugin.registerTemplate(name, slimeFile);
    }
    
    /**
     * 獲取插件實例
     */
    public PurswmPlugin getPlugin() {
        return plugin;
    }
}
