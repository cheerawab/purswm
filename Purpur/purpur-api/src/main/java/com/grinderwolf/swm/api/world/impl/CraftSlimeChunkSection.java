package com.grinderwolf.swm.api.world.impl;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of SlimeChunkSection.
 * Contains block data, light data, palette, and block states for a chunk section.
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class CraftSlimeChunkSection implements SlimeChunkSection {

    // Pre 1.13 block data
    private final byte[] blocks;
    private final NibbleArray data;

    // Post 1.13 block data
    private final ListTag<CompoundTag> palette;
    private final long[] blockStates;

    private final NibbleArray blockLight;
    private final NibbleArray skyLight;

    /**
     * Constructs a post-1.13 chunk section.
     *
     * @param palette the block palette
     * @param blockStates the block state array
     * @param blockLight the block light data, or null if not present
     * @param skyLight the sky light data, or null if not present
     */
    public CraftSlimeChunkSection(ListTag<CompoundTag> palette, long[] blockStates, 
                                   NibbleArray blockLight, NibbleArray skyLight) {
        this.blocks = null;
        this.data = null;
        this.palette = palette;
        this.blockStates = blockStates;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
    }

    /**
     * Constructs a pre-1.13 chunk section.
     *
     * @param blocks the block type array (4096 bytes)
     * @param data the block data nibble array (2048 bytes)
     * @param blockLight the block light data, or null
     * @param skyLight the sky light data, or null
     */
    public CraftSlimeChunkSection(byte[] blocks, NibbleArray data, 
                                   NibbleArray blockLight, NibbleArray skyLight) {
        this.blocks = blocks;
        this.data = data;
        this.palette = null;
        this.blockStates = null;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CraftSlimeChunkSection)) return false;
        CraftSlimeChunkSection other = (CraftSlimeChunkSection) obj;
        return java.util.Arrays.equals(this.blockLight != null ? this.blockLight.getBacking() : null,
                other.blockLight != null ? other.blockLight.getBacking() : null)
                && java.util.Arrays.equals(this.skyLight != null ? this.skyLight.getBacking() : null,
                other.skyLight != null ? other.skyLight.getBacking() : null);
    }

    @Override
    public int hashCode() {
        int result = blockLight != null ? java.util.Arrays.hashCode(blockLight.getBacking()) : 0;
        result = 31 * result + (skyLight != null ? java.util.Arrays.hashCode(skyLight.getBacking()) : 0);
        return result;
    }
}
