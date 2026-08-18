# PP TV（皮皮 TV）

Android TV 用 Emby 客户端。英文名 **PP TV**，中文名 **皮皮 TV**，包名 `magi.aenerv7.ppembytv`。
功能全盘参考 [ChaiChaiEmbyTV](https://github.com/dh374374/ChaiChaiEmbyTV)（不含弹幕功能），从零实现。
Release 使用 `MAGI-OpenSource.jks` 签名（alias: `magi-opensource`）。
应用图标为深色圆角底 + 白色线条兔（来自开源图标库 [Lucide](https://lucide.dev) 的 `rabbit` 图标，MIT 许可），
源文件在 `.tools/rabbit-icons/`。

## 功能特色

- 🎬 **原生 TV 体验** - Jetpack Compose + TV 焦点导航，专为遥控器操作优化
- 🎮 **播放器功能** - 倍速播放、多音轨/字幕切换、外挂字幕延迟调整
- 📱 **扫码配置** - 手机扫码快速配置服务器（电视端启动本地配置服务）
- 🔄 **播放进度同步** - 自动同步播放进度到 Emby 服务器（Sessions/Playing 系列接口）
- 📺 **多服务器管理** - 支持添加多个 Emby 服务器，账号登录（AuthenticateByName）
- 🌐 **代理支持** - 全局 http/socks5 代理设置（SOCKS5 域名由代理解析，可绕过局域网）

## 技术栈

- **UI**: Kotlin + Jetpack Compose + TV Material（androidx.tv:tv-material）
- **播放器**: AndroidX Media3（ExoPlayer + HLS/DASH）+ OkHttp 数据源
- **网络**: Retrofit + OkHttp + kotlinx.serialization
- **架构**: MVVM 风格 + Kotlin Coroutines + Flow + DataStore
- **其他**: ZXing（二维码生成）、NanoHTTPD（本地扫码配置服务）、Coil（图片加载）

## 界面说明

- **服务器列表**：多服务器管理，手动添加 / 扫码配置 / 进入 / 删除
- **首页**：继续观看、接下来观看、最新电影、最新剧集
- **电影 / 电视剧**：媒体库分页网格浏览
- **搜索**：电视端屏幕键盘输入，全局搜索（Search/Hints）
- **详情页**：电影详情（背景图、简介、收藏、播放）、剧集详情（季 → 集 → 播放）
- **播放器**：倍速（0.5x~2x）、±跳转、音轨切换、字幕切换/关闭、字幕延迟 ±0.5s、进度同步
- **设置**：服务器管理、http/socks5 代理、关于

## 构建

**构建/测试环境已自包含在项目目录内**（不依赖系统安装）：

| 目录 | 内容 |
|------|------|
| `.tools/jdk17` | JDK 17（Temurin） |
| `.android-sdk` | Android SDK（platform 35、build-tools、platform-tools、emulator、TV 系统镜像） |
| `.gradle-home` | Gradle 用户目录（依赖缓存、wrapper 发行版） |
| `.android` | Android 用户目录（AVD `ppembytv_tv`、adb key、AGP prefs 缓存） |
| `.tools/mock-emby` | Mock Emby 测试服务器（端到端冒烟测试用） |
| `.tools/jadx`、`.tools/apk-decompiled` | 参考 APK 反编译产物（开发参考用） |

> 上述目录已在 `.gitignore` 中排除（体积大且为本地环境）。`local.properties` 与
> `gradle.properties` 已指向项目内路径；`build.cmd` / `test-emulator.cmd` 会设置
> `JAVA_HOME`、`GRADLE_USER_HOME`、`ANDROID_HOME`、`ANDROID_USER_HOME`、
> `ANDROID_PREFS_ROOT` 等环境变量，保证构建/测试全程不向项目目录外写任何文件。
> 若整目录迁移，请同步更新上述脚本与属性文件中的绝对路径。

```bash
# 一键构建（自动使用项目内 JDK/SDK/Gradle 缓存，参数透传给 gradlew）
build.cmd :app:assembleDebug

# Release（R8 混淆 + ABI 拆分：arm64-v8a / armeabi-v7a / universal）
build.cmd :app:assembleRelease
```

模拟器测试（环境变量全部指向项目内目录）：

```bash
test-emulator.cmd avd-list         # 列出项目内 AVD
test-emulator.cmd boot             # 无窗口启动模拟器
test-emulator.cmd install-debug    # 安装 debug APK
```

> 模拟器自身会把 adb 密钥写入 `%USERPROFILE%\.android`（模拟器固定行为，无法重定向），
> `test-emulator.cmd` 会在会话结束后自动删除该目录并关闭 adb server，保证项目外无残留。

Mock Emby 服务器（配合 `adb reverse tcp:8096 tcp:8096` 做端到端测试）：

```bash
powershell -File .tools\mock-emby\mock-emby.ps1
```

产物位于 `app/build/outputs/apk/`：

| 架构 | 说明 |
|------|------|
| `arm64-v8a` | 64 位 ARM 设备（多数新电视盒子） |
| `armeabi-v7a` | 32 位 ARM 设备 |
| `universal` | 全架构通用包，体积较大 |

> Release 使用 debug 签名，可直接侧载安装；正式分发请自行配置签名。

## 使用说明

1. 安装 APK 到 Android TV 设备
2. 打开应用：
   - **手动添加**：填写服务器地址、端口、用户名、密码，点击「连接并登录」
   - **扫码配置**：电视屏幕显示二维码，手机扫码后在浏览器中填写服务器与账号信息，点击「同步到电视」
3. 添加后进入首页即可浏览和播放媒体
4. 在「设置」中配置 http/socks5 代理（可选；勾选「仅直连」的服务器绕过代理）
5. 播放进度自动同步回 Emby 服务器

## 扫码配置原理

电视端在局域网启动 NanoHTTPD 配置服务（端口 8765，被占用时回退 8750~8764），
屏幕展示 `http://<电视IP>:<端口>/` 的二维码；手机扫码后在网页表单中填写
协议 / 地址 / 端口 / 路径 / 用户名 / 密码，POST `/config` 后电视端自动保存服务器。

## 目录结构

```
app/src/main/java/com/ppembytv/
├── MainActivity.kt          # 入口 + 导航框架
├── PlayerActivity.kt        # 全屏播放器
├── api/                     # Emby REST API（Retrofit）+ 会话/网络层
├── data/                    # 服务器/代理配置模型与 DataStore 持久化
├── playback/                # 播放 URL 构建、进度上报
├── server/                  # 本地扫码配置服务（NanoHTTPD）
├── ui/
│   ├── components/          # TV 焦点组件（卡片/按钮）
│   ├── player/              # 播放器控制栏、字幕延迟、轨道切换
│   ├── screens/             # 服务器/首页/媒体库/搜索/详情/设置
│   └── theme/               # 深色 TV 主题
└── util/                    # 二维码、格式化等工具
```

## 致谢

- [Emby](https://emby.media/) - 媒体服务器
- [ChaiChaiEmbyTV](https://github.com/dh374374/ChaiChaiEmbyTV) - 功能参考项目（未使用其代码）
- [androidx media](https://github.com/androidx/media) - 播放器

## 许可证

本项目仅供学习交流使用。
