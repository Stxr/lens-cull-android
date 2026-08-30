# LensCull

LensCull 是一款离线优先、平板优先的 Android 摄影选片工具。它扫描设备共享存储、SD 卡和可访问的 OTG 设备，支持大图预览、缩放、组合筛选、0–5 星评分、保留/淘汰标记以及 EXIF 快速查看。

## 支持范围

- Android 13（API 33）及以上；个人 APK/ADB 侧载。
- JPEG/JPG、PNG、WebP、HEIC/HEIF、DNG、Panasonic RW2。
- RW2/DNG 使用内嵌 JPEG 预览，不做 RAW 显影。
- JPEG/PNG/WebP 将 `xmp:Rating` 安全写入图片；RW2/DNG 写同名 `.xmp` sidecar；HEIC 评分仅保存在本地目录数据库。
- 不删除、不移动、不重命名照片；无网络、账号或遥测。

## 开发环境

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$JAVA_HOME/bin:$PATH"

./gradlew testDebugUnitTest
./gradlew lintDebug assembleDebug
./gradlew connectedDebugAndroidTest # 已启动模拟器时
```

安装到已连接设备：

```bash
android run --sdk="$ANDROID_HOME"
# 或：adb install -r app/build/outputs/apk/debug/app-debug.apk
```

首次启动后，按引导在系统设置中授予“管理所有文件”。该特殊权限用于核心的本机照片搜索功能，不适合未经审核直接发布到 Google Play。

## 已验证环境

- OpenJDK 17.0.20.1、Android CLI 1.0.15985488、ADB 37.0.1、Emulator 37.1.11。
- Pixel Tablet API 33（Android 13）与 API 36（Android 16）设备测试均通过。
- 设备测试会真实创建 JPEG、PNG、WebP，写入 `xmp:Rating` 后再用 ExifInterface 回读；手工验收还覆盖全盘扫描、组合格式过滤、平板双栏预览、长按 EXIF 和评分写回。

## 快捷键

- `0`–`5`：清除/设置星级
- `P`：保留
- `X`：淘汰
- `←` / `→`：上一张/下一张

完整设计见 [`docs/superpowers/specs/2026-08-30-lenscull-design.md`](docs/superpowers/specs/2026-08-30-lenscull-design.md)。
