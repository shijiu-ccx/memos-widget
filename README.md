# Memos Todo Widget

一个使用 Kotlin 开发的 Android 桌面小组件，从 Memos REST API 获取包含 `#todo` 的 memo，并显示最近 5 条。

## 使用

1. 使用 Android Studio 打开项目并同步 Gradle。
2. 运行 App，填写 Memos 地址和 Access Token，点击“保存并刷新小组件”。
3. 在系统桌面添加“Memos 待办”小组件。

服务器地址示例：`https://memos.example.com`

项目优先请求 Memos v1 接口 `/api/v1/memos`，如接口不存在会回退到旧接口 `/api/memo`。筛选在本地完成，以兼容不同 Memos 版本。

## 扩展位置

- `TodoRepository`：待办读取接口，后续可增加完成、新增等写操作。
- `MemosApiClient`：Memos HTTP 请求及版本兼容。
- `TodoWidgetRenderer`：小组件展示逻辑。

