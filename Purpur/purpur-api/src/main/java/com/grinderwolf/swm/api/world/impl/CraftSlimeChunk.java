package com.grinderwolf.swm.api.world.impl;

import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import lombok.*;

/**
 * Implementation of SlimeChunk.
 */
@EqualsAndHashCode
public class CraftSlimeChunk implements SlimeChunk {

    private final String worldName;
    private final int x;
    private final int z;
    private final SlimeChunkSection[] sections;

    public CraftSlimeChunk(String worldName, int x, int z, SlimeChunkSection[] sections) {
        this.worldName = worldName;
        this.x = x;
        this.z = z;
        this.sections = sections;
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
