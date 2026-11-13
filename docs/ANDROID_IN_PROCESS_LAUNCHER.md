# Android in-process game launcher — implementation plan

## Correct execution model

GD Hub must not start the separate Geometry Dash activity with an Intent, and it must not try to draw the game inside a WebView or virtual machine.

The Geode Android launcher uses its own game Activity inside the launcher APK. That Activity creates the Cocos2d-x Android rendering surface, initializes the Cocos helper, loads the installed game's native libraries into the launcher's process, applies version-specific compatibility fixes, and then loads the mod-loader library. The original Geometry Dash package remains installed; the game engine is hosted by the launcher Activity.

Reference implementation studied:
- https://github.com/geode-sdk/android-launcher
- app/src/main/java/com/geode/launcher/GeometryDashActivity.kt
- app/src/main/java/org/cocos2dx/lib/
- app/src/main/cpp/launcher-fix.cpp

## GD Hub status

The current GD Hub companion app is Java-only and its native CMake target builds libgdhub_loader.so. It does not yet include the Cocos2d-x Java bridge classes, FMOD Java bridge, game Activity, asset redirection, or verified native startup sequence. Therefore it cannot honestly claim to host Geometry Dash yet.

## Implemented first step

GameInstallProbe performs read-only package inspection for com.robtopx.geometryjump. It reports:
- installed version name and version code;
- installed base APK path;
- native library directory;
- split APK paths, if present;
- device-supported ABIs.

It does not copy or alter the installed game.

## Remaining integration milestones

1. Add a dedicated GeometryDashActivity declared in the GD Hub manifest.
2. Integrate a compatible Cocos2d-x Android Java bridge and FMOD Java bridge, with their licenses and exact upstream revisions recorded.
3. Initialize Cocos and FMOD from the dedicated Activity.
4. Resolve base/split APK assets and native library locations for the installed package.
5. Load the game libraries in the correct dependency order and verify both ARM32 and ARM64.
6. Implement and test the version-specific native compatibility layer.
7. Load libgdhub_loader.so only after the game engine is initialized, then verify a harmless in-game UI test.
8. Add lifecycle handling for pause/resume, audio focus, back button, rotation and process termination.

Do not replace step 2 with empty placeholder Cocos classes: the game's native library calls into the Java bridge through JNI, so fake stubs would only produce runtime crashes.
