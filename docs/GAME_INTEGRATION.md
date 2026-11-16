# Geometry Dash in-game integration (2.2081)

## Goal

DL Loader must be integrated into Geometry Dash itself:
- a standalone DL entry button beside the Newgrounds button in the game's main menu;
- tapping it opens a dedicated DL Loader screen, separate from Geometry Dash's own menu;
- the DL screen lists installed mods and their runtime state;
- mods can create and manipulate real Cocos2d-x/game objects, not only draw companion Android UI or emit DashCode UI instructions.

This is not an Android system overlay. The companion app remains the package manager, while the game-side component owns the in-game UI and runtime session.

## Target profile

- Geometry Dash Android **2.2081** (the uploaded target APK has manifest versionName `2.2.144`, versionCode `41`).
- ARM32: `armeabi-v7a`
- ARM64: `arm64-v8a`
- Game rendering engine: modified Cocos2d-x 2.2.3 codebase.

The manifest versionName is recorded as package metadata; it does not change the project's 2.2081 compatibility target. The exact uploaded APK has been inspected for both ABIs. Keep all version-specific details in a dedicated adapter.

## Verified native entry points

The uploaded target APK exports the following game methods in both ABI libraries:
- `MenuLayer::init()`
- `PlayLayer::init(GJGameLevel*, bool, bool)`
- `LevelEditorLayer::init(GJGameLevel*, bool)`

It also exports the Cocos functions used for scene/node/sprite/label operations. See [the APK audit](UPLOADED_GEOMETRY_DASH_APK_AUDIT.md) for mangled names, ELF values, and ABI details.

Symbol presence is not proof that a hook is safe. ELF values are not absolute runtime addresses; ARM32 and ARM64 must be resolved and handled separately.

## Integration stages

### Stage A — establish a verified game boundary

The exact uploaded 2.2081 target APK has been inspected. The next task is to identify and verify a stable game-process load point, the correct method signatures and class layouts, and a safe render-thread dispatch mechanism. Resolve functions from the game's already-loaded `libcocos2dcpp.so`; never load a second Cocos runtime. Keep version-specific details in a dedicated adapter.

### Stage B — native in-game UI

Implement the standalone DL entry button beside Newgrounds and the dedicated DL Loader screen with the game's own Cocos2d-x objects. Create and destroy UI on the game/render thread, respect screen size, and avoid blocking gameplay. The panel must not pretend a package is active until the runtime confirms it loaded.

### Stage C — package bridge

Connect the game-side adapter to the existing GDHB reader and DashCode runtime. The game process must read packages from a deliberately shared/imported game-side location; the companion app's private files directory is not automatically visible to the game.

### Stage D — real-device verification

Test ARM32 and ARM64 independently on Geometry Dash 2.2081. Verify startup, scene changes, opening/closing the DL screen, package enable/disable, and recovery when a package fails. Keep a safe disabled-by-default mode until these checks pass.

## Current implementation status

Implemented foundations include safe-area layout calculation, a renderer-independent UI render plan, touch hit-testing, and a Cocos symbol probe. The current Cocos bridge is diagnostic only: it detects the already-loaded library and selected exported symbols. It does not call Cocos functions, create native nodes, attach a button to the main menu, install hooks, or load mods into the game process.

The Android-hosted DL screen and its temporary entry control are not yet the final native Cocos integration. The native menu button beside Newgrounds and real game-object access remain unimplemented.

Do not use guessed memory offsets, guessed function signatures, or another loader's private implementation as if they were verified DL Loader code. A Cocos UI source file alone is not proof of integration: it must be attached through a tested game adapter built against the exact 2.2081 APK.
