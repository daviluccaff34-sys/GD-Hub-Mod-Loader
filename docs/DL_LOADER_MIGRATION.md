# DL Loader migration

This branch begins the transition from GD Hub Mod Loader to **DL Loader** while preserving the existing package reader, DashCode interpreter, Android host, JNI runtime and build targets.

## Completed in this migration stage

- Android launcher label changed to **DL Loader**.
- Launcher vector artwork updated to a compact white DL monogram on the established dark-purple/purple identity.
- App version advanced to `0.3.0-alpha`.
- README and native CMake project identity updated.
- Added the `DLCocosBridge` diagnostic boundary around the existing Cocos runtime symbol probe.

The Android namespace and application ID remain `com.gdhub.loader` intentionally. Changing them during a rebrand can create a second Android app identity and break existing installation/data continuity. A package-ID migration should be a separate explicit decision.

## Runtime integration status

The existing Cocos probe checks whether the Geometry Dash Cocos shared library is already loaded and whether selected mangled symbols can be resolved. `DLCocosBridge::inspect()` exposes that result as one of:

- `LibraryUnavailable`
- `SymbolsIncomplete`
- `SymbolsDetected`

**SymbolsDetected is not proof that calling those functions is safe.** This stage deliberately does not call Cocos functions, create nodes, change levels, or claim that game-object integration is complete.

Before adding calls, the adapter needs verified target-build signatures and ABI details, object ownership/refcount rules, render-thread dispatch, and physical-device tests for Geometry Dash 2.2081 on ARM32 and ARM64.

## Planned in-game UX

1. Remove the old draggable floating ball and its panel from the Geometry Dash host.
2. Add a DL entry button beside Newgrounds in the Geometry Dash main menu.
3. Open a dedicated DL Loader screen, separate from the game's original main menu.
4. Add DL Loader sound effects.
5. Expose supported Cocos/game-object operations to mods through a versioned API.

The old overlay is not deleted in this first commit because its host and touch lifecycle must be traced before removing it without breaking the user's currently working package execution. It is scheduled for replacement, not redesign.

## Build checks

Run native tests with:

    cmake -S . -B build -DBUILD_TESTING=ON
    cmake --build build
    ctest --test-dir build --output-on-failure

Android builds continue to target `armeabi-v7a` and `arm64-v8a`.
