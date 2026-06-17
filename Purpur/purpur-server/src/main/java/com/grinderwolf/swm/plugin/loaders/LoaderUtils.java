package com.grinderwolf.swm.plugin.loaders;

import com.github.luben.zstd.Zstd;
import com.flowpowered.nbt.*;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.utils.SlimeFormat;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.impl.CraftSlimeChunk;
import com.grinderwolf.swm.api.world.impl.CraftSlimeChunkSection;
import com.grinderwolf.swm.api.world.impl.CraftSlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Utility class for handling Slime Format data.
 */
public class LoaderUtils {

    /**
     * Decompress zstd compressed data.
     */
    public static byte[] decompress(byte[] compressed) throws IOException {
        if (compressed == null || compressed.length == 0) {
            return new byte[0];
        }
        return Zstd.decompress(compressed);
    }

    /**
     * Serializes world data to bytes.
     */
    public static byte[] serializeWorld(SlimeWorld world) {
        // Implementation in CraftSlimeWorld.serialize()
        return world.serialize();
    }

    /**
     * Deserializes a world from bytes.
     */
    public static SlimeWorld deserialize(
            String worldName,
            byte[] data,
            SlimePropertyMap properties,
            boolean readOnly,
            SlimeLoader loader
    ) throws CorruptedWorldException {
        if (data.length < 13) {
            throw new CorruptedWorldException(worldName);
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream input = new DataInputStream(bais);

            // Validate header
            byte[] header = new byte[SlimeFormat.SLIME_HEADER.length];
            input.readFully(header);

            if (!Arrays.equals(SlimeFormat.SLIME_HEADER, header)) {
                throw new CorruptedWorldException(worldName);
            }

            // Read version
            byte version = input.readByte();

            // Read world properties (should be in extra data, but we have separate properties)
            Map<Long, SlimeChunk> chunks = new HashMap<>();
            List<CompoundTag> worldMaps = new ArrayList<>();
            Map<Byte, byte[]> paletteCache = new HashMap<>();

            // Read lowest chunk coords
            short minXShort = input.readShort();
            short minZShort = input.readShort();
            int minX = (int) (minXShort & 0xFFFF);
            int minZ = (int) (minZShort & 0xFFFF);

            // Read width/depth
            short widthShort = input.readShort();
            short depthShort = input.readShort();
            int width = (int) (widthShort & 0xFFFF);
            int depth = (int) (depthShort & 0xFFFF);

            // Read chunk bitmask
            BitSet chunkBitSet = BitSet.valueOf(
                    readBytes(input, (int) Math.ceil((width * depth) / 8.0D))
            );

            // Read chunk data
            byte[] compressedChunkData = readBytes(input, input.readInt());

            // Read tile entities
            readBytes(input, input.readInt()); // compressed length
            int tileEntityCount = readInt(input);

            // Read entities
            boolean hasEntities = input.readBoolean();

            // Read height maps and other chunk data based on version
            for (int i = 0; i < width * depth; i++) {
                if (chunkBitSet.get(i)) {
                    int chunkX = (i % depth) + minX;
                    int chunkZ = (i / depth) + minZ;

                    // Skip based on format - simplified
                    try {
                        SlimeChunk chunk = deserializeChunk(
                                input, version, chunkX, chunkZ, worldName,
                                paletteCache, tileEntityCount, hasEntities
                        );
                        if (chunk != null) {
                            long key = (((long) chunk.getZ()) << 32) | (chunk.getX() & 0xFFFFFFFFL);
                            chunks.put(key, chunk);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            input.close();

            return new CraftSlimeWorld(
                    loader,
                    worldName,
                    chunks,
                    new CompoundTag("", new CompoundMap()),
                    worldMaps,
                    version,
                    properties,
                    readOnly,
                    true
            );

        } catch (IOException e) {
            throw new CorruptedWorldException(worldName);
        }
    }

    private static SlimeChunk deserializeChunk(
            DataInputStream input,
            byte version,
            int chunkX,
            int chunkZ,
            String worldName,
            Map<Byte, byte[]> paletteCache,
            int tileEntityCount,
            boolean hasEntities
    ) throws IOException {
        // Simplified implementation - in real code would deserialization logic
        SlimeChunkSection[] sections = new SlimeChunkSection[16];
        
        byte[] sectionBitmask = readBytes(input, 2);
        BitSet sectionBitSet = BitSet.valueOf(sectionBitmask);

        for (int i = 0; i < 16; i++) {
            if (sectionBitSet.get(i)) {
                boolean hasBlockLight = input.readBoolean();
                if (hasBlockLight) {
                    input.skipBytes(20); // Block light NibbleArray
                }

                // Read block states (simplified)
                if (version >= 0x04) {
                    int paletteSize = readInt(input);
                    for (int p = 0; p < paletteSize; p++) {
                        int size = readInt(input);
                        input.skipBytes(size); // skip palette entry
                    }
                    int blockStatesLen = readInt(input);
                    for (int s = 0; s < blockStatesLen; s++) {
                        input.readLong();
                    }
                } else {
                    input.skipBytes(4096); // blocks
                    input.skipBytes(2048); // data
                }

                boolean hasSkyLight = input.readBoolean();
                if (hasSkyLight) {
                    input.skipBytes(20); // sky light
                }

                sections[i] = null; // null if no block light
            }
        }

        // Read extra data (tile entities, entities, etc.)
        if (tileEntityCount > 0 && input.available() >= 4) {
            readBytes(input, input.readInt()); // compressed tile entities length
            if (input.available() > 0) {
                readBytes(input, input.readInt()); // tile entity data
            }
        }

        if (hasEntities && input.available() >= 1) {
            if (input.readBoolean()) {
                readBytes(input, input.readInt()); // compressed entities length
                if (input.available() > 0) {
                    readBytes(input, input.readInt()); // entities data
                }
            }
        }

        // Read extra data
        if (input.available() >= 4) {
            readBytes(input, input.readInt()); // compressed extra
            readBytes(input, input.readInt()); // extra data
        }

        return new CraftSlimeChunk(worldName, chunkX, chunkZ, sections);
    }

    private static byte[] readBytes(DataInputStream input, int length) throws IOException {
        if (length <= 0) {
            return new byte[0];
        }
        byte[] data = new byte[length];
        input.readFully(data);
        return data;
    }

    private static int readInt(DataInputStream input) throws IOException {
        return input.readInt() & 0xFFFFFFFF;
    }
}
