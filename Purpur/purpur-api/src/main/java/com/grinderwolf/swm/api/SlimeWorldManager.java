package com.grinderwolf.swm.api;

import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.plugin.SWMPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Main entry point for the SWM API. Provides methods to load, create, and manage SlimeWorld instances.
 */
public class SlimeWorldManager implements AutoCloseable {

    private final SlimePlugin slimePlugin;

    /**
     * Creates a new SlimeWorldManager with default configuration.
     */
    public SlimeWorldManager() {
        this(SWMPlugin.getInstance());
    }

    /**
     * Creates a new SlimeWorldManager with the given plugin instance.
     *
     * @param plugin the SWMPlugin instance
     */
    public SlimeWorldManager(SWMPlugin plugin) {
        this.slimePlugin = plugin;
    }

    /**
     * Initializes the world manager. Called automatically if using the default constructor.
     */
    public void init() {
        // SWM plugins are auto-loaded via plugin.yml
    }

    /**
     * Loads a world from the specified data source.
     *
     * @param loader the SlimeLoader to use
     * @param worldName the name of the world
     * @param readOnly whether the world should be loaded as read-only
     * @param propertyMap the property map for the world
     * @return the loaded SlimeWorld
     * @throws UnknownWorldException if the world doesn't exist
     * @throws IOException if loading fails
     * @throws CorruptedWorldException if the world data is corrupted
     * @throws NewerFormatException if the world uses an unsupported format version
     * @throws WorldInUseException if the world is locked by another server
     */
    public SlimeWorld loadWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap)
            throws UnknownWorldException, IOException, CorruptedWorldException, NewerFormatException, WorldInUseException {
        return slimePlugin.loadWorld(loader, worldName, readOnly, propertyMap);
    }

    /**
     * Creates an empty world and stores it.
     *
     * @param loader the SlimeLoader to use
     * @param worldName the name of the world
     * @param readOnly whether the world should be read-only
     * @param propertyMap the property map for the world
     * @return the created SlimeWorld
     * @throws WorldAlreadyExistsException if a world with that name already exists
     * @throws IOException if saving fails
     */
    public SlimeWorld createEmptyWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap)
            throws WorldAlreadyExistsException, IOException {
        return slimePlugin.createEmptyWorld(loader, worldName, readOnly, propertyMap);
    }

    /**
     * Generates a Minecraft world from a SlimeWorld.
     *
     * @param world the SlimeWorld to generate
     */
    public void generateWorld(SlimeWorld world) {
        slimePlugin.generateWorld(world);
    }

    /**
     * Gets a loader by data source name.
     *
     * @param dataSource the data source name (e.g., "file")
     * @return the SlimeLoader, or null if not found
     */
    public SlimeLoader getLoader(String dataSource) {
        return slimePlugin.getLoader(dataSource);
    }

    /**
     * Registers a custom loader.
     *
     * @param dataSource the data source name
     * @param loader the SlimeLoader to register
     */
    public void registerLoader(String dataSource, SlimeLoader loader) {
        slimePlugin.registerLoader(dataSource, loader);
    }

    /**
     * Imports a world from a directory into an SRF format.
     *
     * @param worldDir the world directory
     * @param worldName the name for the imported world
     * @param loader the SlimeLoader to use
     * @throws WorldAlreadyExistsException if the world already exists
     * @throws IOException if import fails
     */
    public void importWorld(File worldDir, String worldName, SlimeLoader loader) throws IOException {
        slimePlugin.importWorld(worldDir, worldName, loader);
    }

    /**
     * Migrates a world from one data source to another.
     *
     * @param worldName the world name
     * @param fromLoader the current loader
     * @param toLoader the target loader
     * @throws IOException if migration fails
     */
    public void migrateWorld(String worldName, SlimeLoader fromLoader, SlimeLoader toLoader) throws IOException {
        slimePlugin.migrateWorld(worldName, fromLoader, toLoader);
    }

    @Override
    public void close() {
        // Clean up resources
    }
}
