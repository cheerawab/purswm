package com.grinderwolf.swm.plugin.loaders;

import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.impl.CraftSlimeChunk;
import com.grinderwolf.swm.api.world.impl.CraftSlimeChunkSection;
import com.grinderwolf.swm.api.world.impl.CraftSlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.plugin.log.Logging;
import com.flowpowered.nbt.*;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Loader utilities for Slime World Manager
 * Handles deserialization of slime world data
 */
public class LoaderUtils {

    /**
     * Deserializes a slime world from byte array
     */
    public static SlimeWorld deserializeWorld(
            String worldName,
            byte[] data,
            SlimePropertyMap properties,
            boolean readOnly,
            SlimeLoader loader
    ) throws CorruptedWorldException {
        if (data == null || data.length < 13) {
            throw new CorruptedWorldException(worldName);
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream input = new DataInputStream(bais);

            // Check SLIM format header
            byte[] header = new byte[11]; // SRF header size
            input.readFully(header);

            // Read version
            byte version = input.readByte();
            if (version > 12) {
                throw new NewerFormatException(version);
            }

            // Read low/high chunk coordinates
            int minX = input.readShort() & 0xFFFF;
            int minZ = input.readShort() & 0xFFFF;
            int maxX = input.readShort() & 0xFFFF;
            int maxZ = input.readShort() & 0xFFFF;

            Map<Long, SlimeChunk> chunks = new HashMap<>();
            Map<Integer, byte[]> paletteCache = new HashMap<>();

            // Read chunks
            int compressedChunkLen = input.readInt();
            int chunkLen = input.readInt();
            byte[] compressedChunkData = readBytes(input, compressedChunkLen);

            // Decompress
            byte[] chunkData = decompress(compressedChunkData);

            // Read chunks
            ByteArrayInputStream chunkBais = new ByteArrayInputStream(chunkData);
            DataInputStream chunkIn = new DataInputStream(chunkBais);

            int width = maxX - minX + 1;
            int depth = maxZ - minZ + 1;

            for (int i = 0; i < width * depth; i++) {
                if (input.readBoolean()) { // Chunk exists bitmask
                    int chunkX = minX + (i % width);
                    int chunkZ = minZ + (i / depth);

                    try {
                        SlimeChunk chunk = deserializeChunk(
                                chunkIn, version,
                                chunkX, chunkZ, worldName, paletteCache
                        );
                        if (chunk != null) {
                            long chunkKey = makeChunkKey(chunkX, chunkZ);
                            chunks.put(chunkKey, chunk);
                        }
                    } catch (Exception e) {
                        Logging.warning("Failed to deserialize chunk at " + chunkX + ", " + chunkZ, e);
                    }
                } else {
                    // Skip chunk data
                }
            }

            // Read tile entities
            int tileEntitiesCompressedLen = input.readInt();
            int tileEntitiesLen = input.readInt();
            byte[] tileEntitiesCompressed = readBytes(input, tileEntitiesCompressedLen);

            // Read entities
            boolean hasEntities = input.readBoolean();
            int entitiesCompressedLen = 0;
            int entitiesLen = 0;
            byte[] entitiesCompressed = new byte[0];

            if (hasEntities) {
                entitiesCompressedLen = input.readInt();
                entitiesLen = input.readInt();
                entitiesCompressed = readBytes(input, entitiesCompressedLen);
            }

            // Read extra data
            int extraCompressedLen = input.readInt();
            int extraLen = input.readInt();
            byte[] extraCompressed = readBytes(input, extraCompressedLen);

            // Read world maps
            int mapsCompressedLen = input.readInt();
            int mapsLen = input.readInt();
            byte[] mapsCompressed = readBytes(input, mapsCompressedLen);

            input.close();

            // Create world
            return new CraftSlimeWorld(
                    loader,
                    worldName,
                    chunks,
                    new CompoundTag("", new CompoundMap()),
                    new ArrayList<>(),
                    version,
                    properties,
                    readOnly,
                    true
            );

        } catch (IOException e) {
            throw new CorruptedWorldException(worldName);
        }
    }

    /**
     * Deserializes a single chunk
     */
    private static SlimeChunk deserializeChunk(
            DataInputStream input,
            byte version,
            int chunkX,
            int chunkZ,
            String worldName,
            Map<Integer, byte[]> paletteCache
    ) throws IOException {
        SlimeChunkSection[] sections = new SlimeChunkSection[16];

        // Read section bitmask
        byte[] sectionBitmask = readBytes(input, 2);
        BitSet sectionBitmaskBits = BitSet.valueOf(sectionBitmask);

        for (int i = 0; i < 16; i++) {
            if (sectionBitmaskBits.get(i)) {
                boolean hasBlockLight = input.readBoolean();

                byte[] blockLight = null;
                if (hasBlockLight) {
                    blockLight = readBytes(input, 2048);
                }

                if (version >= 4) {
                    int paletteSize = input.readInt();
                    for (int p = 0; p < paletteSize; p++) {
                        int size = input.readInt();
                        input.skipBytes(size);
                    }

                    int blockStatesLength = input.readInt();
                    for (int s = 0; s < blockStatesLength / 8; s++) {
                        input.readLong();
                    }
                } else {
                    input.skipBytes(4096);
                    input.skipBytes(2048);
                }

                boolean hasSkyLight = input.readBoolean();
                if (hasSkyLight) {
                    input.skipBytes(2048);
                }

                sections[i] = new CraftSlimeChunkSection(blockLight, null, null, null, null);
            }
        }

        return new CraftSlimeChunk(worldName, chunkX, chunkZ, sections);
    }

    private static long makeChunkKey(int x, int z) {
        return (((long) z) << 32) | (x & 0xFFFFFFFFL);
    }

    private static byte[] readBytes(DataInputStream input, int length) throws IOException {
        if (length <= 0) {
            return new byte[0];
        }
        byte[] data = new byte[length];
        input.readFully(data);
        return data;
    }

    /**
     * Decompresses zstd data
     */
    public static byte[] decompress(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return new byte[0];
        }
        try {
            return com.github.luben.zstd.Zstd.decompress(compressed);
        } catch (Exception e) {
            Logging.warning("Failed to decompress data", e);
            throw e;
        }
    }
}
