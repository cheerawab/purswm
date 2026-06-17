package com.grinderwolf.swm;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.plugin.loaders.file.FileLoader;
import com.grinderwolf.swm.nms.v1_21.impl.V1_21_R1SlimeNMS;

import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SlimeWorldManager - Main implementation of SWM for Purpur 1.21.7
 */
public class SlimeWorldManager implements SlimePlugin {
    
    private static SlimeWorldManager instance;
    
    private final Map<String, SlimeLoader> loaders = new ConcurrentHashMap<>();
    private final Map<String, SlimeWorld> loadedWorlds = new ConcurrentHashMap<>();
    private final Set<String> lockedWorlds = ConcurrentHashMap.newKeySet();
    private final Set<String> inUseWorlds = ConcurrentHashMap.newKeySet();
    
    private V1_21_R1SlimeNMS nms;
    
    public SlimeWorldManager() {
        instance = this;
    }
    
    /**
     * Initialize SWM - called from PurswmPlugin
     */
    public void init() {
        instance = this;
        
        // Setup NMS layer
        try {
            nms = V1_21_R1SlimeNMS.getInstance();
            System.out.println("[SWM] NMS v1_21 layer initialized");
        } catch (Exception e) {
            System.err.println("[SWM] Failed to initialize NMS layer");
            e.printStackTrace();
        }
        
        // Setup default file loader
        File worldsDir = new File("worlds");
        registerLoader("file", new FileLoader(worldsDir));
        
        System.out.println("[SWM] SWM initialized successfully");
    }
    
    /**
     * Get the instance
     */
    public static SlimeWorldManager getInstance() {
        return instance;
    }
    
    /**
     * Get NMS layer
     */
    public V1_21_R1SlimeNMS getNMS() {
        return nms;
    }
    
    /**
     * Register a custom loader
     */
    public void registerLoader(String name, SlimeLoader loader) {
        if (loaders.containsKey(name)) {
            throw new IllegalArgumentException("Loader '" + name + "' already exists");
        }
        loaders.put(name, loader);
        System.out.println("[SWM] Registered loader: " + name);
    }
    
    /**
     * Get loader by name
     */
    @Override
    public SlimeLoader getLoader(String dataSource) {
        SlimeLoader loader = loaders.get(dataSource);
        if (loader == null) {
            throw new IllegalArgumentException("Unknown loader: " + dataSource);
        }
        return loader;
    }
    
    /**
     * Get loader factory (simplified)
     */
    public LoaderFactory getLoaderFactory() {
        return new LoaderFactory();
    }
    
    /**
     * Load a world from a loader
     */
    @Override
    public SlimeWorld loadWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap) 
            throws UnknownWorldException, IOException, CorruptedWorldException, NewerFormatException, WorldInUseException {
        if (loader == null) {
            throw new IllegalArgumentException("Loader cannot be null");
        }
        if (worldName == null) {
            throw new IllegalArgumentException("World name cannot be null");
        }
        if (propertyMap == null) {
            throw new IllegalArgumentException("Properties cannot be null");
        }
        
        System.out.println("[SWM] Loading world: " + worldName);
        
        byte[] data = loader.loadWorld(worldName, readOnly);
        
        if (data == null || data.length == 0) {
            throw new UnknownWorldException(worldName);
        }
        
        SlimeWorld world = SlimeWorld.deserialize(data, propertyMap);
        loadedWorlds.put(worldName, world);
        
        return world;
    }
    
    /**
     * Create an empty world
     */
    @Override
    public SlimeWorld createEmptyWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap) 
            throws WorldAlreadyExistsException, IOException {
        if (loader == null) {
            throw new IllegalArgumentException("Loader cannot be null");
        }
        if (worldName == null) {
            throw new IllegalArgumentException("World name cannot be null");
        }
        
        if (loader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }
        
        System.out.println("[SWM] Creating empty world: " + worldName);
        
        SlimeWorld world = new SlimeWorld(worldName, propertyMap, new HashMap<>(), readOnly);
        
        if (!readOnly) {
            world.setLocked(true);
            byte[] data = world.serialize();
            loader.saveWorld(worldName, data, !readOnly);
        }
        
        loadedWorlds.put(worldName, world);
        
        return world;
    }
    
    /**
     * Generate a Minecraft world from SlimeWorld
     */
    @Override
    public void generateWorld(SlimeWorld slimeWorld) {
        if (slimeWorld == null) {
            throw new IllegalArgumentException("SlimeWorld cannot be null");
        }
        
        String worldName = slimeWorld.getName();
        
        if (Bukkit.getWorld(worldName) != null) {
            System.out.println("[SWM] World already loaded: " + worldName);
            return;
        }
        
        System.out.println("[SWM] Generating world: " + worldName);
        
        try {
            // Mark as in use
            inUseWorlds.add(worldName);
            
            // Get the location from properties
            Location spawnLocation = null;
            if (slimeWorld.getProperties().has(SlimeProperties.SPAWN_X) &&
                slimeWorld.getProperties().has(SlimeProperties.SPAWN_Y) &&
                slimeWorld.getProperties().has(SlimeProperties.SPAWN_Z)) {
                
                int spawnX = slimeWorld.getProperties().getInt(SlimeProperties.SPAWN_X);
                int spawnY = slimeWorld.getProperties().getInt(SlimeProperties.SPAWN_Y);
                int spawnZ = slimeWorld.getProperties().getInt(SlimeProperties.SPAWN_Z);
                
                org.bukkit.World.Environment env = org.bukkit.World.Environment.valueOf(
                    slimeWorld.getProperties().getString(SlimeProperties.ENVIRONMENT, "NORMAL"));
                
                spawnLocation = new Location(null, spawnX, spawnY, spawnZ, 0, 0);
                spawnLocation.setWorld(Bukkit.createWorld(new org.bukkit.WorldCreator(worldName, env)));
            }
            
            // Use NMS to create the world
            if (nms != null) {
                org.bukkit.World bukkitWorld = nms.createWorld(worldName, slimeWorld);
                
                if (bukkitWorld != null) {
                    System.out.println("[SWM] World generated successfully: " + worldName);
                } else {
                    System.err.println("[SWM] Failed to generate world: " + worldName);
                }
            } else {
                System.err.println("[SWM] NMS not initialized, cannot generate world");
            }
            
        } finally {
            inUseWorlds.remove(worldName);
        }
    }
    
    /**
     * Unlock all worlds
     */
    public void unlockWorlds() {
        for (String worldName : new ArrayList<>(loadedWorlds.keySet())) {
            try {
                SlimeWorld world = loadedWorlds.get(worldName);
                if (world != null && !world.isReadOnly()) {
                    lockedWorlds.remove(worldName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Shutdown SWM
     */
    public void close() {
        System.out.println("[SWM] Shutting down...");
        unlockWorlds();
        loadedWorlds.clear();
        instance = null;
    }
    
    /**
     * Loader factory for creating different types of loaders
     */
    public class LoaderFactory {
        
        /**
         * Create a FileSlimeLoader
         */
        public FileSlimeLoader createFileLoader(File directory) {
            return new FileSlimeLoader(directory);
        }
        
        /**
         * Create a FileSlimeLoader from Path
         */
        public FileSlimeLoader createFileLoader(java.nio.file.Path path) {
            return createFileLoader(path.toFile());
        }
    }
    
    /**
     * Fix import - we need to use the correct SlimeWorld constructor
     */
    private static class FactoryFix {
        public FileSlimeLoader create(FileSlimeLoader loader) {
            return loader;
        }
    }
    
    // Inner loader for FileSlimeLoader
    private static class FileSlimeLoader implements SlimeLoader {
        private final File directory;
        
        public FileSlimeLoader(File directory) {
            this.directory = directory;
        }
        
        @Override
        public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, IOException, WorldInUseException {
            File file = new File(directory, worldName + ".slime");
            if (!file.exists()) {
                throw new UnknownWorldException(worldName);
            }
            
            java.io.InputStream is = new java.io.FileInputStream(file);
            try {
                return is.readAllBytes();
            } finally {
                is.close();
            }
        }
        
        @Override
        public boolean worldExists(String worldName) {
            return new File(directory, worldName + ".slime").exists();
        }
        
        @Override
        public java.util.List<String> listWorlds() throws IOException {
            java.util.List<String> worlds = new java.util.ArrayList<>();
            File dir = directory;
            if (!dir.exists() || !dir.isDirectory()) {
                return worlds;
            }
            
            for (File f : dir.listFiles((d, name) -> name.endsWith(".slime"))) {
                worlds.add(f.getName().replace(".slime", ""));
            }
            return worlds;
        }
        
        @Override
        public void saveWorld(String worldName, byte[] data, boolean lock) throws IOException {
            File file = new File(directory, worldName + ".slime");
            file.createNewFile();
            java.io.OutputStream os = new java.io.FileOutputStream(file);
            try {
                os.write(data);
            } finally {
                os.close();
            }
        }
        
        @Override
        public void unlockWorld(String worldName) throws UnknownWorldException, IOException {
            // No-op for simple file loader
        }
        
        @Override
        public boolean isWorldLocked(String worldName) throws UnknownWorldException, IOException {
            return false;
        }
        
        @Override
        public void deleteWorld(String worldName) throws UnknownWorldException, IOException {
            File file = new File(directory, worldName + ".slime");
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
