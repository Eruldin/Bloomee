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
- Nav rail: "Bugün" (today dashboard) / "Profil ve veri". NOTE: rail item centers sit ~x=55-60, not at the rail's left edge — clicks near x<45 can miss; zoom the region if a nav click doesn't land.
- Bugün: ThemePicker row (palette chips Gül/Lavanta/Okyanus/Orman/Gün batımı + dark-mode icon), Döngü card (flow chips Yok/Lekelenme/Hafif/Orta/Yoğun), Su card (+100/+200/+330/+500 ml, Sıfırla), Kalori card (entry rows with trash icon, "Ne yedin?" + "kcal" fields, meal chips Kahvaltı/Öğle/Akşam/Atıştırma, "Ekle").
- Profil ve veri: fields Adın/Doğum yılı/Kilo (kg)/Boy (cm), activity chips, "Kaydet" button, backup import/export buttons, status message line.
- Click precision: TextButtons (e.g. "Sıfırla") get expanded ~48dp touch targets that can overlap the neighboring OutlinedButton's edge — aim for the center of small buttons, and verify the result in the store file if a click seems to hit the wrong control.

## Backup import merge semantics (P11+)
- `importBackup` is per-record last-write-wins: incoming `updatedAt` > local stamp (−1 if absent) applies; otherwise "atlandı". Status format: `N kayıt eklendi/güncellendi[, M atlandı (yereli daha yeni)][, D silme işlendi].`
- `deletedAt > 0` entries load as tombstones (key in `deletedKeys`, stamp = max(updatedAt, deletedAt)); a winning tombstone deletes the local record, a losing one is silently ignored.
- Records without `updatedAt` (legacy exports) merge as stamp 0 → they only fill keys that don't exist locally, never overwrite stamped records.
- Test recipe: craft fixtures with round, clearly ordered stamps (e.g. 1111111111111 stale / 9999999999999 fresh), reuse real record keys (dates for logs/hydration, ids for nutrition — read the id from the store file first), then assert both the status text AND the file diff.

## Known quirks (verify before relying on visuals)
- Theme/dark-mode: since P11 the window repaints instantly (BloomeeDesktopApp reads `state.version`). If palette/dark clicks don't visibly repaint, that's a regression — the profile values still persist to the store file either way.
- Tombstone durability: since P12, tombstone stamps persist in the store file under a `"tombstones"` section (`{key, ts}` pairs), so deletes survive restarts — but ONLY for tombstones registered in `DesktopData.deletedKeys` (UI deletes, or ones already in the file at load). If a stale backup resurrects a deleted record after restart, check whether the delete path actually adds the key to `deletedKeys` (e.g. e87b8e9's `importBackup` tombstone loop didn't, so import-applied deletes still weren't durable). In P11 tombstones were in-memory only and any delete was resurrectible after restart. Verify per-path: delete → grep `"tombstones"` in the file → restart → re-import a stale fixture.
- Backup import/export use `java.awt.FileDialog` (native Windows dialog, modal). For SAVE the File name field is preset to "bloomee-yedek.json" — clear it (ctrl+a, Delete) before typing a full path, or the preset text gets appended and Windows rejects it ("file name is not valid"). Typing a full absolute path in the File name field works. The LOAD dialog usually opens in the last-used directory.
- Apparent "didn't apply" states may just be deferred recomposition — check the store file for ground truth.

## Devin Secrets Needed
None — desktop app is fully local.
