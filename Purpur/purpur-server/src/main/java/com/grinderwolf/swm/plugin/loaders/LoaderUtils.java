package com.grinderwolf.swm.plugin.loaders;

import com.github.luben.zstd.Zstd;
import com.flowpowered.nbt.*;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.utils.SlimeFormat;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.impl.CraftSlimeChunk;
import com.grinderwolf.swm.api.world.impl.CraftSlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.plugin.log.Logging;

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
            byte[] header = new byte[SlimeFormat.SLIME_HEADER.length];
            input.readFully(header);

            if (!Arrays.equals(SlimeFormat.SLIME_HEADER, header)) {
                throw new CorruptedWorldException(worldName);
            }

            // Read magic number and version
            byte version = input.readByte();
            if (version > SlimeFormat.SLIME_VERSION) {
                throw new NewerFormatException(version);
            }

            // Read world version
            byte worldVersion = input.readByte();

            // Read chunk coordinates
            short minX = input.readShort();
            short minZ = input.readShort();
            short maxX = input.readShort();
            short maxZ = input.readShort();

            // Read chunk bitmask
            int chunkBitsetSize = (((maxX - minX + 1) * (maxZ - minZ + 1) + 7) / 8);
            byte[] chunkBitmaskBytes = readBytes(input, chunkBitsetSize);
            BitSet chunkBitmask = BitSet.valueOf(chunkBitmaskBytes);

            Map<Long, SlimeChunk> chunks = new HashMap<>();
            Map<Integer, byte[]> paletteCache = new HashMap<>();

            // Read chunk data
            int compressedChunkLen = input.readInt();
            int chunkLen = input.readInt();
            byte[] compressedChunkData = readBytes(input, compressedChunkLen);
            byte[] chunkData = Zstd.decompress(compressedChunkData);

            // Read chunks
            ByteArrayInputStream chunkBais = new ByteArrayInputStream(chunkData);
            DataInputStream chunkIn = new DataInputStream(chunkBais);

            for (int i = 0; i < chunkBitmask.length(); i++) {
                if (chunkBitmask.get(i)) {
                    int chunkX = minX + (i % (maxX - minX + 1));
                    int chunkZ = minZ + (i / (maxX - minX + 1));

                    try {
                        SlimeChunk chunk = deserializeChunk(
                                chunkIn, version, worldVersion,
                                chunkX, chunkZ, worldName, paletteCache
                        );
                        if (chunk != null) {
                            long chunkKey = makeChunkKey(chunkX, chunkZ);
                            chunks.put(chunkKey, chunk);
                        }
                    } catch (Exception e) {
                        Logging.warning("Failed to deserialize chunk at " + chunkX + ", " + chunkZ, e);
                    }
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
            byte worldVersion,
            int chunkX,
            int chunkZ,
            String worldName,
            Map<Integer, byte[]> paletteCache
    ) throws IOException, CorruptedWorldException {
        SlimeChunkSection[] sections = new SlimeChunkSection[16];

        // Read section bitmask
        byte[] sectionBitmask = readBytes(input, 2);
        BitSet sectionBitmaskBits = BitSet.valueOf(sectionBitmask);

        for (int i = 0; i < 16; i++) {
            if (sectionBitmaskBits.get(i)) {
                // Read block light
                boolean hasBlockLight = input.readBoolean();

                byte[] blockLight = null;
                if (hasBlockLight) {
                    blockLight = readBytes(input, 2048);
                }

                if (worldVersion >= 4) {
                    // Read palette and block states (new format)
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
                    // Read old format blocks
                    input.skipBytes(4096);
                    input.skipBytes(2048);
                }

                // Read sky light
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
    public static byte[] decompress(byte[] compressed) throws IOException {
        if (compressed == null || compressed.length == 0) {
            return new byte[0];
        }
        return Zstd.decompress(compressed);
    }
}
