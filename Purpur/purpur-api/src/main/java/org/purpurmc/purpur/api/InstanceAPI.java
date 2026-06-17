package org.purpurmc.purpur.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 遊戲實例管理 API
 */
public class InstanceAPI {
    
    private final PurswmPlugin plugin;
    
    public InstanceAPI(PurswmPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 從模板創建遊戲實例
     * @param templateName 模板名稱
     * @param instanceName 實例名稱
     * @return 實例 ID 或 null
     */
    public String createInstance(String templateName, String instanceName) {
        return plugin.createInstance(templateName, instanceName);
    }
    
    /**
     * 創建帶自訂選項的實例
     */
    public String createInstance(String templateName, String instanceName, Map<String, Object> options) {
        // 擴展創建邏輯
        return plugin.createInstance(templateName, instanceName);
    }
    
    /**
     * 將玩家加入實例
     */
    public boolean joinPlayer(Player player, String instanceId) {
        var instanceInfo = plugin.getGameInstances().get(instanceId);
        if (instanceInfo == null) {
            player.sendMessage("§cInstance not found!");
            return false;
        }
        
        // TODO: 實現玩家傳送到實例世界
        // player.teleport(world.getSpawnLocation());
        
        player.sendMessage("§aJoined instance: " + instanceId);
        return true;
    }
    
    /**
     * 將玩家移出實例
     */
    public boolean ejectPlayer(Player player, String instanceId) {
        var instanceInfo = plugin.getGameInstances().get(instanceId);
        if (instanceInfo == null) {
            return false;
        }
        
        // TODO: 實現玩家返回匹配廳
        
        player.sendMessage("§eLeft instance!");
        return true;
    }
    
    /**
     * 销毁實例
     */
    public boolean destroyInstance(String instanceId) {
        return plugin.unloadGameInstance(instanceId);
    }
    
    /**
     * 獲取實例信息
     */
    public Optional<InstanceAPI.InstanceInfo> getInstance(String instanceId) {
        var instanceInfo = plugin.getGameInstances().get(instanceId);
        return Optional.ofNullable(instanceInfo);
    }
    
    /**
     * 獲取所有活躍實例
     */
    public List<InstanceAPI.InstanceInfo> getActiveInstances() {
        return new ArrayList<>(plugin.getGameInstances().values());
    }
    
    /**
     * 獲取實例中的玩家數量
     */
    public int getPlayerCount(String instanceId) {
        // TODO: 追蹤每個實例的玩家
        return 0;
    }
    
    /**
     * 實例信息類 (public for API users)
     */
    public static class InstanceInfo {
        private String name;
        private String templatePath;
        private String instanceId;
        private long createdAt;
        
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
}
