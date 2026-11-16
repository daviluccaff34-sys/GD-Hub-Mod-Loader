# DL Loader migration

This branch continues the transition from GD Hub Mod Loader to **DL Loader** while preserving the existing package reader, DashCode interpreter, Android host, JNI runtime and build targets.

## Completed in this migration stage

- Android launcher label changed to **DL Loader**.
- Launcher vector artwork updated to a compact white DL monogram on the established dark-purple/purple identity.
- App version advanced to `0.3.0-alpha`.
- README and native CMake project identity updated.
- Added the `DLCocosBridge` diagnostic boundary around the existing Cocos runtime symbol probe.
- Replaced the old compact GD Hub panel with a dedicated, full-screen DL Loader interface above the Geometry Dash surface.
- Added installed-mod listing, runtime status, refresh, return-to-game controls, and Android Back handling for the DL screen.
- Replaced the old draggable glyph with a static DL entry control. This control is temporary and is not yet attached beside Newgrounds in the native Cocos main menu.

The Android namespace and application ID remain `com.gdhub.loader` intentionally. Changing them during a rebrand can create a second Android app identity and break existing installation/data continuity. A package-ID migration should be a separate explicit decision.

## Runtime integration status

The existing probe checks whether the Geometry Dash Cocos shared library is already loaded and whether selected Cocos functions plus the 2.2081 `MenuLayer::init`, `PlayLayer::init`, and `LevelEditorLayer::init` symbols can be resolved. `DLCocosBridge::inspect()` exposes that result as one of:

- `LibraryUnavailable`
- `SymbolsIncomplete`
- `SymbolsDetected`

**SymbolsDetected is not proof that calling those functions is safe.** The three Geometry Dash layer entry points are resolved for compatibility diagnostics only; this stage deliberately does not call them, install hooks, call Cocos functions, create nodes, change levels, or claim that game-object integration is complete.

Before adding calls, the adapter needs verified target-build signatures and ABI details, object ownership/refcount rules, render-thread dispatch, and physical-device tests for Geometry Dash 2.2081 on ARM32 and ARM64.

## Remaining in-game UX work

1. Attach the DL entry button beside Newgrounds in the Geometry Dash main-menu Cocos scene. The current Android control is only a temporary way to open and test the new screen.
2. Verify the exact menu hook and layout on Geometry Dash 2.2081, ARM32 and ARM64.
3. Keep the dedicated DL Loader screen separate from the game's original main menu.
4. Package and test all five DL sound assets on a physical Android device.
5. Expose supported Cocos/game-object operations to mods through a versioned API.

The old draggable ball and compact overlay panel have been removed. The temporary DL entry control remains until the native Cocos menu button can replace it without risking package execution or the game host lifecycle.

## Build checks

Run native tests with:

    cmake -S . -B build -DBUILD_TESTING=ON
    cmake --build build
    ctest --test-dir build --output-on-failure

Android builds continue to target `armeabi-v7a` and `arm64-v8a`.
