# Memos Glance

Memos Glance 是一个 Android App 和桌面小组件，用于快速查看和更新 Memos 中的未归档笔记。

当前版本：`1.0.0`

项目使用 Kotlin 开发，主要使用 XML/ViewBinding、Compose Markdown、OkHttp、DataStore 和 Android AppWidget API。

## 功能

- 在 App 内配置 Memos 服务器地址和 Access Token。
- 通过 Memos REST API 获取 memo 列表。
- 在 App 内浏览最近的未归档 memo。
- 在 App 内发布新 memo。
- 在 App 内编辑、删除、置顶和归档 memo。
- 通过左侧菜单查看归档历史。
- 在 App 内勾选 Markdown 任务框并同步回 Memos。
- 在 App 内使用 Markdown 快捷输入，例如任务框、加粗、标题、列表、引用和代码。
- 在桌面小组件中显示一条未归档 memo，支持用户选择显示“最新 memo”或“置顶 memo”。
- 自动隐藏已归档 memo。
- 桌面小组件右上角提供新建和刷新按钮。
- 点击小组件里的 memo 内容会进入 App 查看。
- 小组件会缓存上一次成功展示的 memo，避免短暂刷新失败时变成错误状态。
- 支持基础 Markdown 显示：
  - 标题
  - 普通段落
  - 无序列表
  - 引用
  - 行内代码
  - 链接文本
  - Markdown 任务框，例如 `- [ ]` 和 `- [x]`

## 当前交互说明

App 内的 Markdown 任务框可以直接勾选并同步到 Memos，界面会先本地更新，避免刷新后跳回顶部。

小组件里的 Markdown 任务框只做显示，不支持在桌面中直接勾选。点击小组件内容会进入 App，点击右上角加号会直接进入新建 memo 编辑页。

你可以在 App 里编辑、勾选或发布 memo。修改当前小组件展示的 memo 后，小组件会优先使用本地更新后的内容，并在后台刷新服务器数据。

## 使用方式

1. 使用 Android Studio 打开项目。
2. 等待 Gradle 同步完成。
3. 运行 App。
4. 填写 Memos 服务器地址和 Access Token。
5. 点击“保存并登录”。
6. 在设置页选择小组件显示“最新 memo”或“置顶 memo”。
7. 回到系统桌面，添加 “Memos Glance” 小组件。

服务器地址示例：

```text
https://memos.example.com
```

Access Token 需要在你的 Memos 账号设置中生成。

## 打包 APK

版本号：

```text
versionName 1.0.0
versionCode 1
```

调试版 APK：

```powershell
cd C:\Users\XFCY2\Desktop\memos-glance
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
│   ├── MemosJsonParser.kt
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
    ├── TodoWidgetUpdater.kt
    ├── WidgetPreferences.kt
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

- 支持多个小组件配置。
- 支持选择标签或筛选条件。
- 支持更完整的 Markdown 渲染和附件预览。
