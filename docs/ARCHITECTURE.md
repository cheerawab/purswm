# Purswm Architecture

## 專案概述

Purswm 是一個基於 Purpur 1.21.7 修改的 Minecraft 伺服器核心，內建 Slime World Manager (SWM) 功能，專門設計為 API 驅動的遊戲伺服器框架。

## 核心特性

- **禁用預設世界生成**：伺服器啟動時不創建 world / nether / end 等任何世界文件
- **SLIM 世界管理**：完全集成 SWM，支持動態加載/卸載 slime 格式的世界
- **遊戲實例管理**：提供完整的遊戲房間、匹配、快照管理 API
- **世界模板系統**：支持從模板快速克隆世界實例
- **按需加載**：世界僅在需要時加載，遊戲結束後自動卸載

## 架構圖

```
┌─────────────────────────────────────────────────────────┐
│                  Purswm Core (1.21.7)                    │
│  基於 Purpur 修改，內建 SWM 核心，移除所有預設世界生成       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  MinecraftServer (Paper/Purpur 核心初始化修改)       │  │
│  │  - 禁用 prepenulateWorld() 預設世界生成            │  │
│  │  - 覆蓋世界創建流程，只允許 SWM 加載                │  │
│  └───────────────────────────────────────────────────┘  │
│                     │                                   │
│  ┌──────────────────▼─────────────────────────────────┐  │
│  │              SWM Core (內建核心層)                    │  │
│  │  ┌─────────────┐ ┌──────────┐ ┌────────────────┐  │  │
│  │  │ SWM API     │ │ NMS Core │ │ NMS v1_21      │  │  │
│  │  │ (插件層)    │ │ (通用層) │ │ (1.21.7 專用)  │  │  │
│  │  └─────────────┘ └──────────┘ └────────────────┘  │  │
│  │                                                     │  │
│  │  ├─ WorldLoaders                                    │  │
│  │  │  ├─ FileSlimeLoader (本地 .slime 檔案)           │  │
│  │  │  ├─ URISlimeLoader (遠端加載)                    │  │
│  │  │  └─ CustomSlimeDataLoader (自訂來源)             │  │
│  │  └─ WorldManager                                    │  │
│  │     ├─ createWorld(String name, SlimeWorld sl)     │  │
│  │     ├─ unloadWorld(String name, boolean save)      │  │
│  │     └─ cloneTemplate(String src, String dest)      │  │
│  └───────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │        自訂 Bukkit 插件層 (API 暴露)                 │  │
│  │                                                   │  │
│  │  ┌───────────────────────────────────────────┐    │  │
│  │  │  PurswmPlugin.java                        │    │  │
│  │  └───────────────────────────────────────────┘    │  │
│  │                                                   │  │
│  │  ┌───────────────────────────────────────────┐    │  │
│  │  │  API Layer                                │    │  │
│  │  │  ├─ PurswmAPI                             │    │  │
│  │  │  ├─ WorldAPI                              │    │  │
│  │  │  └─ InstanceAPI                           │    │  │
│  │  └───────────────────────────────────────────┘    │  │
│  │                                                   │  │
│  │  ┌───────────────────────────────────────────┐    │  │
│  │  │  Events                                   │    │  │
│  │  │  ├─ WorldLoadEvent                        │    │  │
│  │  │  ├─ WorldUnloadEvent                      │    │  │
│  │  │  └─ GameInstanceEvent                     │    │  │
│  │  └───────────────────────────────────────────┘    │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 世界生成禁用

### 修改位置

通過 Paper patch 機制覆蓋以下方法：

1. `MinecraftServer.prepareServer()` - 不調用 `generateLevel()` 和 `createDimensions()`
2. `WorldLoader` - 不載入預設世界
3. `CraftServer.createWorld()` - 只允許 SWM 創建的世界實例化

### 世界初始化流程

```
Main.main() → Bootstrap.initialize()
              ↓
MinecraftServer.<init>()
  ├── → loadProperties()              # 讀取 server.properties
  ├── → createLevelStorageSource()    # 創建目錄結構
  ├── → prepareServer()               # ⭐ 修改：跳過世界生成
  │    ├── [SKIP] generateLevel()
  │    ├── [SKIP] createDimensions()
  │    └── [SKIP] createStartStructure()
  ├── → loadWorldGenerators()         # 載入生成器（但不創建）
  └── → reloadDimensions()

伺服器啟動完成，等待 API 調用：
  ↓
WorldAPI.loadWorld("my_world", slimeFile)
  ↓
SWM 從 .slime 文件創建世界
  ↓
添加到伺服器並對玩家可用
```

## 依賴關係

```
purswm-parent/
├── purpur-api/                    # API 模塊 (供插件使用)
│   ├── 依賴: paper-api
│   ├── SWM API                 # 內建
│   └── Purswm API            # 自訂 API
│
└── purpur-server/                # 伺服器核心模塊
    ├── 依賴: paper-server
    ├── Purpur Source           # 原有 Purpur 源碼
    ├── SWM Core              # 內建 SWM
    │   ├── nms-common/       # NMS 通用層
    │   └── nms/v1_21/        # 1.21.7 專用 NMS
    └── Purswm Patch          # 禁用世界生成的 Patch
```

## 實作細節

### Purpur 源碼分析

原始的 Purpur 基於 Paper 構建，使用 paperweight 工具鏈。本地結構：

```
Purpur/                              # 根目錄 (Minecraft 版本: 26.1.2 = 1.21.7)
├── build.gradle.kts                  # paperweight patcher v2.0.0-beta.21
├── purpur-api/                       # 插件 API
│   └── src/main/java/org/purpurmc/purpur/
│       ├── entity/                   # 實體擴充
│       ├── event/                    # 事件擴充
│       └── ...
│
└── purpur-server/                    # 伺服器核心
    ├── paper-patches/                # Paper patch 副本
    └── src/main/java/org/purpurmc/purpur/
        ├── PurpurConfig.java         # 主配置
        ├── PurpurWorldConfig.java    # 世界配置
        ├── command/                  # 命令
        └── ...
```

### Paper NMS 源碼結構 (1.21.7)

```
net/minecraft/
├── server/
│   ├── MinecraftServer.java          # 伺服器核心
│   ├── WorldLoader.java              # 世界載入器
│   ├── Main.java                     # 主進入點
│   ├── level/
│   │   └── ServerLevel.java          # 世界維度
│   └── dedicated/
│       └── DedicatedServer.java
├── world/level/
│   ├── Level.java                    # 世界基類
│   ├── Chunk.java
│   └── levelgen/                     # 世界生成器
└── nbt/                             # NBT 數據
```

## 建置流程

1. **本地建置**:
   ```bash
   ./gradlew build
   ```

2. **GitHub Actions 自動建置**:
   - 觸發：push / pull_request / workflow_dispatch
   - 使用 JDK 21
   - 輸出: purswm-server.jar, purswm-api.jar

3. ** artifact 結構**:
   - `purswm-server-[version].jar` - 伺服器核心
   - `purswm-api-[version].jar` - API 庫

## License

- Purpur: LGPL-2.1
- Paper: LGPL-2.1
- Slime World Manager: MIT
- Purswm new code: Apache-2.0
