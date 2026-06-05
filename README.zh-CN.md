# Memos Glance

Memos Glance 是一个 Android 桌面小组件，用于在桌面快速查看 Memos 中最新一条未归档笔记。

项目使用 Kotlin 开发，主要使用 OkHttp、DataStore 和 Android AppWidget API。

## 功能

- 在 App 内配置 Memos 服务器地址和 Access Token。
- 通过 Memos REST API 获取 memo 列表。
- 只显示最新一条未归档 memo。
- 自动隐藏已归档 memo。
- 在桌面小组件中用可滚动区域显示 memo 内容。
- 支持基础 Markdown 显示：
  - 标题
  - 普通段落
  - 无序列表
  - 引用
  - 行内代码
  - 链接文本
  - Markdown 任务框，例如 `- [ ]` 和 `- [x]`
- 点击小组件内容会使用浏览器打开对应的 Memos 页面。
- 小组件右上角提供刷新按钮。

## 当前交互说明

小组件里的 Markdown 任务框只做显示，不支持在桌面中直接勾选。

原因是部分 Android 桌面启动器对“可滚动小组件列表项点击”的支持不稳定。为了保证行为可靠，Memos Glance 采用点击内容后跳转浏览器打开 Memos 页面的方式。

你可以在 Memos 页面里编辑或勾选任务，然后回到桌面点击小组件刷新按钮。

## 使用方式

1. 使用 Android Studio 打开项目。
2. 等待 Gradle 同步完成。
3. 运行 App。
4. 填写 Memos 服务器地址和 Access Token。
5. 点击“保存并刷新小组件”。
6. 回到系统桌面，添加 “Memos Glance” 小组件。

服务器地址示例：

```text
https://memos.example.com
```

Access Token 需要在你的 Memos 账号设置中生成。

## 打包 APK

调试版 APK：

```powershell
cd C:\Users\XFCY2\Desktop\memos_widget
.\gradlew.bat assembleDebug
```

APK 生成位置：

```text
app\build\outputs\apk\debug\app-debug.apk
```

也可以在 Android Studio 中使用：

```text
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

## 项目结构

```text
app/src/main/java/com/example/memostodowidget/
├── data/
│   ├── MemosApiClient.kt
│   ├── MemosTodoRepository.kt
│   └── SettingsRepository.kt
├── domain/
│   ├── TodoItem.kt
│   └── TodoRepository.kt
├── ui/
│   └── MainActivity.kt
└── widget/
    ├── TodoWidgetProvider.kt
    ├── TodoWidgetRenderer.kt
    ├── MemoRemoteViewsService.kt
    ├── MarkdownParser.kt
    └── MarkdownLine.kt
```

## API 兼容

项目优先请求：

```text
GET /api/v1/memos
```

如果该接口不可用，会回退到旧接口：

```text
GET /api/memo
```

归档过滤会兼容常见字段：

- `state = ARCHIVED`
- `rowStatus = ARCHIVED`
- `status = ARCHIVED`
- `visibility = ARCHIVED`
- `archived = true`

## 后续可扩展方向

- 新增 memo。
- 在 App 内展示完整 Markdown 预览。
- 支持多个小组件配置。
- 支持选择标签或筛选条件。
- 如果目标桌面启动器支持稳定点击事件，可以重新启用小组件内任务同步。

