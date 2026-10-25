# Android app

The app plans a journey with the backend and then follows it by GPS, so it can wake the passenger one stop before each change and before the destination.

## Files

- `MainActivity.kt` asks for local network access on Android 17 and shows the journey screen.
- `JourneyScreen.kt` has the From and To fields, the planned journey and the alarm card.
- `JourneyApi.kt` calls the backend's `/journey`, `/progress` and `/nearest` endpoints and reads the JSON.
- `AlarmCard.kt` starts and stops the alarm and shows how far there is to go.
- `AlarmService.kt` runs the alarm as a foreground service, so it keeps going when the screen locks. It checks the nearest station every 30 seconds, updates a notification, and vibrates once for each stop where the passenger needs to get off.

## Running on the emulator

Start the backend so the emulator can reach it (from `backend/`):

```
uvicorn api:app --host 0.0.0.0 --port 8000
```

Then open this folder in Android Studio and run the app, or from the command line:

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The emulator reaches the Mac at `10.0.2.2`, which is the backend address the app uses. To test the alarm without travelling, set a fake location, for example Oxford Circus:

```
adb emu geo fix -0.1419 51.5152
```

## Tests

```
./gradlew testDebugUnitTest
```

These check that the app reads the backend's JSON correctly.
