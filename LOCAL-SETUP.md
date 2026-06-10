# Running the Game Tracker locally — free forever

This backend now runs entirely on your own machine:

- **Database:** a single SQLite file at `data/gamelibrary.db` (was Railway MySQL).
- **Images/videos:** saved to the `uploads/` folder (was AWS S3).
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

## 2. Bring your existing games over from Railway (one time)

Do this **while your Railway database is still reachable** — once Railway deletes
the trial database, this data is gone unless you have a backup.

1. Get the DB connection details: Railway dashboard → your **MySQL** service →
   **Variables / Connect**. You need the public proxy host, port, the database
   name (usually `railway`), and the root password.
2. Run:

   ```powershell
   .\scripts\migrate-from-railway.ps1 `
     -MysqlUrl "jdbc:mysql://HOST:PORT/railway?sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC" `
     -Password "YOUR_DB_PASSWORD"
   ```

   (An example host/port from your old config was
   `metro.proxy.rlwy.net:46798` — verify the current value in Railway.)

This exports every game to `games-export.json`, then imports it into
`data/gamelibrary.db`. It's **read-only** against Railway and safe to re-run
(games already present are skipped).

**If Railway is already gone** but you ran the emergency `mysqldump` earlier:
start a throwaway MySQL locally, load the dump, then point the script at it:

```powershell
docker run -d --name gl-mysql -e MYSQL_ROOT_PASSWORD=pw -e MYSQL_DATABASE=railway -p 3307:3306 mysql:8
# wait ~20s for it to start, then load your dump:
docker exec -i gl-mysql sh -c 'exec mysql -uroot -ppw railway' < gamelibrary-backup.sql
.\scripts\migrate-from-railway.ps1 -MysqlUrl "jdbc:mysql://localhost:3307/railway?serverTimezone=UTC" -Password "pw"
docker rm -f gl-mysql
```

---

## 3. Run it day-to-day

```powershell
.\scripts\run-local.ps1
```

The backend starts on **http://localhost:8080**. Leave the window open; stop with
`Ctrl+C`. Check it's healthy at http://localhost:8080/health/db.

### Point the frontend at it
In your **frontend** (Next.js) repo, set the API base URL to
`http://localhost:8080` — usually an env var like `NEXT_PUBLIC_API_URL` in
`.env.local` — then run `npm run dev` and open http://localhost:3000.
(`localhost:3000` and `:5173` are already allowed by the backend's CORS config.)

You don't need Netlify anymore for personal use, but it can stay deployed; it
just won't reach the backend unless this app is running and exposed.

---

## 4. Back up your data (do this regularly)

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

## What about the old images on AWS S3?

Games you migrated still point at their S3 image URLs, so they keep working as
long as that bucket exists. AWS S3's free tier lasts ~12 months; after that a
few hundred MB costs pennies, but if you want **truly $0**, download those
images into `uploads/` and update the URLs. Ask and I can add a small one-off
task that does this automatically. **New** uploads already go to `uploads/`.

## Notes / tradeoffs

- **Local only:** the app runs when your laptop is on. If you later want phone
  access, a free **Cloudflare Tunnel** can expose `localhost:8080` to your
  Netlify frontend while the laptop is running — ask and I'll set it up.
- **Auth still needs internet** to verify Firebase tokens. Removing Firebase to
  go fully offline is possible but also touches the frontend repo.
- The `data/`, `uploads/`, and `backups/` folders are git-ignored — your code
  lives in git, your *data* lives in backups. Keep both safe.
