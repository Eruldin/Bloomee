---
name: testing-desktop-app
description: How to launch and end-to-end test the Bloomee Compose Desktop app (:desktop:run) on the Windows box — store file location, AWT FileDialog quirks, window-focus tricks, and per-record updatedAt verification.
---

# Testing the Bloomee desktop app

## Launch
- `./gradlew.bat :desktop:run` (Git Bash) opens the Compose Desktop window "Bloomee" (1200x780). Run it in a background exec shell (`shell_id`) — the task blocks while the window is open and exits cleanly on close.
- The window may open behind Chrome or lose focus to other windows. To bring it forward: `powershell -c "(New-Object -ComObject WScript.Shell).AppActivate('Bloomee')"` or user32 `SetForegroundWindow` on the java process MainWindowHandle. Alt+Tab cycling also works.
- Maximize by double-clicking the title bar.

## Data store
- Store file: `%USERPROFILE%\.bloomee\bloomee-store.json` (`C:\Users\Administrator\.bloomee\bloomee-store.json`). Same schema as the phone `bloomee-yedek.json` backup; a desktop-only `profile` section holds theme/name/weight/etc.
- Every record carries `updatedAt` (epoch ms). The per-record stamp map lives in `DesktopData.updatedAt`; save() writes stamps verbatim for unchanged records. To verify: read the JSON before and after a mutation — only the mutated record's stamp should change. Snapshot the file between steps (`cp store bloomee-test/store-stepN.json`) for evidence.
- No python/jq on the box — use `read`/`grep`/`cat` on the JSON directly; it's small.

## UI map (Turkish labels)
- Nav rail: "Bugün" (today dashboard) / "Profil ve veri".
- Bugün: ThemePicker row (palette chips Gül/Lavanta/Okyanus/Orman/Gün batımı + dark-mode icon), Döngü card (flow chips Yok/Lekelenme/Hafif/Orta/Yoğun), Su card (+100/+200/+330/+500 ml, Sıfırla), Kalori card (entry rows with trash icon, "Ne yedin?" + "kcal" fields, meal chips Kahvaltı/Öğle/Akşam/Atıştırma, "Ekle").
- Profil ve veri: fields Adın/Doğum yılı/Kilo (kg)/Boy (cm), activity chips, "Kaydet" button, backup import/export buttons, status message line.

## Known quirks (verify before relying on visuals)
- Theme/dark-mode changes write to the store file but do NOT re-render live — `DesktopAppState.data.profile` is a plain Kotlin var, not Compose state, so `BloomeeDesktopTheme` params freeze at first composition. Persisted theme applies on the next launch; verify via the JSON file, not pixels.
- Backup import/export use `java.awt.FileDialog` (native Windows dialog, modal). For SAVE the File name field is preset to "bloomee-yedek.json" — clear it (ctrl+a, Delete) before typing a full path, or the preset text gets appended and Windows rejects it ("file name is not valid"). Typing a full absolute path in the File name field works. The LOAD dialog usually opens in the last-used directory.
- Apparent "didn't apply" states may just be deferred recomposition — check the store file for ground truth.

## Devin Secrets Needed
None — desktop app is fully local.
