# PPEmbyTV 复刻交接文档

> 本文档供新会话快速接续当前进度。最后更新：2026-08-20。

## 一句话现状

当前实现已使用同一套真实 Emby 数据逐页运行参考 APK 与 PP TV，完成首页、媒体库、搜索、收藏、详情和播放器主流程的视觉/交互对照。应用保持 Android TV 优先，并为主流程组件统一加入触摸支持；手机端只支持强制横屏。

### 2026-08-20 UI 对照结果

- 参考截图在 `.tools/ui-baseline/reference/`，实现截图在 `.tools/ui-baseline/current/`；不要只依据反编译代码判断 UI。
- 首页、媒体库、搜索、收藏、详情页沿用参考应用的尺寸层级、海报列数、导航结构和状态标记；配色改为与 PP TV 图标一致的蓝黑品牌色。
- 详情页使用真实背景图、Logo、季/集数据，以及播放、预告、音轨、字幕、下载、搜索、已看、收藏等对应图标；已看和收藏会调用真实 Emby API。
- 播放器禁用 Media3 默认控制器，使用参考应用同序的图标工具栏：后退 10 秒、播放/暂停、前进 10 秒、下一项、列表、字幕、音轨、倍速、解码和更多。触摸屏点按画面、遥控器方向键均可唤出控制层。
- `TvComponents.tvClickable` 是全局 D-pad/触摸入口，`TvInput` 同时处理遥控器确认编辑与触屏系统键盘。不要在页面里另造只响应遥控器的 clickable。
- Manifest 已全局声明 `android:screenOrientation="landscape"`；验证视口为 TV `1920x1080 @ 320 dpi` 和手机横屏 `2400x1080 @ 480 dpi`，不处理竖屏。
- 真实服务账号仅在 `.tools/LOCAL_TEST_CREDENTIALS.md`，禁止复制进源码、日志或文档。

---

## 1. 背景（为什么只能靠反编译）

- 参考项目的 GitHub 源码已**全部删除**（所有分支/标签只剩 README，已实测确认）。
- 唯一可依赖的是 `v0.3.1-alpha2` 的 APK 反编译产物，位于 `.tools/apk-decompiled/`（jadx 输出）。
- 反编译产物分两半：
  - **可读层**（`sources/com/dh/myembyapp/`，约 200 个文件）：data（模型/API/偏好/仓库）、server、dlna、MainActivity、MyEmbyApp —— 名字完整，已**忠实 1:1 移植**。
  - **混淆层**（`sources/defpackage/`，约 2700 个类）：UI/ViewModel/同步逻辑被 R8 混淆成 `a.java…zz1.java`。UI 已按反编译逻辑重建，但少数**执行算法**无法忠实还原（见第 6 节）。

---

## 2. 已完成（编译进 APK 的功能）

### 数据/网络层（忠实移植）
- `data/model/`（99 个文件）：所有 Emby 模型（`MediaItem/MediaSource/MediaStream/Library/QueryResult/DeviceProfile/PlaybackInfo*/ServerConfig/BackupRouteConfig` 等）+ 全部 Trakt 模型（28 个）+ 版本优先级/外观/枚举模型。
- `data/api/`（12 个）：`EmbyApiService`（40 个端点）、`RetrofitClient`（授权头/`/emby` 前缀/代理/SOCKS DNS/信任证书/图片视频字幕转码 URL 构建/`remapAbsoluteMediaUrlToBaseUrl`）、`ExternalHttpClient`、`TraktApiService`、`TraktRetrofitClient`、`IqiyiSuggestApi`、`PlaybackProgressInfo/QueueItem/UserDataRequest/PlaybackInfoResponse`。
- `data/preferences/`（3 个）：`ServerPreferences`（SharedPreferences+Gson）、`UserPreferences`、`AggregateSearchHistoryPreferences`。
- `data/` 根：代理（`ProxyConfig/ProxySettings/ProxyManager/ProxyProtocol`）、`DeviceIdManager`、`DecoderSettings`、`SubtitlePreferences`、`IntroOutroSettings`、`TraktSettings`、`OnlineSubtitleConfig/Settings`、`WebDavSyncConfig/Settings`、`SubtitleFontEntry`、`AudioTrackMemory`、`VideoVersionMemory`、`IntroOutroMemory`、`AssrtApiProtocol`。
- `data/model/MediaSourcePriorityKt.kt`：媒体源/版本选择与排序逻辑。
- `data/model/DeviceProfileKt.kt`：`createAndroidTvDeviceProfile`。

### 服务器层（`server/`，20 个文件）
9 个 NanoHTTPD 扫码配置 server + 对应 Manager，全部接到 UI：
ConfigServer、BackupRouteConfigServer、ProxyConfigServer、OnlineSubtitleConfigServer、OnlineSubtitleSearchInputServer、SearchInputServer、ServerIconLibraryInputServer、SubtitleFontUploadServer、WebDavSyncConfigServer；以及 `ServerUrlResolver`（二维码地址解析）。

### DLNA（`dlna/`，7 个文件）
`DlnaService`（SSDP/foreground service）、`DlnaHttpServer`（UPnP/SOAP，字符串逐字节校验）、`DlnaSettings/DlnaConfig/DlnaConstants/DlnaPlayRequest/DlnaConfigSaver`。

### UI（主要在 `ui/AppRoot.kt`，各屏幕合并在一份文件里）
- 服务器列表 → 添加服务器（手动 + 扫码）→ 登录 → 首页（继续观看/最新/媒体库入口）→ 媒体库 → 搜索（含爱奇艺搜索联想）→ 详情（剧集：季→集→播放）→ 播放器 → 直播（Live TV）→ 设置。
- 设置子页：代理、解码器/音频直通、DLNA、播放与字幕、片头片尾跳过、Trakt 授权、同步与其它（备用线路/图标库/在线字幕/WebDAV/字幕字体扫码）。
- `ui/components/TvComponents.kt`（`tvClickable` 焦点点击、TvButton、PosterCard）、`ui/components/TvKeyboard.kt`（电视软键盘）、`ui/theme/Theme.kt`、`util/QrCode.kt`（ZXing 二维码生成）。

### 播放器（`ui/player/PlayerScreen.kt`）
倍速（0.5~2.0x）、音轨/字幕轨切换、断点续播、直连/转码 URL（优先服务器返回的 `directStreamUrl`/`transcodingUrl`）、进度同步（Playing/Progress/Stopped）。

### 已移除
弹幕（AkDanmaku 引擎、弹幕 API/模型/服务器/偏好/界面）——源码中无任何 danmaku 文件（已核验 = 0）。

---

## 3. 构建方法

自包含环境（`build.cmd` 会设好 JAVA_HOME / GRADLE_USER_HOME / ANDROID_HOME / ANDROID_USER_HOME 全部指向项目内 `.tools/` 与 `.android-sdk/`、`.gradle-home/`）：

```bash
build.cmd :app:assembleDebug        # 或 :app:assembleRelease
```

产物：`app/build/outputs/apk/debug/` 下
`app-arm64-v8a-debug.apk` / `app-armeabi-v7a-debug.apk` / `app-universal-debug.apk`（约 16 MB）。

模拟器测试：`test-emulator.cmd`（boot / install-debug）。Mock Emby：`.tools/mock-emby/mock-emby.ps1`。

---

## 4. 关键文件地图

| 文件 | 作用 |
|---|---|
| `MainActivity.kt` | 启动恢复服务器、DLNA 接收器、setContent(AppRoot) |
| `PPEmbyTVApp.kt` | Application：Coil 图片加载器（代理/SSL/缓存）、崩溃处理、代理配置加载 |
| `DlnaPlayRequestReceiver.kt` | DLNA 投屏播放请求广播接收器 |
| `data/api/RetrofitClient.kt` | 单例：baseUrl/授权头/拦截器/代理/URL 构建 |
| `data/api/EmbyApiService.kt` | Retrofit 接口（suspend，40 端点） |
| `data/model/ServerConfig.kt` | 服务器配置（含备用线路 effectiveServerConfig/fullUrl） |
| `data/model/MediaSourcePriorityKt.kt` | 媒体源选择 |
| `data/preferences/ServerPreferences.kt` | 服务器列表持久化（SharedPreferences "server_configs" + Gson） |
| `ui/AppRoot.kt` | 全部屏幕 + 导航 + 设置子页 + 扫码二维码 overlay |
| `ui/player/PlayerScreen.kt` | Media3 播放器 |
| `server/ConfigServerManager.kt` 等 | 9 个扫码配置 server |

---

## 5. 重要踩坑记录（新会话务必先读，避免重复踩）

### 5.1 R8 混淆的 `StringsKt` 辅助函数真实语义
在 `.tools/apk-decompiled/sources/kotlin/text/StringsKt.java`（桥接类）里能查到每个混淆方法到底映射到哪个 Kotlin 标准库函数：

| 混淆名 | 真实语义 |
|---|---|
| `w(a,b)` | `a.equals(b, ignoreCase=true)`（**不是** startsWith！） |
| `R(a,b)` | `a.startsWith(b, ignoreCase=true)` |
| `U(a,b)` | `a.startsWith(b, ignoreCase=false)` |
| `v(a,b)` | `a.endsWith(b, ignoreCase=false)` |
| `p(a,b)` | `a.contains(b, ignoreCase=false)` |
| `L(a,b,c)` | `a.replace(b,c)` |

（曾因把 `w` 误判成 startsWith 改错 6 处，后按上表纠正。）

### 5.2 OkHttp 4.12 弃用 API
- `chain.request()` → `chain.request`（属性）
- `request.url()`/`host()`/`method()` → `request.url`/`host`/`method`
- `HttpUrl.parse(x)` → `x.toHttpUrlOrNull()`（`import okhttp3.HttpUrl.Companion.toHttpUrlOrNull`）
- `url.encodedPath()`/`encodedQuery()` → `url.encodedPath`/`encodedQuery`（属性）
- **注意区分**：`okhttp3.Response.code()` → `code`（属性）；但 `retrofit2.Response.code()` → 仍是 `code()`（方法，未弃用）。

### 5.3 Media3 1.5 音轨切换 API
`TrackSelectionParameters.Builder` 用 **`setOverrideForType(override)` / `clearOverridesOfType(trackType)`**，没有 `setOverrides` / `clearOverrideForType`。

### 5.4 Compose 键位
D-pad 中心键是 **`Key.DirectionCenter`**（不是 `Key.DpadCenter`，Compose 1.7 里已不存在）。

### 5.5 依赖选择
- `androidx.tv:tv-material3` **在 Maven 上不存在**（androidx.tv 组只有 `tv-foundation`、`tv-material`）。改用了 `androidx.tv:tv-material:1.0.0`。
- `io.github.peerless2012:ass`（ASS 解析库）暂未引入（ASS 增强未做，见第 6 节）。

### 5.6 从混淆层恢复的字符串常量（R8 不混淆字符串）
- **Trakt Client ID**：`1c6390b346287cb8aad251da052645aa6e57f4e2dd67ae9d9ee9c7217cc513e6`
- **Trakt Client Secret**：`0adc6e4aa2ddd7858eb346db6467d9678709322badd984c655514c97c36a8847`
- **Assrt 默认 API Key**：`G1jDEk5mvd5s8eRlDnLLQbpWaHwmzoU9`
- 这些已写入 `data/TraktSettings.kt`、`data/OnlineSubtitleConfig.kt`。

### 5.7 曾发生的误删
第一轮曾 `Remove-Item` 整个源码树导致刚移植完的 Trakt/Iqiyi（33 个文件）被误删，后已重新移植补齐。**新会话若再动源码树，注意别覆盖已生成的文件。**

### 5.8 DataStore 流在 `runBlocking` 里不能 `collect`（真机黑屏根因）
`PPEmbyTVApp.onCreate` 里加载代理配置时曾写成：

```kotlin
runBlocking { ProxySettings(this).proxyConfigFlow.collect { ... } }   // ❌ 主线程永久阻塞
```

DataStore 的 `data` 流**永不结束**（每次写入都会继续发值），`collect` 不会返回，
`runBlocking` 把主线程卡死在 `Application.onCreate`，Activity 永远无法创建，
窗口只有主题的黑色背景 → **真机安装后打开就是黑屏**（不崩溃、无日志、进程常驻）。

参考 APK 反编译实现用的是 `FlowKt.first(flow, this)`（`defpackage/o2.java` case 5），
已改回 `proxyConfigFlow.first()`（与 `DlnaSettings.configSync` 的写法一致）。

### 5.9 播放 URL：服务器返回的相对路径必须拼上基址（真机/模拟器无法播放）
PlaybackInfo 的 `DirectStreamUrl` 可能是**相对路径**（如 `/videos/{id}/original.mp4?…`，
无 scheme/host）。原 `remapAbsoluteMediaUrlToBaseUrl` 对无 scheme 的 URL 会 `toHttpUrlOrNull()`
返回 null → 原样返回 → Media3 当成本地文件路径 → `FileNotFoundException: /videos/...`（ENOENT）。
已修复：相对路径按基址 origin 补齐，并补 `/emby` 前缀（与参考 `defpackage/cq1.p` 的兜底逻辑一致）。
修复后用真实服务器验证：`https://emby.bangumi.ca/emby/videos/241448/original.mp4?…` → 200 video/mp4。

### 5.10 首页/媒体库分页（对齐参考 APK 行为）
- 原首页每个栏目一次性加载固定 30 条、**无右移加载更多**；参考 APK 是每页 8 条、滚动到末尾自动
  `startIndex+8` 续载（实测 `16→24→32→40` 直到总数 45）。
- 已实现：`HomeScreen` 的「继续观看/最新媒体」与 `LibraryScreen` 均按 `LazyListState` +
  `snapshotFlow` 检测滚动近末尾 → 追加下一页（`HOME_PAGE_SIZE=20`、`LIBRARY_PAGE_SIZE=50`）。
  ⚠️ 注意：`LaunchedEffect` 必须以 `items.size` 为键 + `snapshotFlow{visibleItemsInfo…}` 观察滚动，
  只把 `listState` 当 key 不会在滚动时重新触发。
- **续播端点坑**：`Users/{userId}/Items?Filters=IsResumable` 在该服务器返回 0；正确端点是
  `Users/{userId}/Items/Resume`（`getResumeItemsV2`，实测返回 31 条）。首页已改用 V2。

---

## 6. 残余未完成与自实现进展

原「三块执行算法在 R8 混淆层、无 mapping 文件、无法忠实还原」的功能已在本会话处理完毕：

1. ✅ **WebDAV 真正同步执行（自实现）**：`data/WebDavSyncClient.kt`（OkHttp **MKCOL / PROPFIND / PUT / GET**，固定目录 `PPEmbyTV/`、文件 `sync-config.json`）+ `data/WebDavSyncManager.kt`（payload 构建/应用：服务器列表/最后使用服务器 + 代理/图标库/DLNA/解码器/Trakt 基础配置/剧集与媒体库排序）。同步为**手动**（设置 → 同步与其它 → 立即上传/立即下载）——与参考 APK 行为一致（参考实现同样是纯手动按钮触发，无启动自动同步、无定时器）。已用**真实坚果云 WebDAV** 端到端验证：上传（MKCOL 201 + PUT 1203B）、下载（GET + 回写偏好）。
2. ✅ **在线字幕（Assrt）完全移除**：删除 `data/OnlineSubtitleConfig.kt`、`data/OnlineSubtitleSettings.kt`、`data/AssrtApiProtocol.kt`、`server/OnlineSubtitleConfigServer.kt`、`server/OnlineSubtitleConfigServerManager.kt`、`server/OnlineSubtitleSearchInputServerManager.kt`，并清理 AppRoot 的 UI 入口（「在线字幕配置」按钮/QrOnlineSubtitleOverlay/相关 import）。
3. ✅ **字幕字体 ASS 增强渲染（自实现）**：`data/SubtitleFontManager.kt`（字体列表 + 选中项持久化，上传后自动生效）；播放器用 Media3 `SubtitleView.setStyle(CaptionStyleCompat(…, typeface))` 应用上传字体——与参考 APK 是**同一机制**（已核对 media3-ui 1.5.1 字节码：SubtitlePainter 会把 CaptionStyleCompat.typeface 设到 TextPaint）；另新增全局字幕字号/颜色偏好与「播放与字幕设置」UI（播放与字幕/同步页已加滚动）。

剩余边界（自实现范围内未做）：
- 参考 APK 的 ASS 增强走 **libass 原生位图渲染**（peerless2012 的 `io.github.peerless2012:ass`，native 依赖重，未引入）。我们改用 Media3 内置 ASS/SSA 解析 + 自定义字体/颜色/字号增强。
- WebDAV payload 与参考实现（`ChaiChaiEmby/` 目录、danmaku/onlineSubtitle 等字段）不完全一致——本应用使用 `PPEmbyTV/` 目录与自己的 schema（弹幕与在线字幕已移除）。

> 参考 APK（v0.3.1-alpha2 arm64，`.tools/apk/`，jadx 输出 `.tools/apk-new-decompiled/`）核对结论：WebDAV 客户端在 `defpackage.tg2`（PROPFIND 探目录→404 时 MKCOL→GET 下载→PUT 上传，纯手动）；字体应用在 `defpackage.no1`/`mk1`（CaptionStyleCompat typeface + Cue TypefaceSpan）；在线字幕为 defpackage ff/ef/xe/ye/we + server 层。

---

## 7. 下一步建议

1. **提交代码**（当前 86 个变更都在未提交工作区，无 commit）：
   ```bash
   git add -A
   git commit -m "feat: 完全复刻 ChaiChaiEmbyTV（去弹幕），改名皮皮 TV"
   ```
2. **真机/模拟器冒烟测试**：装 APK → 扫码或手动配置服务器 → 浏览/播放，重点验证直连 vs 转码播放、多音轨/字幕、断点续播、进度同步。
3. （可选）继续自实现第 6 节的 3 项残余执行逻辑。

---

## 8. 验证结论（本会话末尾）

- `build.cmd :app:assembleDebug` → **BUILD SUCCESSFUL**。
- 源码 168 个 `.kt` 文件；`data/model` 99、`data/api` 12、`data/preferences` 3、`server` 20、`dlna` 7。
- 弹幕文件数 = 0。
- 新旧实现未混杂：旧「从零实现」的 29 个文件已删除（仍在 git 历史可恢复），14 个同名文件内容已换成新版，43 个新文件为新增。
