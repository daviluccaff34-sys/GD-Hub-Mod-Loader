# GD Hub game UI bridge

This module defines the boundary between GD Hub's game-independent UI controller and a verified Geometry Dash 2.2081 adapter.

## Intended behavior

- The adapter creates a native in-game floating button at the lower-right edge.
- Tapping it opens the mod panel.
- The panel receives the catalog's current package list.
- Toggle requests are sent to the catalog; the panel is refreshed from catalog state.
- The close callback hides the panel.

## Implemented here

- `GameUiHost`: interface for a game-specific UI backend.
- `ModCatalog`: interface for listing and activating installed packages.
- `GameUiBridge`: UI state controller with game-thread checks.

## Not implemented yet

This is a compiled integration boundary, not a live Geometry Dash plugin. There is no concrete Cocos2d-x backend, no scene attachment, no game entry point, and no package catalog connected to the companion app yet. The callbacks must be owned by a long-lived bridge instance; the eventual adapter must detach UI nodes and callbacks before destroying it.

## Adapter requirements

The concrete 2.2081 adapter must:
1. Verify the exact game library and ABI before using Cocos types.
2. Create and destroy all Cocos nodes on the game/render thread.
3. Position the button using the current visible screen size, with safe margins.
4. Keep touch input limited to the button and panel controls.
5. Implement package activation through the GD Hub runtime and report the actual result.
6. Be tested independently for ARM32 and ARM64.

Do not connect this interface to guessed addresses, guessed signatures, or unverified Cocos class layouts.
