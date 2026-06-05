# Memos Glance

Memos Glance is an Android home-screen widget for showing the latest unarchived memo from a Memos server.

The app is written in Kotlin and uses OkHttp, DataStore, and Android AppWidget APIs.

## Features

- Configure Memos server URL and Access Token in the app.
- Fetch memo data through the Memos REST API.
- Show only the latest unarchived memo.
- Hide archived memo entries.
- Display memo content in a scrollable widget area.
- Render basic Markdown:
  - headings
  - paragraphs
  - bullet lists
  - quotes
  - inline code
  - links as readable text
  - task checkboxes, such as `- [ ]` and `- [x]`
- Tap memo content in the widget to open the memo in a browser.
- Refresh button in the widget header.

## Current Interaction Model

Markdown task checkboxes are display-only inside the widget.

Some Android launchers do not reliably support click events inside scrollable widget list items. To keep the behavior stable, Memos Glance opens the memo page in a browser when the user taps widget content.

You can edit or check tasks in Memos, then tap the widget refresh button to update the widget.

## Setup

1. Open the project in Android Studio.
2. Wait for Gradle sync to finish.
3. Run the app.
4. Enter your Memos server URL and Access Token.
5. Tap "Save and refresh widget".
6. Add the "Memos Glance" widget to your Android home screen.

Example server URL:

```text
https://memos.example.com
```

The Access Token should be generated from your Memos account settings.

## Build APK

Debug build:

```powershell
cd C:\Users\XFCY2\Desktop\memos_widget
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

- Add memo creation.
- Add an in-app Markdown preview.
- Support multiple widget configurations.
- Add tag or filter selection.
- Re-enable in-widget task syncing for launchers with stable widget item click support.

