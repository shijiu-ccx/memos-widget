# Memos Glance

Memos Glance 是一个 Android App 和桌面小组件，用来快速查看、编辑、发布和更新 Memos 服务器中的 memo。

当前版本：`1.1.0`

项目使用 Kotlin 开发，主要使用 XML/ViewBinding、Jetpack Compose、OkHttp、DataStore 和 Android AppWidget API。

## 功能

- 在 App 内配置 Memos 服务器地址和 Access Token。
- 通过 Memos REST API 获取未归档和已归档 memo。
- 在 App 内浏览最近的未归档 memo。
- 在 App 内发布新 memo。
- 在 App 内编辑、删除、置顶、取消置顶和归档 memo。
- 通过左侧抽屉查看归档历史。
- 在 App 内勾选 Markdown 任务框，并同步回 Memos。
- 在桌面小组件里直接勾选任务框。
- 支持缩进任务列表，包括空格缩进任务和 `  - [ ] 子任务` 这样的列表任务。
- App 和小组件都会保留嵌套任务、普通列表的缩进层级。
- 编辑器支持 Markdown 快捷输入：
  - 任务
  - 缩进，点击后插入两个空格
  - 加粗
  - 标题
  - 列表
  - 引用
  - 代码
- 编辑长 memo 时，输入框固定高度并在框内滚动，快捷按钮和保存按钮会保持在输入法上方。
- 在桌面小组件中显示一条未归档 memo，可选择显示“最新 memo”或“置顶 memo”。
- 小组件会充分利用可用高度，不再固定只显示少量行。
- 桌面小组件右上角提供新建和刷新按钮。
- 点击小组件里的 memo 内容会进入 App。
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

App 内的 Markdown 任务框可以直接勾选并同步到 Memos。界面会先本地更新，避免勾选任务后列表跳回顶部。

桌面小组件里的任务框也可以直接勾选。更新完成后，小组件会刷新显示内容，保持和 Memos 同步。

编辑长 memo 时，正文输入框保持固定高度，内容在输入框内部滚动。Markdown 快捷按钮行和保存按钮会保持在输入法上方。

修改当前小组件展示的 memo 后，小组件会优先使用本地更新后的内容，并在后台刷新服务器数据。

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
versionName 1.1.0
versionCode 2
```

调试版 APK：

```powershell
cd C:\Users\PC\Desktop\memos-glance
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

## 发布说明

### 1.1.0

- 支持空格缩进和列表写法的嵌套 Markdown 任务。
- App 和桌面小组件中的任务框都可以直接勾选并同步到 Memos。
- 修复普通列表导致 memo 卡片异常变高的问题。
- 修复编辑长 memo 时快捷按钮和保存按钮被输入法遮挡的问题。
- 新增“缩进”快捷按钮，一键插入两个空格。
- 修复小组件预览固定只显示少量行的问题，现在会利用可用高度。
- 增加缩进任务和列表解析测试。

### 1.0.0

- 增加完整 App 体验：浏览、发布、编辑、删除、置顶、归档和查看归档 memo。
- 增加桌面小组件，可显示最新 memo 或置顶 memo。

## 后续可扩展方向

- 支持多个小组件配置。
- 支持选择标签或筛选条件。
- 支持更完整的 Markdown 渲染和附件预览。
