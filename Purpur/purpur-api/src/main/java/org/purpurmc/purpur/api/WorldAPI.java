package org.purpurmc.purpur.api;

import com.grinderwolf.swm.api.SlimeWorldManager;
import com.grinderwolf.swm.api.exceptions.SlimeException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.world.WorldType;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

/**
 * 世界管理 API
 */
public class WorldAPI {
    
    private final PurswmPlugin plugin;
    
    public WorldAPI(PurswmPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 加載 slime 世界
     * @param worldName 世界名稱
     * @param slimeFile slime 文件路徑
     * @return 是否成功
     */
    public boolean loadWorld(String worldName, Path slimeFile) {
        try {
            SlimeWorldManager swm = plugin.getSWM();
            if (swm == null) {
                plugin.getLogger().severe("SWM not initialized!");
                return false;
            }
            
            // 創建 FileSlimeLoader
            SlimeLoader loader = swm.getLoaderFactory().createFileLoader(slimeFile.getParent());
            
            // 加載 slime 世界
            SlimeWorld slimeWorld = loader.getWorld(worldName);
            if (slimeWorld == null) {
                plugin.getLogger().severe("Failed to load slime world: " + worldName);
                return false;
            }
            
            // 添加到伺服器
            World world = swm.loadWorld(worldName, slimeWorld, Bukkit.getWorldType.NORMAL, false);
            if (world != null) {
                plugin.getLogger().info("Successfully loaded world: " + worldName);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading world: " + worldName, e);
            return false;
        }
    }
    
    /**
     * 從遠端 URL 加載世界
     */
    public boolean loadWorldFromUrl(String worldName, String url) {
        try {
            SlimeWorldManager swm = plugin.getSWM();
            if (swm == null) {
                return false;
            }
            
            // URLLoader 實現
            // TODO: 需要實現 URiSlimeLoader
            
            plugin.getLogger().info("Loading world from URL: " + url);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading world from URL", e);
            return false;
        }
    }
    
    /**
     * 卸載世界
     */
    public boolean unloadWorld(String worldName) {
        try {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World not found: " + worldName);
                return false;
            }
            
            world.clearClientViews();
            Bukkit.getServer().unloadWorld(world, true);
            
            plugin.getLogger().info("Unloaded world: " + worldName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error unloading world: " + worldName, e);
            return false;
        }
    }
    
    /**
     * 複製世界模板
     * @param templateName 模板名稱
     * @param newName 新世界名稱
     * @return 是否成功
     */
    public boolean cloneFromTemplate(String templateName, String newName) {
        String templatePath = plugin.worldTemplates.get(templateName);
        if (templatePath == null) {
            plugin.getLogger().warning("Template not found: " + templateName);
            return false;
        }
        
        return loadWorld(newName, Path.of(templatePath));
    }
    
    /**
     * 獲取所有已加載的世界
     */
    public List<World> getLoadedWorlds() {
        return Bukkit.getWorlds();
    }
    
    /**
     * 檢查世界是否已加載
     */
    public boolean isWorldLoaded(String worldName) {
        return Bukkit.getWorld(worldName) != null;
    }
    
    /**
     * 獲取 SWM 實例
     */
    public SlimeWorldManager getSWM() {
        return plugin.getSWM();
    }
}
