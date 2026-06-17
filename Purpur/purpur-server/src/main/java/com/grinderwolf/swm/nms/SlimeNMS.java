package com.grinderwolf.swm.nms;

import com.grinderwolf.swm.api.world.SlimeWorld;
import org.bukkit.World;

import java.util.List;

/**
 * The main interface for NMS (Net Minecraft Server) operations.
 */
public interface SlimeNMS {

    /**
     * Gets the version number of the world format supported by this NMS implementation.
     *
     * @return the world version
     */
    int getWorldVersion();

    /**
     * Creates a NMS world from a SlimeWorld.
     *
     * @param slimeWorld the SlimeWorld to create from
     * @return the NMS world object (platform-specific)
     */
    World createWorld(SlimeWorld slimeWorld);

    /**
     * Migrates a world from one format to another.
     *
     * @param world the world to migrate
     * @param newVersion the new version to migrate to
     * @return the migrated world
     */
    default World migrateWorld(SlimeWorld world, int newVersion) {
        // Default implementation does nothing
        return null;
    }

    /**
     * Adds a world to the server's world list.
     *
     * @param nmsWorld the NMS world object
     */
    void addWorldToServerList(Object nmsWorld);

    /**
     * Gets the NMS world associated with a Bukkit world.
     *
     * @param bukkitWorld the Bukkit world
     * @return the NMS world object
     */
    Object getSlimeWorld(org.bukkit.World bukkitWorld);

    /**
     * Checks if a world version is supported by this NMS implementation.
     *
     * @param version the version to check
     * @return true if supported, false otherwise
     */
    boolean supportsGeneration(int version);

    /**
     * Checks if a Minecraft version is supported by this NMS implementation.
     *
     * @param minecraftVersion the Minecraft version (e.g., "1.21.7")
     * @return true if supported, false otherwise
     */
    boolean supports(String minecraftVersion);
}
