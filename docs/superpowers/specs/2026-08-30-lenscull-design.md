# LensCull 设计说明

## 目标

为摄影师提供一个完全离线、平板优先的 Android 选片目录：扫描全部可访问共享存储，快速浏览常见成片和 Panasonic RW2，按格式、目录、日期、星级和选片状态筛选，并在不删除原片的前提下记录评分。

## 架构

应用采用单模块 Kotlin/Compose 架构。`StorageScanner` 分批扫描文件并把索引、EXIF 缓存和选片状态写入 Room；`PhotoCatalogRepository` 用 Paging 提供组合查询；`RawPreviewExtractor` 从 RW2/DNG 提取内嵌 JPEG；`MetadataWriter` 隔离所有文件写操作。应用通过一个轻量 `AppContainer` 组装依赖，不引入运行时依赖注入框架。

扫描由唯一 WorkManager 前台任务执行，可取消并在应用离开前台后继续。单文件异常记录在对应照片上，不影响其余扫描。完成整轮扫描后才删除本轮未出现的旧索引，取消扫描不会清空目录。

## 数据安全

- `MANAGE_EXTERNAL_STORAGE` 只用于用户主动发起的照片扫描。
- JPEG/PNG/WebP 的评分先写入同目录临时副本，校验 XMP 与图像可读性后原子替换。
- RW2/DNG 永不修改，评分写标准同名 XMP sidecar；同目录同 basename 的多 RAW 冲突时只保存在 Room 并提示。
- HEIC/HEIF 只读取元数据，评分留在本地目录。
- 保留/淘汰标记不写入照片，可通过 JSON 备份和恢复。

## 交互

横屏平板使用网格/预览双栏，窄屏使用网格和全屏预览切换。预览支持分块缩放、双击、平移、前后切换和键盘评分。长按期间在右下角显示核心 EXIF；完整信息面板提供 GPS 坐标及系统地图跳转。
