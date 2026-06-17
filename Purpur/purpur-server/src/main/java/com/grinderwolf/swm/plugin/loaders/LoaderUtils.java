package com.grinderwolf.swm.plugin.loaders;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.stream.ByteInputStream;
import com.flowpowered.nbt.stream.ByteOutputStream;
import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.SlimeException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.plugin.log.Logging;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader utilities for serializing/deserializing SlimeWorlds
 * and managing registered loaders.
 */
public class LoaderUtils {
    
    private static final Map<String, SlimeLoader> loaders = new ConcurrentHashMap<>();
    private static final List<SlimeWorld> slimeWorlds = new ArrayList<>();
    
    /**
     * Register a loader
     */
    public static void registerLoader(String name, SlimeLoader loader) {
        if (loaders.containsKey(name)) {
            throw new IllegalArgumentException("Loader already registered: " + name);
        }
        loaders.put(name, loader);
        Logging.info("Registered loader: " + name);
    }
    
    /**
     * Get a loader by name
     */
    public static SlimeLoader getLoader(String name) {
        return loaders.get(name);
    }
    
    /**
     * Get all registered loaders
     */
    public static Map<String, SlimeLoader> getLoaders() {
        return new HashMap<>(loaders);
    }
    
    /**
     * Serialize a SlimeWorld to bytes
     */
    public static byte[] serialize(SlimeWorld world) {
        try {
            ByteOutputStream out = new ByteOutputStream();
            com.grinderwolf.swm.api.utils.SlimeFormat.serialize(world, out);
            return out.getBytes();
        } catch (IOException e) {
            throw new SlimeException("Failed to serialize world: " + world.getName(), e);
        }
    }
    
    /**
     * Deserialize bytes to a SlimeWorld
     */
    public static SlimeWorld deserialize(
        SlimeLoader loader,
        String worldName,
        byte[] data,
        SlimePropertyMap properties,
        boolean readOnly
    ) throws CorruptedWorldException {
        try {
            ByteInputStream in = new ByteInputStream(data);
            return com.grinderwolf.swm.api.utils.SlimeFormat.deserialize(
                loader, worldName, properties, readOnly, in
            );
        } catch (IOException e) {
            throw new CorruptedWorldException(worldName);
        }
    }
    
    /**
     * Add a SlimeWorld to the known list
     */
    public static void addSlimeWorld(SlimeWorld world) {
        if (!slimeWorlds.contains(world)) {
            slimeWorlds.add(world);
        }
    }
    
    /**
     * Remove a SlimeWorld from the known list
     */
    public static void removeSlimeWorld(SlimeWorld world) {
        slimeWorlds.remove(world);
    }
    
    /**
     * Get all known SlimeWorlds
     */
    public static List<SlimeWorld> getSlimeWorlds() {
        return Collections.unmodifiableList(slimeWorlds);
    }
}
