package com.grinderwolf.swm.api.world.impl;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of SlimeChunk.
 * Contains all chunk data including sections, heightmaps, biomes, tile entities, and entities.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CraftSlimeChunk implements SlimeChunk {

    private final String worldName;
    private final int x;
    private final int z;
    private final SlimeChunkSection[] sections;
    private CompoundTag heightMaps;
    private int[] biomes;
    private final List<CompoundTag> tileEntities;
    private final List<CompoundTag> entities;

    /**
     * Constructs a CraftSlimeChunk with default values for height maps, biomes, and lists.
     *
     * @param worldName the world name
     * @param x chunk X coordinate
     * @param z chunk Z coordinate
     * @param sections the chunk sections
     */
    public CraftSlimeChunk(String worldName, int x, int z, SlimeChunkSection[] sections) {
        this.worldName = worldName;
        this.x = x;
        this.z = z;
        this.sections = sections;
        this.heightMaps = new CompoundTag("", new com.flowpowered.nbt.CompoundMap());
        this.biomes = new int[256]; // Default biome array size for 16x16 area
        this.tileEntities = new ArrayList<>();
        this.entities = new ArrayList<>();
    }

    /**
     * Sets the height maps for this chunk.
     *
     * @param heightMaps the height maps compound tag
     */
    public void setHeightMaps(CompoundTag heightMaps) {
        this.heightMaps = heightMaps;
    }

    /**
     * Sets the biomes for this chunk.
     *
     * @param biomes the biome array
     */
    public void setBiomes(int[] biomes) {
        this.biomes = biomes;
    }

    @Override
    public String getWorldName() {
        return worldName;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public SlimeChunkSection[] getSections() {
        return sections;
    }
}
