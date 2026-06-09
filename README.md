# Memos Glance

[中文说明](README.zh-CN.md)

Memos Glance is an Android app and home-screen widget for quickly viewing and updating unarchived memos from a Memos server.

Current version: `1.0.0`

The app is written in Kotlin and uses XML/ViewBinding, Compose Markdown, OkHttp, DataStore, and Android AppWidget APIs.

## Features

- Configure Memos server URL and Access Token in the app.
- Fetch memo data through the Memos REST API.
- Browse recent unarchived memos in the app.
- Publish new memos from the app.
- Edit, delete, pin, and archive memos from the app.
- View archived memo history from the left drawer.
- Toggle Markdown task checkboxes from the app and sync them back to Memos.
- Insert Markdown snippets from the editor, including tasks, bold text, headings, lists, quotes, and code.
- Show one unarchived memo in the widget, with a user setting for either the latest memo or a pinned memo.
- Hide archived memo entries.
- Add and refresh actions in the widget header.
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

Task checkboxes are display-only inside the widget. Tapping widget content opens the app, and tapping the plus button opens the new memo editor.

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
versionName 1.0.0
versionCode 1
```

Debug build:

```powershell
cd C:\Users\XFCY2\Desktop\memos-glance
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

## Future Ideas

- Support multiple widget configurations.
- Add tag or filter selection.
- Support richer Markdown rendering and attachment previews.
