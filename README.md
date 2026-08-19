# PP TV（皮皮 TV）

Android TV 用 Emby 客户端。英文名 **PP TV**，中文名 **皮皮 TV**，包名 `magi.aenerv7.ppembytv`。

本版本是参考 [ChaiChaiEmbyTV](https://github.com/dh374374/ChaiChaiEmbyTV)（v0.3.1-alpha2，`com.dh.myembyapp`，即「柴柴emby」）的**反编译 APK 完全复刻**，并**移除了弹幕功能**。

> 背景：参考项目的 GitHub 源码已全部删除（所有分支/标签只剩 README），因此只能依赖其 APK 反编译产物（`.tools/apk-decompiled/`，jadx 输出）进行复刻。

## 功能特色

- 🎬 **原生 TV 体验** - Jetpack Compose + D-pad 焦点导航，专为遥控器操作优化
- 🎮 **播放器功能** - 倍速（0.5x~2x）、±跳转、音轨切换、字幕切换/关闭、断点续播、直连/转码自动选择、播放进度同步（Sessions/Playing 系列接口）
- 📱 **扫码配置** - 电视端本地启动 NanoHTTPD 配置服务，手机扫码填表即可配置（服务器 / 代理 / 备用线路 / 服务器图标库 / 在线字幕 / WebDAV / 字幕字体等 9 项）
- ⌨️ **电视软键盘** - 内置屏幕键盘，可手动输入服务器地址/账号/密码等
- 📺 **多服务器管理** - 多 Emby 服务器、账号登录（AuthenticateByName）、备用线路（BackupRoute）
- 🌐 **代理支持** - 全局 http/https/socks5 代理（SOCKS5 域名由代理解析，可绕过局域网、可「仅直连」）
- 🖥️ **DLNA 投屏** - 本地 DLNA 渲染器（SSDP 发现 + UPnP SOAP），可被手机/其它设备投屏
- 🎞️ **剧集与直播** - 剧集详情（季 → 集 → 播放）、Live TV 频道列表与当前节目
- 🔍 **搜索联想** - 全局搜索 + 爱奇艺 suggest 联想
- 🎬 **片头片尾跳过**、**字幕偏好**、**解码器/音频直通设置**
- 🔗 **Trakt 同步** - 设备码授权 + 播放进度/已看同步设置
- 💾 **播放进度同步** - 自动同步到 Emby 服务器

## 技术栈

- **UI**: Kotlin + Jetpack Compose + TV Material（`androidx.tv:tv-material`，因 `tv-material3` 未发布到 Maven）
- **播放器**: AndroidX Media3（ExoPlayer + HLS/DASH）
- **网络**: Retrofit + OkHttp + **Gson**
- **架构**: Kotlin Coroutines + Flow + DataStore（部分）+ SharedPreferences
- **其他**: ZXing（二维码生成）、NanoHTTPD（本地扫码配置服务）、Coil（图片加载）、Lottie

## 界面说明

- **服务器列表**：多服务器管理，手动添加 / 扫码配置 / 进入 / 删除
- **首页**：媒体库入口、继续观看、最新媒体
- **媒体库**：分页网格浏览（电影/剧集/直播等，直播走专用频道列表）
- **搜索**：电视软键盘输入，全局搜索（含爱奇艺联想）
- **详情页**：电影详情（简介、播放）、剧集详情（季 → 集 → 播放）
- **播放器**：倍速、±30s 跳转、音轨切换、字幕切换/关闭、断点续播、进度同步
- **设置**：代理 / 解码器与音频直通 / DLNA / 播放与字幕（片头片尾跳过、字幕亮度）/ Trakt 授权 / 同步与其它

## 构建

**构建/测试环境已自包含在项目目录内**（不依赖系统安装）：

| 目录 | 内容 |
|------|------|
| `.tools/jdk17` | JDK 17（Temurin） |
| `.android-sdk` | Android SDK（platform 35、build-tools、platform-tools、emulator、TV 系统镜像） |
| `.gradle-home` | Gradle 用户目录（依赖缓存、wrapper 发行版） |
| `.android` | Android 用户目录（AVD `ppembytv_tv`、adb key、AGP prefs 缓存） |
| `.tools/mock-emby` | Mock Emby 测试服务器（端到端冒烟测试用） |
| `.tools/jadx`、`.tools/apk-decompiled` | 参考 APK 反编译产物（复刻依据） |

> 上述目录已在 `.gitignore` 中排除（体积大且为本地环境）。`local.properties` 与
> `gradle.properties` 已指向项目内路径；`build.cmd` / `test-emulator.cmd` 会设置
> `JAVA_HOME`、`GRADLE_USER_HOME`、`ANDROID_HOME`、`ANDROID_USER_HOME`、
> `ANDROID_PREFS_ROOT` 等环境变量，保证构建/测试全程不向项目目录外写任何文件。
> 若整目录迁移，请同步更新上述脚本与属性文件中的绝对路径。

```bash
# 一键构建（自动使用项目内 JDK/SDK/Gradle 缓存，参数透传给 gradlew）
build.cmd :app:assembleDebug

# Release（ABI 拆分：arm64-v8a / armeabi-v7a / universal；未开混淆）
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
   - **手动添加**：填写服务器地址、端口、用户名、密码（电视软键盘输入），点击「保存并连接」
   - **扫码配置**：电视屏幕显示二维码，手机扫码后在浏览器中填写服务器与账号信息，点击「同步到电视」
3. 添加后进入首页即可浏览和播放媒体
4. 在「设置」中配置 http/socks5 代理、解码器、DLNA、Trakt 等（均为可选）
5. 播放进度自动同步回 Emby 服务器

## 扫码配置原理

电视端在局域网启动 NanoHTTPD 配置服务，屏幕展示 `http://<电视IP>:<端口>/` 的二维码；
手机扫码后在网页表单中填写并提交，电视端自动保存。各配置项端口（被占用时自动回退）：

- 服务器：8765（回退 8750~8764）
- 代理：8760~8764
- 备用线路：8771~8799
- 在线字幕：8770~8799
- WebDAV 同步：8772~8799
- 字幕字体上传：8768~8799
- 服务器图标库：8780~8799
- 搜索输入：8767~8799

## 目录结构

```
app/src/main/java/magi/aenerv7/ppembytv/
├── MainActivity.kt              # 入口 + 恢复服务器 + DLNA 接收器
├── PPEmbyTVApp.kt               # Application：Coil 图片加载器、崩溃处理、代理加载
├── DlnaPlayRequestReceiver.kt   # DLNA 投屏广播接收器
├── data/
│   ├── model/                   # Emby + Trakt 模型（Gson @SerializedName）、媒体源优先级
│   ├── api/                     # Retrofit 接口、OkHttp 客户端（RetrofitClient/EmbyApiService/Trakt/爱奇艺）
│   ├── preferences/             # SharedPreferences 持久化（Server/User/AggregateSearch）
│   └── *.kt                     # 代理/解码器/字幕/片头片尾/Trakt/WebDAV/在线字幕 设置
├── server/                      # 9 个 NanoHTTPD 扫码配置服务 + Manager
├── dlna/                        # DLNA 渲染器（SSDP + UPnP SOAP）
├── ui/
│   ├── AppRoot.kt               # 全部屏幕 + 导航 + 设置子页 + 扫码二维码 overlay
│   ├── components/              # TV 焦点组件（TvComponents）、电视软键盘（TvKeyboard）
│   ├── player/                  # Media3 播放器
│   └── theme/                   # 深色主题
└── util/                        # 二维码生成（ZXing）
```

## 已知限制

反编译产物里 UI/同步等执行逻辑被 R8 混淆（无 mapping 文件），以下功能按**自实现**补齐（不再是逐字复刻）：

- **WebDAV 同步（自实现）**：设置 → 同步与其它 → 立即上传 / 立即下载；固定使用目录 `PPEmbyTV/` 与文件 `sync-config.json`，同步服务器配置与应用设置（代理 / 图标库 URL / DLNA / 解码器 / Trakt 基础配置 / 剧集与媒体库排序），为手动触发。
- **ASS 字幕增强渲染（自实现）**：手机扫码上传 ttf/otf 字体后，播放器通过 Media3 SubtitleView（CaptionStyleCompat）应用自定义字体；「播放与字幕设置」中可调整字幕字号与颜色。
- **在线字幕（Assrt）已完全移除**，不再提供。

未实现：
- 参考 APK 的 ASS 走 libass 原生位图渲染（native 依赖重），未移植；当前为 Media3 文本字幕增强渲染。

详细进展、踩坑记录与下一步见 [`HANDOFF.md`](HANDOFF.md)。

## 致谢

- [Emby](https://emby.media/) - 媒体服务器
- [ChaiChaiEmbyTV](https://github.com/dh374374/ChaiChaiEmbyTV) - 复刻参考（基于其反编译 APK）
- [androidx media](https://github.com/androidx/media) - 播放器

## 许可证

本项目仅供学习交流使用。
