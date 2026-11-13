# GD Hub game UI bridge

This module defines the boundary between GD Hub's game-independent UI controller and a verified Geometry Dash 2.2081 adapter.

## Intended behavior

- The adapter creates a native in-game floating button at the lower-right edge.
- Tapping it opens the mod panel.
- The panel receives the catalog's current package list.
- Toggle requests are sent to the catalog; the panel is refreshed from catalog state.
- The close callback hides the panel.

## Visual design contract

The intended in-game skin follows Geometry Dash's compact arcade-menu language rather than Android Material UI:
- deep navy/near-black panel surface with a bright cyan/blue outline and small lime/yellow state accents;
- rounded, beveled-looking controls with bold, high-contrast labels;
- one small circular GD Hub launcher at the lower-right only;
- a single centered panel, with its close control inside the panel header;
- no full-screen dimmer, side rail, top bar, or extra corner widgets;
- panel and launcher must stay within the game visible-area safe insets and scale down on narrow viewports.

`game_ui_layout.hpp/.cpp` calculates the compact launcher and centered panel rectangles from the visible game size and safe-area insets. Coordinates use a bottom-left origin in game points. This is geometry and a renderer-independent draw plan and touch hit-testing only; the Cocos adapter must translate these instructions into actual nodes.

## Implemented here

- `GameUiHost`: interface for a game-specific UI backend.
- `ModCatalog`: interface for listing and activating installed packages.
- `GameUiBridge`: UI state controller with game-thread checks.
- `calculate_game_ui_layout`: tested pure layout calculation for the lower-right launcher and centered safe-area panel.
- `build_game_ui_render_plan`: tested renderer-independent draw instructions and hit targets for the launcher, panel header, close control, mod rows and enabled/disabled toggles.

## Not implemented yet

This is a compiled integration boundary, not a live Geometry Dash plugin. There is still no concrete Cocos2d-x backend, scene attachment, game entry point, live package catalog connected to the companion app, or UI rendered in Geometry Dash. The touch hit-test is a tested platform-independent component, not a native Cocos touch listener. The callbacks must be owned by a long-lived bridge instance; the eventual adapter must detach UI nodes and callbacks before destroying it.

## Adapter requirements

The concrete 2.2081 adapter must:
1. Verify the exact game library and ABI before using Cocos types.
2. Create and destroy all Cocos nodes on the game/render thread.
3. Position the button using the current visible screen size, with safe margins.
4. Keep touch input limited to the button and panel controls.
5. Implement package activation through the GD Hub runtime and report the actual result.
6. Be tested independently for ARM32 and ARM64.

Do not connect this interface to guessed addresses, guessed signatures, or unverified Cocos class layouts.
