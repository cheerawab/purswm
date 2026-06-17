package com.grinderwolf.swm.nms.v1_21;

import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.nms.SlimeNMS;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/**
 * V1_21 implementation of SlimeNMS for Paper 1.21+ (1.21.7)
 * Handles SlimeWorld to Minecraft ServerLevel conversion.
 */
public class V1_21_R1SlimeNMS implements SlimeNMS {

    private static final AtomicReference<Constructor<?>> LEVEL_CONSTRUCTOR = new AtomicReference<>();
    private SlimeWorld defaultOverworld;
    private SlimeWorld defaultNether;
    private SlimeWorld defaultEnd;

    public V1_21_R1SlimeNMS() {
        initializeConstructors();
    }

    private void initializeConstructors() {
        try {
            // In Paper 1.21, net.minecraft.world.level.Level (formerly ServerLevel) has multiple constructors.
            // We try to find the one used by Paper for async world generation.
            for (Constructor<?> constructor : net.minecraft.world.level.Level.class.getDeclaredConstructors()) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length >= 10 &&
                    net.minecraft.server.MinecraftServer.class.isAssignableFrom(paramTypes[0]) &&
                    net.minecraft.world.level.storage.WorldData.class.isAssignableFrom(paramTypes[2])) {
                    
                    if (LEVEL_CONSTRUCTOR.compareAndSet(null, constructor)) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize V1_21_R1SlimeNMS", e);
        }
    }

    @Override
    public void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) {
        this.defaultOverworld = normalWorld;
        this.defaultNether = netherWorld;
        this.defaultEnd = endWorld;
    }

    @Override
    public World createWorld(SlimeWorld slimeWorld) {
        if (LEVEL_CONSTRUCTOR.get() == null) {
            throw new IllegalStateException("ServerLevel constructor not yet resolved");
        }

        net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();

        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey = getDimensionKey(slimeWorld);

        // Paper 1.21: ServerChunkProvider constructor
        // ServerChunkProvider(MinecraftServer server, ServerLevelServer level, 
        //                   ServerChunkCacheSettings settings, 
        //                   ServerChunkTaskScheduler scheduler, 
        //                   Executor executor, 
        //                   int maxEntityRenderDistance, 
        //                   int simulationDistance, 
        //                   boolean forceTickEmptyChunks)
        
        // Create ServerChunkCache (Paper 1.21 uses ServerChunkCache, not ServerChunkProvider)
        
        // Create the level with Paper's virtual thread support
        try {
            net.minecraft.world.level.Level level = (net.minecraft.world.level.Level) LEVEL_CONSTRUCTOR.get().newInstance(
                server,
                server.registryAccess(),
                server.getWorldData(),
                dimKey,
                new net.minecraft.server.level.ChunkTaskScheduler(),
                2,  // fetchRadius
                false, // shouldLoadSpawn
                Collections.emptyList(),
                Collections.emptyList() // structureGenerators
            );

            if (level != null) {
                server.addLevel(level);
                return new CraftWorld(level.getWorld());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create world: " + slimeWorld.getName(), e);
        }
        return null;
    }

    private net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> getDimensionKey(SlimeWorld slimeWorld) {
        String env = slimeWorld.getPropertyMap().getStringProperty("environment", "NORMAL").toUpperCase();
        switch (env) {
            case "NETHER":
                return net.minecraft.world.level.Level.NETHER;
            case "END":
                return net.minecraft.world.level.Level.END;
            default:
                return net.minecraft.world.level.Level.OVERWORLD;
        }
    }

    @Override
    public void generateWorld(SlimeWorld world) {
        this.createWorld(world);
    }

    @Override
    public void addWorldToServerList(Object worldObject) {
        if (worldObject instanceof net.minecraft.world.level.Level) {
            net.minecraft.server.MinecraftServer.getServer().addLevel((net.minecraft.world.level.Level) worldObject);
        } else {
            throw new IllegalArgumentException("worldObject must be a Level instance");
        }
    }

    @Override
    public com.grinderwolf.swm.api.world.SlimeWorld getSlimeWorld(World world) {
        String worldName = world.getName();
        try {
            for (com.grinderwolf.swm.api.world.SlimeWorld sw : com.grinderwolf.swm.plugin.SWMPlugin.getInstance().getWorlds()) {
                if (sw.getName().equals(worldName)) {
                    return sw;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public byte getWorldVersion() {
        return 12;
    }

    @Override
    public Object createNMSWorld(SlimeWorld world) {
        return createWorld(world);
    }
}
