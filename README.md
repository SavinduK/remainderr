# Remainderr

A native Android productivity and clinical utility application built with Kotlin, Jetpack Compose, Material 3, Room, and Glance App Widgets.

## Features

- **Tasks & Calendar**: Monthly 35-day task calendar with overdue (red), pending (blue), and completed (green) indicators, date-filtered task list, start/end time scheduling, and edit/delete mode.
- **Home Screen Widget**: Jetpack Glance Android app widget displaying today's date and upcoming/overdue incomplete tasks with one-tap launch into the app.
- **Diary / Journal**: Multi-photo diary entries with dynamic collage previews, detail view with horizontal image pager, inline editing, and SAF backup/restore.
- **Expense Tracker & Cards**: Income/expense transaction ledger with date grouping, category badges, net balance summary (`LKR`), swipe-to-delete, and credit/debit card management (Visa/Mastercard auto-detection).
- **Notes & Categories**: Searchable notes with `All Notes`, `Favorites`, and `Categories` tabs, 2-column staggered masonry grid, formatting toolbar, undo/redo, and SAF backup/restore.
- **Settings**: Encrypted Gemini API key & model selection (`gemini-2.5-flash` / `gemini-3.5-flash`) using Android Keystore, full app SAF backup/restore (`AppData`, `Diary`, `History`), and navigation to Password Tracker and Clinical Tracker.
- **Password Tracker**: Local credential vault with platform badges, masked/revealed password toggle, clipboard copy, and entry management.
- **Clinical Tracker**: Saved JSON patient history viewer/editor, file sharing via `FileProvider`, handwritten history extraction via Gemini Vision using a customizable JSON template, and a 102-item clinical history checklist.
