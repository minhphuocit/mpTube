# mpTube

A lightweight Android-only YouTube client built with Jetpack Compose.

## Build setup

1. Open the project in Android Studio.
2. Let Gradle sync/download dependencies.
3. Create `local.properties` in the project root (Android Studio may create it automatically).
4. Add your own YouTube Data API v3 key:

`YOUTUBE_API_KEY=YOUR_YOUTUBE_API_KEY`

5. Run the `app` configuration on an Android device/emulator.

## Notes

- `local.properties` is intentionally not included because it contains machine-specific SDK paths and secrets.
- The app uses `android-youtube-player` for playback.
- Search/cache disk work is moved off the main thread to keep Compose UI responsive.
- The YouTube player is registered with the Activity lifecycle and released when removed.
- In-memory cache is bounded to avoid unbounded growth.
