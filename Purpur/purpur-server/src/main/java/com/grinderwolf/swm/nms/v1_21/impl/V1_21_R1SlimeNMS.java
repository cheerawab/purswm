package com.grinderwolf.swm.nms.v1_21;

import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.SlimeNMS;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * V1_21 implementation of SlimeNMS for Paper 1.21+ (1.21.7)
 * Handles SlimeWorld to Minecraft ServerLevel conversion.
 */
public class V1_21_R1SlimeNMS implements SlimeNMS {

    @Override
    public int getWorldVersion() {
        return 12;
    }

    @Override
    public World createWorld(SlimeWorld slimeWorld) {
        try {
            // Add the world to the server's world list
            ServerLevel serverLevel = createServerLevel(slimeWorld);
            
            if (serverLevel == null) {
                return null;
            }

            // Get the Bukkit World from the ServerLevel
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null) {
                return null;
            }

            // Add to server worlds
            server.addLevel(serverLevel);
            
            // Convert to Bukkit world
            return new CraftWorld(server.getWorld());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create world: " + slimeWorld.getName(), e);
        }
    }

    private ServerLevel createServerLevel(SlimeWorld slimeWorld) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return null;
        }

        // Use reflection to find the ServerLevel constructor
        java.lang.reflect.Constructor<ServerLevel> constructor = 
            ServerLevel.class.getDeclaredConstructor(
                MinecraftServer.class,
                net.minecraft.resources.RegistryAccess.class,
                net.minecraft.world.level.storage.WorldData.class,
                net.minecraft.resources.ResourceKey.class,
                net.minecraft.world.level.chunk.status.ChunkStatus.class,
                java.util.concurrent.Executor.class,
                java.util.concurrent.Executor.class,
                net.minecraft.world.level.chunk.status.ChunkStatus.class,
                boolean.class,
                java.util.List.class,
                long.class,
                boolean.class,
                java.util.List.class
            );
        constructor.setAccessible(true);
        
        return constructor.newInstance(
            server,
            server.registryAccess(),
            null, // WorldData (will be populated from SlimeWorld)
            net.minecraft.core.registries.Registries.LEVEL_STEM,
            null, // ChunkStatus
            null, // ChunkHolderMapHolder (null to use default)
            java.util.concurrent.Executors.newFixedThreadPool(2), // Chunk task executor
            java.util.concurrent.Executors.newFixedThreadPool(2), // Main thread pool
            null, // ProgressListener (null to create default)
            false, // shouldLoadSpawn
            java.util.Collections.emptyList(), // BiomeContainers
            0L, // random seed
            true, // keepSpawnLoaded
            java.util.Collections.emptyList() // CustomDimensions
        );
    }

    @Override
    public boolean supportsGeneration(String version) {
        return version.startsWith("1.21");
    }

    @Override
    public Object createNMSWorld(SlimeWorld world) {
        // This is used for async world generation
        // Returns the NMS ServerLevel object
        return createWorld(world);
    }

    @Override
    public boolean isWorldLoaded(Object nmsWorld) {
        if (!(nmsWorld instanceof ServerLevel)) {
            return false;
        }
        
        ServerLevel serverLevel = (ServerLevel) nmsWorld;
        return MinecraftServer.getServer().worlds.contains(serverLevel);
    }

    @Override
    public String getWorldName(Object nmsWorld) {
        if (nmsWorld instanceof org.bukkit.World) {
            return ((org.bukkit.World) nmsWorld).getName();
        } else if (nmsWorld instanceof ServerLevel) {
            return ((ServerLevel) nmsWorld).getWorld().getWorld().getName();
        }
        return "unknown";
    }

    @Override
    public void addWorldToServerList(Object nmsWorld) {
        if (nmsWorld instanceof ServerLevel) {
            MinecraftServer.getServer().addLevel((ServerLevel) nmsWorld);
        } else {
            throw new IllegalArgumentException("nmsWorld must be a ServerLevel instance");
        }
    }

    @Override
    public org.bukkit.World getSlimeWorld(org.bukkit.World bukkitWorld) {
        // Implementation would go here
        return null;
    }
}
