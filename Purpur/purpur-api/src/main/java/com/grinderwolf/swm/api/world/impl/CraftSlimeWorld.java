package com.grinderwolf.swm.api.world.impl;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.tag.ListTag;
import com.flowpowered.nbt.tag.StringTag;
import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.utils.SlimeFormat;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.IOException;
import java.util.*;

/**
 * Implementation of SlimeWorld interface
 */
@Getter
@EqualsAndHashCode
@ToString
public class CraftSlimeWorld implements SlimeWorld {

    private final SlimeLoader loader;
    private final String name;
    private Map<Long, SlimeChunk> chunks;
    private final CompoundTag extraData;
    private final List<CompoundTag> worldMaps;
    
    @Getter(lombok.AccessLevel.PUBLIC) private final byte version;
    private final SlimePropertyMap propertyMap;
    private final boolean readOnly;
    @Getter(lombok.AccessLevel.PUBLIC) private boolean locked;
    
    private volatile int saveVersion = 0;
    
    /**
     * Creates a new CraftSlimeWorld instance with the specified parameters.
     *
     * @param loader     The {@link SlimeLoader} used to load and store the world (can be null).
     * @param name       The name of the world.
     * @param chunks     The chunks belonging to the world contained in a {@link Map} of Long (Z and X coordinates combined) to {@link SlimeChunk}.
     * @param extraData  The world's extra data.
     * @param worldMaps  The world maps, represented by their nbt.
     * @param version    The version of the world.
     * @param propertyMap  The properties map of the world.
     * @param readOnly   Whether or not readOnly mode is enabled.
     * @param locked     Whether or not the world is locked.
     */
    public CraftSlimeWorld(
            SlimeLoader loader,
            String name,
            Map<Long, SlimeChunk> chunks,
            CompoundTag extraData,
            List<CompoundTag> worldMaps,
            byte version,
            SlimePropertyMap propertyMap,
            boolean readOnly,
            boolean locked
    ) {
        this.loader = loader;
        this.name = name;
        this.chunks = new HashMap<>(chunks);
        this.extraData = extraData.clone();
        this.worldMaps = new ArrayList<>(worldMaps);
        this.version = version;
        this.propertyMap = propertyMap;
        this.readOnly = readOnly;
        this.locked = locked;
    }

    public CraftSlimeWorld(
            String name,
            byte version,
            SlimePropertyMap propertyMap
    ) {
        this(null, name, new HashMap<>(), new CompoundTag("", new CompoundMap()), new ArrayList<>(), version, propertyMap, false, false);
    }

    public CraftSlimeWorld(
            String name,
            byte version,
            SlimePropertyMap propertyMap,
            List<SlimeChunk> chunks,
            boolean readOnly,
            boolean locked
    ) {
        this(null, name, new HashMap<>(), new CompoundTag("", new CompoundMap()), new ArrayList<>(), version, propertyMap, readOnly, locked);
        
        for (SlimeChunk chunk : chunks) {
            long key = (((long) chunk.getZ()) << 32) | (chunk.getX() & 0xFFFFFFFFL);
            chunks.put(key, chunk);
        }
    }

    /**
     * Serializes the slime world to bytes.
     * Uses SlimeFormat to serialize the world data.
     *
     * @return Serialized world data as byte array.
     * @throws RuntimeException if serialization fails.
     */
    public byte[] serialize() {
        return SlimeFormat.serializeWorld(this);
    }

    /**
     * Clones the world with a new name.
     * The clone will always be read-only.
     *
     * @param worldName the new name for the cloned world.
     * @return the cloned world
     * @throws IllegalArgumentException if worldName is null or same as this world
     */
    @Override
    public SlimeWorld clone(String worldName) {
        return clone(worldName, null, false);
    }

    /**
     * Clones the world with a new name and saves to the specified loader.
     *
     * @param worldName the new name for the cloned world.
     * @param loader the loader to save the clone to (can be null).
     * @return the cloned world
     * @throws IllegalArgumentException if worldName is null or same as this world
     * @throws WorldAlreadyExistsException if loader.worldExists(worldName) is true
     * @throws IOException if the clone cannot be saved.
     */
    @Override
    public SlimeWorld clone(String worldName, SlimeLoader loader) throws WorldAlreadyExistsException, IOException {
        return clone(worldName, loader, true);
    }

    /**
     * Clones the world with a new name and saves to the specified loader.
     *
     * @param worldName the new name for the cloned world.
     * @param loader the loader to save the clone to (can be null).
     * @param lock whether the clone should be locked.
     * @return the cloned world
     * @throws IllegalArgumentException if worldName is null or same as this world
     * @throws WorldAlreadyExistsException if loader.worldExists(worldName) is true and lock is true
     * @throws IOException if the clone cannot be saved.
     */
    @Override
    public SlimeWorld clone(String worldName, SlimeLoader loader, boolean lock) throws WorldAlreadyExistsException, IOException {
        if (name.equals(worldName)) {
            throw new IllegalArgumentException("The clone world cannot have the same name as the original world!");
        }
        if (worldName == null) {
            throw new IllegalArgumentException("The world name cannot be null!");
        }

        CraftSlimeWorld clone = new CraftSlimeWorld(
                loader == null ? this.loader : loader,
                worldName,
                chunks,
                extraData.clone(),
                new ArrayList<>(worldMaps),
                version,
                (SlimePropertyMap) propertyMap.clone(),
                true, // clones are always read-only
                lock
        );

        if (loader != null) {
            loader.saveWorld(worldName, clone.serialize(), lock);
        }

        return clone;
    }

    /**
     * Adds or updates a chunk in the world.
     *
     * @param chunk the chunk to add.
     */
    public void addChunk(SlimeChunk chunk) {
        long index = (((long) chunk.getZ()) << 32) | (chunk.getX() & 0xFFFFFFFFL);
        chunks.put(index, chunk);
    }

    /**
     * Removes a chunk from the world.
     *
     * @param x X coordinate.
     * @param z Z coordinate.
     * @return the removed chunk, or null if no chunk existed at that location.
     */
    public SlimeChunk removeChunk(int x, int z) {
        long index = (((long) z) << 32) | (x & 0xFFFFFFFFL);
        return chunks.remove(index);
    }

    /**
     * Gets a chunk by coordinates.
     *
     * @param x X coordinate.
     * @param z Z coordinate.
     * @return the chunk at those coordinates, or null if none exists.
     */
    @Override
    public SlimeChunk getChunk(int x, int z) {
        long index = (((long) z) << 32) | (x & 0xFFFFFFFFL);
        return chunks.get(index);
    }

    /**
     * Gets all chunks in the world.
     *
     * @return a collection of all chunks.
     */
    public Collection<SlimeChunk> getChunks() {
        return Collections.unmodifiableCollection(chunks.values());
    }

    /**
     * Gets whether the world has any chunks.
     *
     * @return true if the world has chunks, false otherwise.
     */
    public boolean hasChunks() {
        return !chunks.isEmpty();
    }

    /**
     * Gets the number of chunks in the world.
     *
     * @return the number of chunks.
     */
    public int getChunkCount() {
        return chunks.size();
    }
}
