# Memos Glance

[中文说明](README.zh-CN.md)

Memos Glance is an Android app and home-screen widget for quickly viewing, editing, publishing, and updating memos from a Memos server.

Current version: `1.1.0`

The app is written in Kotlin and uses XML/ViewBinding, Jetpack Compose, OkHttp, DataStore, and Android AppWidget APIs.

## Features

- Configure a Memos server URL and Access Token in the app.
- Fetch active and archived memos through the Memos REST API.
- Browse recent active memos in the app.
- Publish new memos from the app.
- Edit, delete, pin, unpin, and archive memos from the app.
- View archived memo history from the left drawer.
- Toggle Markdown task checkboxes in the app and sync changes back to Memos.
- Toggle task checkboxes from the home-screen widget.
- Render indented task lists, including space-indented tasks and list-style tasks such as `  - [ ] child task`.
- Preserve indentation for nested tasks and bullet lists in the app and widget.
- Insert Markdown snippets from the editor:
  - task
  - indent, which inserts two spaces
  - bold
  - heading
  - list
  - quote
  - code
- Keep the editor buttons visible above the keyboard by using a fixed-height, internally scrollable memo input box.
- Show one active memo in the widget, with a setting for either the latest memo or a pinned memo.
- Use the available widget height instead of truncating after a small fixed number of lines.
- Add and refresh memos from the widget header.
- Tap widget memo content to open the app.
- Cache the last successfully displayed widget memo so transient refresh failures do not replace it with an error state.
- Render basic Markdown:
  - headings
  - paragraphs
  - bullet lists
  - quotes
  - inline code
  - readable link text
  - task checkboxes, such as `- [ ]` and `- [x]`

## Current Interaction Model

Markdown task checkboxes can be toggled directly inside the app and are synced back to Memos. The UI updates optimistically so checking a task does not jump the feed back to the top.

Widget task checkboxes can also be toggled directly. The widget refreshes after the update so the displayed memo stays in sync with Memos.

When editing a long memo, the memo input box keeps a fixed height and scrolls internally. The Markdown shortcut row and save button remain visible above the keyboard.

When the memo currently shown in the widget is edited in the app, the widget first updates from the local edited content and then refreshes server data in the background.

## Setup

1. Open the project in Android Studio.
2. Wait for Gradle sync to finish.
3. Run the app.
4. Enter your Memos server URL and Access Token.
5. Tap "Save and sign in".
6. Choose whether the widget shows the latest memo or a pinned memo from the settings screen.
7. Add the "Memos Glance" widget to your Android home screen.

Example server URL:

```text
https://memos.example.com
```

The Access Token should be generated from your Memos account settings.

## Build APK

Version:

```text
versionName 1.1.0
versionCode 2
```

Debug build:

```powershell
cd C:\Users\PC\Desktop\memos-glance
.\gradlew.bat assembleDebug
```

APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

You can also build from Android Studio:

```text
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

## Project Structure

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

## API Compatibility

Memos Glance first requests:

```text
GET /api/v1/memos
```

If that endpoint is unavailable, it falls back to:

```text
GET /api/memo
```

Archived memo filtering checks common fields:

- `state = ARCHIVED`
- `rowStatus = ARCHIVED`
- `status = ARCHIVED`
- `visibility = ARCHIVED`
- `archived = true`

## Release Notes

### 1.1.0

- Added nested task indentation support for space-indented and list-style Markdown tasks.
- Made app and widget task checkboxes interactive and synced with Memos.
- Fixed memo cards becoming too tall when rendering plain lists.
- Fixed long memo editing so shortcut buttons and save stay visible above the keyboard.
- Added an indent shortcut button that inserts two spaces.
- Fixed the widget memo preview so it uses the available height instead of stopping after seven lines.
- Added parser tests for indented tasks and lists.

### 1.0.0

- Added the full app experience for browsing, publishing, editing, deleting, pinning, archiving, and viewing archived memos.
- Added widget support for latest or pinned memo display.

## Future Ideas

- Support multiple widget configurations.
- Add tag or filter selection.
- Support richer Markdown rendering and attachment previews.
