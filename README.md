# Purswm

Purpur 1.21.7 結合 Slime World Manager 的伺服器核心框架。

## 概述

Purswm 是一個基於 [Purpur](https://purpurmc.org/) 修改的 Minecraft 伺服器核心，內建 [Slime World Manager](https://github.com/Grinderwolf/Slime-World-Manager) 功能，專門為 API 驅動的遊戲伺服器設計。

### 主要特性

- **零預設世界生成**：伺服器啟動時不創建任何 world/nether/end 維度
- **SLIM 世界管理**：完全集成 SWM，支持動態加載/卸載 slime 格式世界
- **遊戲實例 API**：提供完整的遊戲房間/實例管理系統
- **模板系統**：支持世界模板快速克隆
- **插件兼容**：完全兼容 Bukkit/Spigot/Paper 插件生態
- **按需加載**：世界仅在需要時加載，遊戲結束後自動卸載

## 快速開始

### 編譯

使用 GitHub Actions 自動編譯：

```bash
# 本地編譯（需要 JDK 21）
cd Purpur
./gradlew build
```

或在 CI/CD 中自動觸發。

### 部署

```bash
java -Xmx4G -Xms2G -jar purswm.jar
```

伺服器啟動後不創建任何世界文件，等待插件調用 API 加載 slime 世界。

## 架構

詳細的架構設計請查看 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## API 使用示例

```java
// 加載 slime 世界
PurswmAPI api = new PurswmAPI();
api.getWorldManager().loadWorld("example_world", Paths.get("worlds/example.slime"));

// 創建遊戲實例
String instanceId = api.getInstanceManager().createInstance("skywars-1", "worlds/templates/skywars.slime");

// 玩家加入實例
Player player = ...;
api.getInstanceManager().joinPlayer(player, instanceId);

// 卸載世界
api.getWorldManager().unloadWorld("example_world");
```

## 結構

```
purswm/                          # 主倉庫
├── docs/                        # 文檔
│   └── ARCHITECTURE.md          # 架構設計
├── .github/workflows/           # CI/CD
│   └── build.yml                # GitHub Actions 建置流程
├── Purpur/                      # 修改後的 Purpur 核心
│   ├── purpur-api/              # API 模塊
│   │   └── src/main/java/
│   │       ├── com/grinderwolf/swm/api/
│   │       │   └── ...          # SWM API
│   │       └── org/purpurmc/purpur/api/
│   │           ├── PurswmPlugin.java
│   │           ├── PurswmAPI.java
│   │           └── ...
│   └── purpur-server/           # 伺服器核心
│       └── src/main/java/
│           ├── com/grinderwolf/swm/
│           │   └── ...          # SWM 核心實作
│           └── org/purpurmc/purpur/
│               └── ...          # 原有 Purpur 代碼
└── README.md
```

## 開發指南

### 本地開發環境

1. **JDK 21** - 必需（Paper/Purpur 1.21.x 要求）
2. **Gradle 9.1.0+** - 自動管理
3. **Git** - 版本控制

### 專案修改流程

1. Fork 本倉庫
2. 在本地克隆原始 Purpur 和 SWM 源碼
3. 修改代碼
4. 運行 `cd Purpur && ./gradlew build` 編譯
5. 測試核心
6. 提交 PR

## 許可證

本專案基於 LGPL-2.1 和 Apache-2.0 許可證（取決於集成的模塊）。詳情請查看 [LICENSE](LICENSE) 文件。

- Purpur: LGPL-2.1
- Paper: LGPL-2.1
- Slime World Manager: MIT
- Purswm 新增部分: Apache-2.0

## 貢獻

歡迎提交 Issue 和 Pull Request！

## 聯繫

- GitHub Issues: https://github.com/cheerawab/purswm/issues
- Discussions: https://github.com/cheerawab/purswm/discussions
