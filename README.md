# Subscriber Tracker (Android)

Subscriber-facing app: subscribers log in and see their monthly payment due date, with automatic
reminder notifications (3 days before, 1 day before, and on the due date).

## ⚠️ Required setup before this will actually work

The app needs a **real Firebase project**. The included `app/google-services.json` is a
placeholder just so the build doesn't crash — replace it before you actually use the app.

### 1. Create a Firebase project
1. Go to https://console.firebase.google.com → **Add project** → name it (e.g. "Rustech Subscriber Tracker").
2. Inside the project, click **Add app → Android**.
3. Package name: `com.rustech.subscribertracker` (must match exactly).
4. Download the generated **google-services.json**.
5. In your GitHub repo, replace `app/google-services.json` with the one you downloaded (upload via GitHub web → drag file → commit).

### 2. Enable Authentication
1. In Firebase Console → **Build → Authentication → Get started**.
2. Enable **Email/Password** sign-in method.

### 3. Create Firestore Database
1. Firebase Console → **Build → Firestore Database → Create database**.
2. Start in **production mode**.
3. Go to the **Rules** tab and paste the contents of `firestore.rules` (included in this repo), then Publish.

### 4. Add a subscriber record (until the PC dashboard exists)
In Firestore, manually create a document for testing:
- Collection: `subscribers`
- Document ID: the subscriber's email, **all lowercase** (e.g. `juan@gmail.com`)
- Fields:
  - `name` (string) — e.g. "Juan Dela Cruz"
  - `email` (string) — same as doc ID
  - `dueDate` (timestamp) — pick a date
  - `monthlyAmount` (number) — e.g. 500
  - `status` (string) — "active"

## Building the APK (GitHub Actions)

1. Push this whole folder to a new GitHub repo (main branch).
2. GitHub Actions will auto-build on push (see `.github/workflows/build.yml`).
3. Go to the repo's **Actions** tab → open the latest run → download the
   `subscriber-tracker-debug-apk` artifact → install on your phone.

## How it works

- `LoginActivity` — Firebase email/password login + self-registration.
- `DashboardActivity` — fetches `subscribers/{email}` from Firestore and displays it, then
  schedules local reminder notifications via WorkManager.
- `ReminderWorker` — fires the actual notification when its scheduled delay elapses.
- Reminders are rescheduled every time the dashboard loads, so as long as the subscriber opens
  the app at least once after you set/update their due date, reminders will be (re)scheduled correctly.

## Known limitation (fine for v1, worth fixing later)

Reminders scheduled via WorkManager's one-time delay **will be lost if the phone reboots**
before they fire, unless we add a `BOOT_COMPLETED` receiver that reschedules them from the
last-known Firestore data. Let Claude know if you want that added.
