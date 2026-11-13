# Android in-process game launcher — implementation plan

## Correct target identification

The Geode Android Launcher maps Android `versionCode 41` to the unified Geometry Dash version `2.2081`. The installed APK's `versionName` may be `2.2.144`; this is not evidence that it is the wrong target. GD Hub's package probe now records both the manifest version and the mapped Geometry Dash version.

## Correct execution model

GD Hub must not start the separate Geometry Dash activity with an Intent, and it must not try to draw the game inside a WebView or virtual machine.

The Geode Android launcher uses its own game Activity inside the launcher APK. That Activity adds the installed game's APK assets to the host AssetManager, resolves the correct ABI libraries, initializes FMOD, loads FMOD before Cocos2d-x, initializes the Cocos helper and OpenGL surface, applies its version-specific compatibility/data-path fixes, and only then loads the mod framework. GD Hub will implement the same execution sequence with its own Activity and runtime bridge. The original Geometry Dash package remains installed; the game engine is hosted by GD Hub's Activity.

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

## GD Hub porting strategy

Use the Geode launcher as the technical reference, not as a separate app to launch. Implement GD Hub-owned classes for the host Activity, Cocos JNI bridge, FMOD bridge, APK/split asset resolver, native library resolver, lifecycle, and save-path handling. Preserve the native JNI class names/signatures expected by Geometry Dash where interoperability requires them. Load `libgdhub_loader.so`'s game-attachment entry point only after the Cocos game libraries are initialized. Keep the existing `.gdhb` package interpreter and manager UI in GD Hub. Do not create placeholder Cocos classes or claim runtime support before a device test.
