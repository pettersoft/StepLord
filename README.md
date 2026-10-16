# StepLord

StepLord is a simple Android step counter. It shows how many steps you have taken today, how close you are to your daily goal, and how the past week went. It reminds you of your progress every evening and tells you when you reach your goal.

All data stays on the device. There is no account, no cloud sync and no tracking.

## Features

- **Today's steps** in a large circle on the start page, with a ring showing progress towards the daily goal. Tap the circle to change the goal.
- **Approximate distance and walking time** in the circle, e.g. "≈ 5.7 km · 1 hr, 16 min".
  - Distance = steps × stride, where stride ≈ 0.415 × height (from *Settings → Profile*, default 175 cm).
  - Time assumes about 100 steps per minute.
  - Units (metric or imperial) are chosen in Settings. Unit names and number formats come from Android's ICU formatter, so they follow the app language.
- **Week and month charts:** bar charts of daily steps with the period's total and daily average.
  - Switch between *Week* and *Month*.
  - Use the arrows to move between periods. ← is available when older steps exist, and → up to the current period.
  - Weeks start on the locale's first day of the week (Monday in Sweden and Germany, Sunday in the US).
- **Counts in the background** without a permanent notification, using Google Play services' Recording API (see [How step counting works](#how-step-counting-works)).
- **Daily summary notification** at a time you choose (default 21:00), shown as a banner at the top of the screen: *"7,559 steps today, 76% of your daily goal of 10,000"*.
- **Goal reached notification**, at most once per day.
- **Settings:** daily goal, summary on/off and time, goal notification on/off, and app language.
- **Light and dark mode** following the system setting, with a blue look in both.
- **Portrait only on phones.** Android 16+ ignores orientation locks on large screens (tablets, foldables), so the layout still keeps the circle at a sensible size there.
- **Backup:** step history and settings are included in Android's backup and device transfer, so they follow the user to a new phone.
- **Languages:** English, Swedish, German and Spanish. More can be added easily (see [Localisation](#localisation)).

## Requirements

| | |
|---|---|
| Android | 8.0 (API 26) or later. Targets API 37 |
| Hardware | A step counter sensor (most phones have one) |
| Background counting | Google Play services. Without it, steps are only counted while the app is open |
| Permissions | *Physical activity* (to count steps) and *Notifications* (Android 13+). Both are asked for in the app |

## How step counting works

Android's step counter sensor only counts while some app is listening to it. Reading it every now and then would miss the steps in between, so StepLord uses two sources of step data:

1. **Recording API on mobile (main source).** When the user grants *Physical activity*, the app subscribes to step recording in Google Play services ([`StepHistory`](app/src/main/java/se/pettersoft/steplord/data/history/StepHistory.kt)). Play services keeps the sensor registered in a battery-efficient way and stores up to 10 days of history on the device, even when StepLord isn't running.
2. **Sync into the app database.** Daily totals are copied from that history into the Room database:
   - every 15 minutes in the background, by a WorkManager job ([`StepSyncWorker`](app/src/main/java/se/pettersoft/steplord/sync/StepSyncWorker.kt)). WorkManager keeps it scheduled across reboots
   - every 30 seconds while the start page is visible
   - just before the daily summary is shown

   A sync never lowers a day's stored total.
3. **Fallback without Play services.** The app reads the step sensor directly while it is open ([`StepSensor`](app/src/main/java/se/pettersoft/steplord/data/sensor/StepSensor.kt)). The sensor reports total steps since the phone booted, so the app stores the last reading and adds the difference. The stored reading also records which boot it came from. After a reboot, or after the database is restored onto another phone, the next reading starts a fresh baseline instead of being compared with a counter from a different boot. Steps taken overnight aren't credited to the next day.

This avoids a foreground service, so there is no permanent notification and no special Google Play declaration.

**Known limitations:**
- Steps taken before the user first granted access aren't counted.
- How quickly new steps appear depends on how often Play services writes its history.

## Notifications

Notification text follows the app language, and both notifications show as a banner at the top of the screen.

**Daily summary** ([`notifications/`](app/src/main/java/se/pettersoft/steplord/notifications))
- **Scheduling:** `AlarmManager.setAndAllowWhileIdle`, an *inexact* alarm that also fires when the phone is idle. It needs no exact-alarm permission. The trade-off is that it can arrive a few minutes late (Android allows up to an hour, but in practice it is a few minutes).
- **When it fires:** the app syncs the latest steps, posts the summary, and schedules the next day's alarm.
- **Staying scheduled:** Android clears alarms on reboot and app updates, and they should follow time changes. A receiver therefore sets the alarm again after `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET` and `TIMEZONE_CHANGED`. The alarm is also set again at app start, since force-stopping an app clears it.
- **Time zones:** the next alarm time is worked out in local time, so it stays correct across daylight-saving changes.

**Goal reached**
- **When it's checked:** after every update of today's total (background sync, sync while the app is open, sensor fallback), [`GoalReachedNotifier`](app/src/main/java/se/pettersoft/steplord/notifications/GoalReachedNotifier.kt) checks the goal.
- **Once per day:** the date of the last goal notification is stored in the settings table, inside a database transaction, so it is shown at most once per day.

**Channels:** there are two, *Daily summary* and *Goal reached*, so each can be turned off separately in the system settings.

## Localisation

Supported languages: **English** (default), **Swedish**, **German**, **Spanish**.

### How it works

- **Text:** all user-facing text lives in string resources: `app/src/main/res/values/strings.xml` (English) and `values-<language>/strings.xml` for each translation. Text that follows a number ("1 step" / "2 steps") uses `<plurals>`, because languages differ in plural forms. Spanish, for example, also needs a `many` form.
- **Dates and numbers** are formatted for the current language by the platform, not by fixed patterns:
  - the date header uses `DateFormat.getBestDateTimePattern`: "Thursday 15 October", "jueves, 15 de octubre", "Donnerstag, 15. Oktober"
  - numbers use `NumberFormat`: "3,574", "3 574", "3.574". This includes the chart axis
  - times follow the device's 12/24-hour setting
- **Per-app language (Android 13+):** `generateLocaleConfig` in [`app/build.gradle.kts`](app/build.gradle.kts) builds the app's language list from the `values-<language>` folders. StepLord then appears in *System settings → Apps → StepLord → Language*, and *Settings → Language* in the app opens that page. On Android 8–12 the app follows the device language.
- **Notification channel names** are refreshed when the app language changes.

### Adding a language

1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-<code>/strings.xml`, where `<code>` is the language code, e.g. `values-fr` for French or `values-pt-rBR` for Brazilian Portuguese.
2. Translate every string. Leave out `app_name`, which is marked `translatable="false"`. Give each `<plurals>` the quantities the language needs. Lint warns about missing ones.
3. Build. The language appears in the system's per-app language list automatically.

If any string is left untranslated, **lint fails the build** (`MissingTranslation`) and names the languages missing it.

**Testing layouts:** debug builds include two pseudo-languages, **en-XA** (longer, accented text) and **ar-XB** (right-to-left). Turn on *Developer options → Show pseudo-locales*, then pick one as the app language. They make text that doesn't fit, or that isn't translated, easy to spot.

You can switch the app language from the command line on an emulator or device:

```sh
adb shell cmd locale set-app-locales se.pettersoft.steplord --locales sv-SE
adb shell cmd locale set-app-locales se.pettersoft.steplord --locales ""   # back to system default
```

## App icon

The source artwork lives in [`branding/`](branding):

| File | Use |
|---|---|
| `ico-blue.png` | Main icon: crown and shoes on blue. Used for the Play Store icon and the legacy bitmaps |
| `ico-trans.png` | Same artwork on a transparent background. Used as the adaptive icon's foreground |

The launcher icon is an **adaptive icon** ([`mipmap-anydpi/ic_launcher.xml`](app/src/main/res/mipmap-anydpi/ic_launcher.xml)), so each launcher can apply its own shape (circle, squircle and so on). It has three layers:

- **Background:** the solid blue `ic_launcher_background` (`#2580FD`), from [`values/ic_launcher_background.xml`](app/src/main/res/values/ic_launcher_background.xml)
- **Foreground:** the transparent artwork, scaled to fit the launcher's safe area
- **Monochrome:** the artwork's silhouette, used for themed icons on Android 13+

**Regenerating the icon** after the artwork changes:

```sh
pip install pillow
python branding/generate_icons.py
```

The script:
- cleans up stray pixels left by the background removal
- writes every size to `app/src/main/res/mipmap-*`
- writes the 512×512 Play Store icon to `app/src/main/ic_launcher-playstore.png`
- saves a preview of the icon in common launcher shapes to `build/icon_preview.png`

## Tech stack

| Area | Library |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Database | Room (SQLite), schemas exported to [`app/schemas/`](app/schemas) |
| Dependency injection | Hilt |
| Background work | WorkManager, AlarmManager |
| Step data | Recording API on mobile (`play-services-fitness`), step counter sensor |
| Charts | Vico |
| Navigation | Navigation Compose (type-safe routes) |
| Testing | JUnit, kotlinx-coroutines-test, Turbine, Room in-memory database |

Versions are managed in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Project structure

```
app/src/main/java/se/pettersoft/steplord/
  data/
    local/        Room database: daily steps, sensor state, user settings, DAOs
    history/      Recording API (system step history)
    sensor/       Step counter sensor (fallback)
    repository/   StepsRepository, SettingsRepository
  sync/           StepSyncWorker (periodic background sync)
  notifications/  Channels, daily summary alarm, goal reached, reschedule receivers
  di/             Hilt modules
  ui/
    home/         Start page
    settings/     Settings
    components/   Step circle, bar chart
    navigation/   Navigation graph
    theme/        Colours (light/dark), typography
  util/           Permission helpers
```

## Building and testing

Open the project in Android Studio, or build from a terminal. Gradle needs a JDK: Android Studio's bundled one works.

```sh
# Windows (PowerShell): $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug                # build
./gradlew installDebug                 # install on a running emulator/device
./gradlew testDebugUnitTest            # unit tests
./gradlew connectedDebugAndroidTest    # database tests on an emulator/device
./gradlew lintDebug                    # lint, including missing translations
```

Note that `connectedDebugAndroidTest` uninstalls the app when it finishes.

**Debug builds** have an *Add sample data* button on the start page, which fills the last week with random step counts. It is useful on emulators, which usually have no step sensor.

**Backup:** [`data_extraction_rules.xml`](app/src/main/res/xml/data_extraction_rules.xml) (Android 12+) and [`backup_rules.xml`](app/src/main/res/xml/backup_rules.xml) (older versions) include only the app database. WorkManager keeps its own database in storage that isn't backed up, and alarms and background work are recreated at app start. To test a backup and restore on an emulator:

```sh
adb shell bmgr enable true
adb shell bmgr transport com.android.localtransport/.LocalTransport
adb shell bmgr backupnow se.pettersoft.steplord
adb uninstall se.pettersoft.steplord && ./gradlew installDebug   # data is restored on install
```

**Changing the database:** bump the version in [`AppDatabase`](app/src/main/java/se/pettersoft/steplord/data/local/AppDatabase.kt) and add a migration (an `AutoMigration` where possible). Commit the exported schema in `app/schemas/`.
