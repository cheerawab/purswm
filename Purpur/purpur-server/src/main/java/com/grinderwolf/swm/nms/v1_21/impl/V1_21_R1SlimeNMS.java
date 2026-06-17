package com.grinderwolf.swm.nms.v1_21.impl.nms;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.SlimeException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperty;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.SlimeNMS;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.world.CraftChunk;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.world.WorldType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.LayerKey;
import net.minecraft.resources.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BigBiome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.BlendedFeatureReceiver;
import net.minecraft.world.level.saveddata.maps.MapData;
import net.minecraft.world.level.storage.MainLevelStorage;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * v1_21 R1 NMS 實現
 * 負責將 SlimeWorld 轉換為 Minecraft 伺服器世界
 */
public class V1_21_R1SlimeNMS implements SlimeNMS {
    
    private static final SlimeNMS INSTANCE = new V1_21_R1SlimeNMS();
    
    public static SlimeNMS getInstance() {
        return INSTANCE;
    }
    
    private V1_21_R1SlimeNMS() {}
    
    @Override
    public World loadWorld(String worldName, SlimeWorld slimeWorld, WorldType worldType, boolean hardcore) {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null) {
                throw new IllegalStateException("Server is not running!");
            }
            
            CraftServer craftServer = server.server;
            
            // 創建 SlimeWorldCustom 實例
            SlimeWorld slimeWorldCopy = slimeWorld;
            
            // 創建 ServerLevel
            ServerLevel serverLevel = createServerLevel(
                server,
                worldName,
                slimeWorldCopy,
                worldType,
                hardcore
            );
            
            if (serverLevel == null) {
                throw new SlimeException("Failed to create World");
            }
            
            // 添加到 Server 世界列表
            server.addLevel(serverLevel);
            
            // 創建 Bukkit World 包裝
            World bukkitWorld = craftServer.addWorld(serverLevel, worldName);
            
            return bukkitWorld;
            
        } catch (Exception e) {
            throw new SlimeException("Error loading world", e);
        }
    }
    
    /**
     * 創建 ServerLevel 實例
     */
    private ServerLevel createServerLevel(
        MinecraftServer server,
        String worldName,
        SlimeWorld slimeWorld,
        WorldType worldType,
        boolean hardcore
    ) {
        try {
            Path worldPath = server.getWorldPath(new File(worldName));
            
            // 創建 LevelStorageSource
            LevelStorageSource.LevelStorageSourceConstructor storage =
                new LevelStorageSource().createAccess(worldName, server.overworldType(), worldPath);
            
            // 創建 ServerLevel 配置
            SlimeWorldCustom slimeWorldCustom = new SlimeWorldCustom(
                slimeWorld,
                worldName,
                worldType,
                hardcore,
                storage
            );
            
            // 創建 ServerChunkCache
            ServerChunkCache chunkProvider = createChunkProvider(
                server,
                worldName,
                slimeWorld
            );
            
            // 創建 RegistryAccess
            RegistryAccess.Frozen registryAccess = buildRegistryAccess(server, slimeWorld);
            
            // 創建 ServerLevel
            ServerLevel serverLevel = new ServerLevelCustom(
                server,
                server.executor,
                storage,
                registryAccess,
                slimeWorldCustom.getDimensionType().access(),
                chunkProvider,
                slimeWorldCustom.getDimensionType(),
                slimeWorldCustom.getDebugWorldScene(),
                slimeWorldCustom.getRecipeTags(),
                slimeWorldCustom.shouldGenerateFeatures()
            );
            
            // 加載世界數據
            loadWorldData(serverLevel, slimeWorld);
            
            return serverLevel;
            
        } catch (Exception e) {
            throw new SlimeException("Error creating ServerLevel", e);
        }
    }
    
    /**
     * 創建 ChunkProvider
     */
    private ServerChunkCache createChunkProvider(
        MinecraftServer server,
        String worldName,
        SlimeWorld slimeWorld
    ) {
        int viewDistance = server.getIntProperty("view-distance", 10);
        boolean verboseChunkLoading = server.getBooleanProperty("verbose", false);
        
        return new ServerChunkCacheCustom(
            server.server,
            server.executor,
            new LevelStorageSource.LevelStorageSourceConstructor() {
                @Override
                public Path getDirectory() { return null; }
                @Override
                public boolean isCustomDimension() { return true; }
                @Override
                public String getId() { return worldName; }
            },
            slimeWorld.getProperties().get(SlimePropertyMap.SPAWN_CHUNK_RADIUS),
            viewDistance,
            verboseChunkLoading,
            server.getWorldGenSettings(),
            server.reloadableRegistries,
            null // 不執行任務
        );
    }
    
    /**
     * 創建 registry 上下文
     */
    private RegistryAccess.Frozen buildRegistryAccess(
        MinecraftServer server,
        SlimeWorld slimeWorld
    ) {
        // 從伺服器獲取 registry 數據
        RegistryAccess.Frozen frozen = server.registryAccess();
        return frozen;
    }
    
    /**
     * 從 Slime 數據加載 World 數據
     */
    private void loadWorldData(ServerLevel level, SlimeWorld slimeWorld) {
        // TODO: 實現具體的世界數據加載邏輯
        // 1. 加載 slime chunks
        // 2. 設置 biome 數據
        // 3. 加載 tile entities
        // 4. 加載實體數據
    }
    
    /**
     * 從 SlimeChunk 創建 Chunk
     */
    public static LevelChunk createLevelChunk(
        ServerLevel level,
        com.grinderwolf.swm.api.world.SlimeChunk slimeChunk
    ) {
        ChunkPos chunkPos = new ChunkPos(slimeChunk.getX(), slimeChunk.getZ());
        
        // 創建 ProtoChunk (用於初始化)
        ProtoChunk protoChunk = new ProtoChunk(chunkPos, level);
        
        // 設置 slime 數據
        setSlimeData(protoChunk, slimeChunk);
        
        // 轉換為 LevelChunk
        return protoChunk.getLevelChunk();
    }
    
    /**
     * 設置 slime chunk 數據
     */
    private static void setSlimeData(ProtoChunk chunk, com.grinderwolf.swm.api.world.SlimeChunk slimeChunk) {
        // 設定高度圖數據
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y += 4) {
                    // 從 NBT 數據重建 BlockState
                    // TODO: 實現具體的 nbt -> blockstate 映射
                }
            }
        }
        
        // 設置 tile entities
        if (slimeChunk.hasTileEntities()) {
            ListTag tileEntityTags = (ListTag) slimeChunk.getNbt().get("TileEntities");
            if (tileEntityTags != null) {
                for (int i = 0; i < tileEntityTags.size(); i++) {
                    CompoundTag te = tileEntityTags.getCompound(i);
                    // TODO: 設置 tile entity 到 chunk
                }
            }
        }
        
        // 設置實體
        if (slimeChunk.hasEntities()) {
            ListTag entityTags = (ListTag) slimeChunk.getNbt().get("Entities");
            if (entityTags != null) {
                for (int i = 0; i < entityTags.size(); i++) {
                    CompoundTag entityTag = entityTags.getCompound(i);
                    // TODO: 創建實體
                }
            }
        }
    }
    
    /**
     * 創建世界文件
     */
    public static void createWorldFile(String worldName, Path path) {
        try {
            // 創建基本文件結構
            File levelDat = new File(path.toFile(), "level.dat");
            File entities = new File(path.toFile(), "entities");
            File playerdata = new File(path.toFile(), "playerdata");
            File stats = new File(path.toFile(), "stats");
            File region = new File(path.toFile(), "dimension");
            
            // 創建目錄
            entities.mkdirs();
            playerdata.mkdirs();
            stats.mkdirs();
            region.mkdirs();
            
            // 創建空 level.dat
            if (!levelDat.exists()) {
                levelDat.createNewFile();
            }
            
        } catch (IOException e) {
            throw new SlimeException("Error creating world files", e);
        }
    }
}
