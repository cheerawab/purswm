package com.grinderwolf.swm.nms.v1_21.impl;

import com.grinderwolf.swm.api.exceptions.SlimeException;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.loaders.file.FileLoader;

import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToJsonConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkBuildStatus;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * NMS implementation for Paper 1.21 (v1_21_R1)
 * Implements SlimeNMS for Paper 1.21.7+
 * 
 * Note: This implementation creates a simplified version that focuses on the core
 * functionality needed to load slime worlds and add them to the server.
 * More sophisticated chunk loading and world generation will be added later.
 */
public class V1_21_R1SlimeNMS implements com.grinderwolf.swm.nms.SlimeNMS {
    
    public static final int WORLD_VERSION = 12;
    
    private static final Logger LOG = Logger.getLogger("SWM/NMS/v1_21");
    
    static {
        try {
            LogManager.getLogManager().readStream(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("logging.properties"));
        } catch (IOException e) {
            // Ignore
        }
    }
    
    @Override
    public int getWorldVersion() {
        return WORLD_VERSION;
    }
    
    @Override
    public World createWorld(String worldName, SlimeWorld slimeWorld) {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null) {
                LOG.severe("MinecraftServer is null - cannot create world");
                return null;
            }
            
            Path worldPath = getWorldPath(server, worldName);
            LOG.info("Creating world at path: " + worldPath);
            
            // Get environment from properties or default to NORMAL
            String envStr = slimeWorld.getProperties().getStringProperty(SlimeProperties.ENVIRONMENT, "NORMAL");
            org.bukkit.World.Environment environment = org.bukkit.World.Environment.valueOf(envStr);
            
            // Create ServerLevel for this world
            ServerLevel serverLevel = createServerLevel(
                server,
                worldName,
                worldPath,
                environment,
                slimeWorld
            );
            
            if (serverLevel == null) {
                LOG.severe("Failed to create ServerLevel");
                return null;
            }
            
            // Register the world with Bukkit through the scheduler
            final ServerLevel finalServerLevel = serverLevel;
            try {
                org.bukkit.World bukkitWorld = com.google.common.util.concurrent.MoreExecutors
                    .directExecutor()
                    .map(Bukkit.getScheduler().runTask(
                        Bukkit.getPluginManager().getPlugin("Purswm"),
                        () -> {
                            CraftServer craftServer = (CraftServer) Bukkit.getServer();
                            return craftServer.addWorld(finalServerLevel, worldName);
                        }
                    ));
                
                if (bukkitWorld != null) {
                    LOG.info("Successfully created world: " + worldName);
                    return bukkitWorld;
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error adding world to server", e);
            }
            
            return null;
            
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error creating world: " + worldName, e);
            throw new SlimeException("Failed to create world: " + worldName, e);
        }
    }
    
    private ServerLevel createServerLevel(
        MinecraftServer server,
        String worldName,
        Path worldPath,
        org.bukkit.World.Environment environment,
        SlimeWorld slimeWorld
    ) {
        try {
            // Create world data
            net.minecraft.world.level.storage.PrimaryLevelData worldData = 
                createPrimaryLevelData(worldName, environment);
            
            // Get dimension key
            net.minecraft.resources.ResourceKey<Level> dimensionKey = getDimensionKey(environment);
            
            // Create chunk cache (simplified)
            ServerChunkCache chunkProvider = new ServerChunkCache(
                server,
                worldData,
                slimeWorld
            );
            
            // Get registry access
            RegistryAccess.Frozen registryAccess = server.registryAccess();
            
            // Create the ServerLevel
            ServerLevel serverLevel = new ServerLevel(
                server,
                worldData.getLevelStorageAccess().dataFixer,
                worldData.getLevelStorageAccess(),
                registryAccess,
                dimensionKey,
                chunkProvider,
                new ChunkProgressListenerFactory() {
                    @Override
                    public java.util.concurrent.CompletableFuture<ChunkProgressListener> create(
                        String s, ChunkStatus chunkStatus) {
                        return java.util.concurrent.CompletableFuture.completedFuture(
                            new ChunkProgressListener() {
                                @Override
                                public void finishedBuildingChunk(ChunkBuildStatus chunkBuildStatus) {}
                                
                                @Override
                                public void finishedLoadingChunk(int i, int i1) {}
                                
                                @Override
                                public void progressChunkSourceTickStart(long l, long l1) {}
                                
                                @Override
                                public void progressChunkSourceTickEnd() {}
                                
                                @Override
                                public void startedBuildingChunk(java.util.concurrent.CompletableFuture<?> future) {}
                                
                                @Override
                                public void setGenerationProgress(int i) {}
                            }
                        );
                    }
                },
                false,
                new net.minecraft.world.level.chunk.status.ChunkStatus[] {},
                0L,
                java.util.Collections.emptyList(),
                null
            );
            
            return serverLevel;
            
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error creating ServerLevel", e);
            throw e;
        }
    }
    
    private net.minecraft.world.level.storage.PrimaryLevelData createPrimaryLevelData(
        String worldName, 
        org.bukkit.World.Environment environment
    ) {
        // Create world settings
        net.minecraft.world.level.LevelSettings settings = new net.minecraft.world.level.LevelSettings(
            worldName,  // name
            net.minecraft.world.level.GameType.SURVIVAL.getProtocolId(), // gameMode
            false,      // customMapFeatures
            false,      // debug
            false,      // hardcore
            org.bukkitDifficultyToMinecraft(
                org.bukkit.Difficulty.NORMAL
            ),  // difficulty
            true,       // allowMonsters
            true,       // allowAnimals
            null        // oldLevelStorageLegacyLevel (deprecated)
        );
        
        // Create primary level data
        return new net.minecraft.world.level.storage.PrimaryLevelData(
            settings,
            net.minecraft.world.level.storage.PrimaryLevelData.DemoMode.OFF,
            true  // reprocessing
        );
    }
    
    private net.minecraft.world.Diff BDifficulty getBukkitDifficulty(org.bukkit.Difficulty bukkitDiff) {
        switch (bukkitDiff) {
            case PEACEFUL: return net.minecraft.world.Difficulty.PEACEFUL;
            case EASY:     return net.minecraft.world.Difficulty.EASY;
            case NORMAL:   return net.minecraft.world.Difficulty.NORMAL;
            case HARD:     return net.minecraft.world.Difficulty.HARD;
            default:       return net.minecraft.world.Difficulty.NORMAL;
        }
    }
    
    private net.minecraft.world.Difficulty BDifficulty getMinecraftDifficulty(
        org.bukkit.level.difficulty.BDifficulty fromDifficulty(org.bukkit.Difficulty diff) {
        switch (diff) {
            case PEACEFUL: return net.minecraft.world.Difficulty.PEACEFUL;
            case EASY:     return net.minecraft.world.Difficulty.EASY;
            case NORMAL:   return net.minecraft.world.Difficulty.NORMAL;
            case HARD:     return net.minecraft.world.Difficulty.HARD;
            default:       return net.minecraft.world.Difficulty.NORMAL;
        }
    }
    
    private net.minecraft.resources.ResourceKey<Level> getDimensionKey(
        org.bukkit.World.Environment environment
    ) {
        switch (environment) {
            case NETHER:   return Level.NETHER;
            case THE_END:  return Level.END;
            default:       return Level.OVERWORLD;
        }
    }
    
    private Path getWorldPath(MinecraftServer server, String worldName) {
        return server.getWorldPath(new File(worldName));
    }
    
    @Override
    public SlimeWorld getSlimeWorld(org.bukkit.World bukkitWorld) {
        if (bukkitWorld instanceof CraftWorld) {
            CraftWorld craftWorld = (CraftWorld) bukkitWorld;
            ServerLevel nmsWorld = craftWorld.getHandle();
            
            // Check if this is a known slime world
            for (com.grinderwolf.swm.api.world.SlimeWorld slimeWorld : 
                com.grinderwolf.swm.util.LoaderUtils.getSlimeWorlds()) {
                if (slimeWorld.getName().equals(bukkitWorld.getName())) {
                    return slimeWorld;
                }
            }
        }
        return null;
    }
    
    @Override
    public void setDefaultWorlds(
        com.grinderwolf.swm.api.world.SlimeWorld defaultWorld,
        com.grinderwolf.swm.api.world.SlimeWorld nether,
        com.grinderwolf.swm.api.world.SlimeWorld end
    ) {
        // Configure default worlds
        if (defaultWorld != null) {
            LOG.info("Default world set to: " + defaultWorld.getName());
        }
        if (nether != null) {
            LOG.info("Nether world set to: " + nether.getName());
        }
        if (end != null) {
            LOG.info("End world set to: " + end.getName());
        }
    }
    
    @Override
    public void addWorldToServerList(Object nmsWorldObject) {
        if (!(nmsWorldObject instanceof ServerLevel)) {
            throw new IllegalArgumentException("Must provide a ServerLevel");
        }
        
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            server.addLevel((ServerLevel) nmsWorldObject);
            LOG.info("Added world to server list: " + ((ServerLevel) nmsWorldObject).dimension().location().getPath());
        }
    }
    
    @Override
    public void generateWorld(com.grinderwolf.swm.api.world.SlimeWorld world) {
        createWorld(world.getName(), world);
    }
    
    /**
     * SlimeChunkProvider - Extends ServerChunkCache to support SlimeWorld loading
     * This is a placeholder that will be expanded later to actually load chunks from 
     * the SlimeWorld data structure.
     */
    private static class SlimeChunkProvider {
        private final MinecraftServer server;
        private final net.minecraft.world.level.storage.PrimaryLevelData worldData;
        private final SlimeWorld slimeWorld;
        
        public SlimeChunkProvider(
            MinecraftServer server,
            net.minecraft.world.level.storage.PrimaryLevelData worldData,
            SlimeWorld slimeWorld
        ) {
            this.server = server;
            this.worldData = worldData;
            this.slimeWorld = slimeWorld;
        }
        
        // Methods to load chunks from SlimeWorld...
        // This will load chunks in the format required by Minecraft
    }
}
