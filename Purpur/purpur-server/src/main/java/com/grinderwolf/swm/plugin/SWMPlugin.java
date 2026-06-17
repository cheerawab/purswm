package com.grinderwolf.swm.plugin;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.api.world.impl.CraftSlimeWorld;
import com.grinderwolf.swm.plugin.loaders.file.FileSlimeLoader;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.nms.SlimeNMS;
import com.grinderwolf.swm.nms.v1_21.V1_21_R1SlimeNMS;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main implementation of the SlimePlugin interface.
 * Manages all worlds and loaders for the SWM system.
 */
public class SWMPlugin implements SlimePlugin {

    private static SWMPlugin instance;

    private final Map<String, SlimeLoader> loaders = new ConcurrentHashMap<>();
    private final Map<String, CraftSlimeWorld> loadedWorlds = new ConcurrentHashMap<>();
    private final Map<String, World> bukkitWorlds = new ConcurrentHashMap<>();
    private final Set<String> inUseLocks = ConcurrentHashMap.newKeySet();

    private SlimeNMS nms;
    private File worldsDirectory;

    public SWMPlugin() {
        instance = this;
    }

    /**
     * Gets the instance of SWMPlugin.
     *
     * @return the instance
     */
    public static SWMPlugin getInstance() {
        return instance;
    }

    /**
     * Initializes the plugin.
     */
    public void onEnable() {
        // Initialize NMS layer
        nms = new V1_21_R1SlimeNMS();
        
        // Create default worlds directory
        worldsDirectory = new File("worlds");
        if (!worldsDirectory.exists()) {
            worldsDirectory.mkdirs();
        }

        // Register default file loader
        registerLoader("file", new FileSlimeLoader(worldsDirectory));

        System.out.println("[SWM] SWMPlugin enabled!");
    }

    /**
     * Gets the NMS bridge.
     *
     * @return the NMS bridge
     */
    public SlimeNMS getNMS() {
        return nms;
    }

    /**
     * Gets the worlds directory.
     *
     * @return the worlds directory
     */
    public File getWorldsDirectory() {
        return worldsDirectory;
    }

    // ==================== SlimePlugin Implementation ====================

    @Override
    public SlimeWorld loadWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap)
            throws UnknownWorldException, IOException, CorruptedWorldException, NewerFormatException, WorldInUseException {
        Objects.requireNonNull(loader, "Loader cannot be null");
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(propertyMap, "PropertyMap cannot be null");

        if (inUseLocks.contains(worldName) && !readOnly) {
            throw new WorldInUseException(worldName);
        }

        byte[] data = loader.loadWorld(worldName, readOnly);
        return LoaderUtils.deserializeWorld(worldName, data, propertyMap, readOnly, loader);
    }

    @Override
    public SlimeWorld createEmptyWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap)
            throws WorldAlreadyExistsException, IOException {
        Objects.requireNonNull(loader, "Loader cannot be null");
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(propertyMap, "PropertyMap cannot be null");

        if (loader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        CraftSlimeWorld world = new CraftSlimeWorld(worldName, (byte) 12, propertyMap, true, readOnly);
        loader.saveWorld(worldName, world.serialize(), false);
        return world;
    }

    @Override
    public void generateWorld(SlimeWorld slimeWorld) {
        Objects.requireNonNull(slimeWorld, "SlimeWorld cannot be null");
        if (!(slimeWorld instanceof CraftSlimeWorld)) {
            throw new IllegalArgumentException("World must be a CraftSlimeWorld instance");
        }

        CraftSlimeWorld world = (CraftSlimeWorld) slimeWorld;

        if (bukkitWorlds.containsKey(world.getName())) {
            // World already exists, don't create again
            return;
        }

        // Generate the world using NMS
        World bukkitWorld = nms.createWorld(world);
        
        if (bukkitWorld != null) {
            bukkitWorlds.put(world.getName(), bukkitWorld);
            loadedWorlds.put(world.getName(), world);
        }
    }

    @Override
    public void migrateWorld(String worldName, SlimeLoader currentLoader, SlimeLoader newLoader)
            throws IOException, WorldInUseException, WorldAlreadyExistsException, UnknownWorldException {
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(currentLoader, "CurrentLoader cannot be null");
        Objects.requireNonNull(newLoader, "NewLoader cannot be null");

        if (newLoader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        boolean inUse = inUseLocks.contains(worldName);
        if (inUse) {
            throw new WorldInUseException(worldName);
        }

        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld != null) {
            // Unload the world from server first
            bukkitWorld.getChunkLoadQueue(); // Force chunk unload
            bukkitWorld.clearClientViews();
            Bukkit.getScheduler().stop(); // Stop scheduler to allow unload
            Bukkit.getServer().unloadWorld(bukkitWorld, true);
        }

        byte[] data = currentLoader.loadWorld(worldName, false);
        loader.unlockWorld(worldName);
        newLoader.saveWorld(worldName, data, false);
        currentLoader.deleteWorld(worldName);
    }

    @Override
    public SlimeLoader getLoader(String dataSource) {
        Objects.requireNonNull(dataSource, "Data source name cannot be null");
        SlimeLoader loader = loaders.get(dataSource);
        if (loader == null) {
            throw new IllegalArgumentException("Unknown data source: " + dataSource);
        }
        return loader;
    }

    @Override
    public void registerLoader(String dataSource, SlimeLoader loader) {
        Objects.requireNonNull(dataSource, "Data source name cannot be null");
        Objects.requireNonNull(loader, "Loader cannot be null");

        if (loaders.containsKey(dataSource)) {
            throw new IllegalArgumentException("Loader already registered: " + dataSource);
        }
        loaders.put(dataSource, loader);
    }

    @Override
    public void importWorld(File worldDir, String worldName, SlimeLoader loader)
            throws WorldAlreadyExistsException, InvalidWorldException, WorldLoadedException, WorldTooBigException, IOException {
        Objects.requireNonNull(worldDir, "World directory cannot be null");
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(loader, "Loader cannot be null");

        if (loader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        World bukkitWorld = Bukkit.getWorld(worldDir.getName());
        if (bukkitWorld != null && !nms.getSlimeWorld(bukkitWorld).isEmpty()) {
            throw new WorldLoadedException(worldDir.getName());
        }

        CraftSlimeWorld world = LoaderUtils.convertToSlimeWorld(worldDir);
        loader.saveWorld(worldName, world.serialize(), false);
    }

    /**
     * Gets the list of loaded world names.
     *
     * @return list of world names
     */
    public List<String> getLoadedWorldNames() {
        return new ArrayList<>(loadedWorlds.keySet());
    }

    /**
     * Gets the list of in-use locks.
     *
     * @return list of in-use world names
     */
    public Set<String> getInUseLocks() {
        return Collections.unmodifiableSet(inUseLocks);
    }
}
