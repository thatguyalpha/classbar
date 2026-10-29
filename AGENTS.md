# Agent Guidelines

## Project Context
- **Stack**: Native Android application (Kotlin, Jetpack Compose, Gradle).
- **Build**: Run `.\gradlew.bat assembleDebug` to compile debug builds.
- **Output Artifact**: The `assembleDebug` task automatically copies the resulting APK to `ClassBar.apk` in the root directory. Omit redundant filesystem verification after a successful build.

## Local Configuration
- **Local Rules**: Read and apply `.agents/rules/apk-link.md` directly before responding.

## Testing & UI Verification
- **scrcpy**: Use `scrcpy` to test, interact with, and verify the application on connected Android devices.
