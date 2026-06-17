package com.grinderwolf.swm.api.world.impl;

import com.grinderwolf.swm.api.world.SlimeChunkSection;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Implementation of SlimeChunkSection
 */
@EqualsAndHashCode
public class CraftSlimeChunkSection implements SlimeChunkSection {

    @Getter
    private final byte[] blockLight;
    @Getter
    private final byte[] skyLight;
    @Getter
    private final int[] biomes;
    @Getter
    private final byte[] blocks;
    @Getter
    private final byte[] data;

    public CraftSlimeChunkSection(byte[] blockLight, byte[] skyLight, 
                                  int[] biomes, byte[] blocks, byte[] data) {
        this.blockLight = blockLight;
        this.skyLight = skyLight;
        this.biomes = biomes;
        this.blocks = blocks;
        this.data = data;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CraftSlimeChunkSection)) return false;
        CraftSlimeChunkSection other = (CraftSlimeChunkSection) obj;
        return java.util.Arrays.equals(this.blockLight, other.blockLight) 
            && java.util.Arrays.equals(this.skyLight, other.skyLight);
    }

    @Override
    public int hashCode() {
        int result = blockLight != null ? java.util.Arrays.hashCode(blockLight) : 0;
        result = 31 * result + (skyLight != null ? java.util.Arrays.hashCode(skyLight) : 0);
        return result;
    }
}
