# Geometry Dash in-game integration (2.2081)

## Goal

The GD Hub control must be rendered by Geometry Dash itself:
- a small floating GD Hub button in the lower-right corner;
- tapping it opens an in-game panel;
- the panel lists installed `.gdhb` packages;
- each package has an enabled/disabled state;
- the X button closes the panel.

This is not an Android system overlay. The companion app remains the package manager, while the game-side component owns the in-game UI and runtime session.

## Target profile

- Geometry Dash Android 2.2081
- ARM32: `armeabi-v7a`
- ARM64: `arm64-v8a`
- Game rendering engine: the 2.2081 reconstruction identifies a modified Cocos2d-x 2.2.3 codebase.

The two ABIs must be treated as separate targets. Android ARM64 class layouts, calling conventions, and native addresses cannot be copied to ARM32.

## Integration stages

### Stage A — establish a verified game boundary

Before calling game internals, identify and verify the exact 2.2081 Android library, ABI, Cocos types, scene lifecycle, and a stable point where the GD Hub component can be initialized. The user-provided APK audit has confirmed `libcocos2dcpp.so` for both ABIs and verified a small common set of exported Cocos symbols. GD Hub now has a runtime symbol probe that checks the already-loaded library without calling those functions. The game-process load point and scene attachment lifecycle remain unresolved. Keep version-specific details in a dedicated adapter; do not scatter offsets or assumptions through the runtime.

### Stage B — native in-game UI

Implement the floating button and panel with the game's own Cocos2d-x objects. The UI must be created and destroyed on the game/render thread, respect the current screen size, and avoid blocking gameplay. The panel must not pretend a package is active until the runtime confirms it loaded.

### Stage C — package bridge

Connect the game-side adapter to the existing GDHB reader and DashCode runtime. The game process must read packages from a deliberately shared/imported game-side location; the companion app's private files directory is not automatically visible to the game.

### Stage D — real-device verification

Test ARM32 and ARM64 independently on Geometry Dash 2.2081. Verify startup, scene changes, opening/closing the panel, package enable/disable, and recovery when a package fails. Keep a safe disabled-by-default mode until these checks pass.

## Current implementation status

This document records the design and verified target profile. The current APK is still a companion manager. Implemented foundations now include safe-area layout calculation, a renderer-independent UI render plan, touch hit-testing, and a Cocos symbol probe. These are not a native Cocos backend: no in-game button, scene attachment, game-process loader integration, game-side package path, or live game hook has been implemented yet.

Do not use guessed memory offsets, guessed function signatures, or another loader's private implementation as if they were verified GD Hub code. A Cocos UI source file alone is not proof of integration: it must be attached through a tested 2.2081 game adapter.
