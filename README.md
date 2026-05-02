# 铁铁汁 Android 客户端

铁铁汁 AI Agent 框架的 Android 客户端，支持本地模式和远程模式。

## 特性

- 🤖 本地模式：内置 tietiezhi-server，无需外部服务器
- 🌐 远程模式：连接远程 tietiezhi 服务
- 💬 流式/非流式对话
- 📝 Markdown 渲染
- 🎨 Material You 动态配色
- 💾 本地会话持久化（Room）
- ⚙️ 完整设置界面

## 技术栈

- Kotlin + Jetpack Compose
- Material Design 3 (Material You)
- Hilt 依赖注入
- Room 数据库
- DataStore Preferences
- Retrofit + OkHttp + SSE
- Kotlinx Serialization
- Coroutines + Flow

## 系统要求

- Android 7.0+ (API 24)
- ARM64 设备（本地模式）

## 构建

需要 Android Studio 或 Android SDK，使用 Gradle 构建：

```bash
./gradlew assembleRelease
```

## 项目结构

```
app/src/main/java/com/tietiezhi/apk/
├── di/           # Hilt 依赖注入
├── data/         # 数据层（Room/网络/存储）
├── domain/       # 领域层（模型/仓库接口）
├── ui/           # UI 层（Compose + ViewModel）
└── server/       # 本地服务管理
```

## 许可

MIT License
