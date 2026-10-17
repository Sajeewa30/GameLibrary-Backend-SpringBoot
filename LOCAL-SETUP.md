# Running the Game Tracker locally — free forever

This backend now runs entirely on your own machine:

- **Database:** a single SQLite file at `data/gamelibrary.db`.
- **Images/videos:** saved to the `uploads/` folder.
- **Login/auth:** still Firebase Authentication (free; needs internet + a
  service-account file — see prerequisites).

No paid services, no trials, nothing that expires. Your whole library is two
folders (`data/` + `uploads/`) that you back up and carry between laptops.

---

## 1. Prerequisites (one time)

1. **JDK 21** installed (`java -version` should report 21).
2. **`firebase-service-account.json`** in the project root. This file is **not**
   in the repo (it's a secret). Get it from the
   [Firebase console](https://console.firebase.google.com/) → your project →
   ⚙ Project settings → **Service accounts** → **Generate new private key** →
   save the downloaded file as `firebase-service-account.json` here.
   - Alternatively set the env var `FIREBASE_CREDENTIALS_FILE` to its path, or
     `FIREBASE_CREDENTIALS_B64` to the base64 of the file.

> If you skip this, the app will fail to start with
> *"Firebase credentials path is not set"*.

---

## 2. Export / import your games as JSON (optional)

The app has a built-in, database-agnostic export/import (`DataMigrationRunner`).
It is handy for moving the library to another database later:

```powershell
.\gradlew.bat bootRun --args="--app.migrate.export=games-export.json"   # current DB -> JSON
.\gradlew.bat bootRun --args="--app.migrate.import=games-export.json"   # JSON -> current DB
```

Both commands do their job and exit. Import skips games that already exist.

---

## 3. Run it day-to-day

```powershell
.\scripts\run-local.ps1
```

The backend starts on **http://localhost:8080**. Leave the window open; stop with
`Ctrl+C`. Check it's healthy at http://localhost:8080/health/db.

### Point the frontend at it
In your **frontend** (Next.js) repo, set the API base URL to
`http://localhost:8080` — the `NEXT_PUBLIC_API_BASE_URL` env var in
`.env.local` — then run `npm run dev` and open http://localhost:3000.
(`localhost:3000` and `:5173` are already allowed by the backend's CORS config.)

You don't need Netlify anymore for personal use, but it can stay deployed; it
just won't reach the backend unless this app is running and exposed.

---

## 4. Back up your data (do this regularly)

### Google Drive (recommended): the "Back up to Google Drive" button

The **Stats** page has a **Back up to Google Drive** button. It uploads a safe
snapshot of the database plus every new or changed image/video to the
`GameLibrary` folder in your Google Drive. Drive is an **exact mirror**: only new
files are uploaded each time, and files you delete in the app are removed from
Drive too (they sit in Drive's trash for 30 days). The site itself never reads
from Drive.

One-time setup (already done on this PC):

```powershell
winget install Rclone.Rclone
rclone config create gdrive drive scope=drive.file   # sign in to Google in the browser
```

`scope=drive.file` means rclone can only see the files it created, nothing else
in your Drive. To restore from Drive (new laptop, lost data), stop the backend and run:

```powershell
.\scripts\restore-from-drive.ps1
```

### Local zip

```powershell
.\scripts\backup.ps1
# or copy the backup straight into a Google Drive / OneDrive synced folder:
.\scripts\backup.ps1 -Destination "G:\My Drive\GameLibraryBackups"
```

This zips `data/` + `uploads/` into `backups/gamelibrary-backup-<timestamp>.zip`.
That single zip is your complete, restorable library. Keep a copy off the laptop
(Drive, an external disk, a private GitHub repo — anywhere).

> Run backups when the app is idle (not mid-upload) so the database file is in a
> clean state.

---

## 5. Move to a new laptop / restore

1. Install JDK 21, clone this repo, add `firebase-service-account.json`.
2. Restore your data from a backup zip:

   ```powershell
   .\scripts\restore.ps1 -ZipPath .\backups\gamelibrary-backup-20260609-101500.zip
   ```

3. `.\scripts\run-local.ps1` — you're back, with all games and images.

---

## Notes / tradeoffs

- **Local only:** the app runs when your laptop is on. If you later want phone
  access, a free **Cloudflare Tunnel** can expose `localhost:8080` to your
  Netlify frontend while the laptop is running — ask and I'll set it up.
- **Auth still needs internet** to verify Firebase tokens. Removing Firebase to
  go fully offline is possible but also touches the frontend repo.
- The `data/`, `uploads/`, and `backups/` folders are git-ignored — your code
  lives in git, your *data* lives in backups. Keep both safe.
