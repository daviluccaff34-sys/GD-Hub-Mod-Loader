# Using the GD 2.2081 Decompiled Reconstruction

## Decision

Use [iArtie/GD-2.2081-Decompiled](https://github.com/iArtie/GD-2.2081-Decompiled) as a **reference for the exact 2.2081 game/Cocos API**, not as a ready-to-build SDK and not as a drop-in replacement for the game's native libraries.

The upstream README states that the project has never been compiled, that it contains 1,216 reconstructed C++ source/header files, and that some large classes may be incomplete or incorrectly reconstructed. It also says the project expects Geometry Dash's modified Cocos2d-x 2.2.3.

## Initial source audit

- The repository contains reconstructed game classes and Cocos extension classes such as `CCMenuItemSpriteExtra`.
- It includes `src/Platform/cocos2d.h` and `src/Platform/cocos2d.cpp`, but these are reconstructed platform stubs, not a verified standalone Cocos SDK.
- The inspected `cocos2d.h` has unusual generated declarations and self-referential includes. It must not be assumed to compile as-is.
- The upstream README explicitly says compilation has never been attempted and warns about missing or inaccurate methods/types.

## Integration strategy

1. Keep the upstream repository as an external reference; do not copy its entire source tree into GD Hub.
2. Inspect only the minimum Cocos types and methods needed for the GD Hub in-game button and panel.
3. Compare declarations and class layouts against the user's supplied Geometry Dash 2.2081 APK for each ABI before using them.
4. Build a small GD Hub compatibility layer around verified APIs rather than compiling the entire decompilation.
5. Add the game-side adapter only after a verified load point, scene lifecycle, game-thread dispatch, and object ownership rules are established.
6. Keep ARMv7 and ARM64 verification separate. Never reuse offsets, layouts, or calling conventions across ABIs without evidence.

## Current status

- Source repository and README inspected.
- The source is suitable as a research/reference starting point.
- No upstream code has been copied into GD Hub.
- No Cocos adapter has been implemented or compiled yet.
- The Geometry Dash APK still needs binary inspection to confirm the actual native libraries and ABI-specific details.
- The original Geometry Dash installation must remain untouched; any clone must use a distinct application ID and separate save storage.

## Important

A class declaration in the decompiled source is not proof that it matches the runtime binary. Do not call reconstructed functions or instantiate reconstructed objects in the game until their ABI, layout, signatures, and lifecycle have been checked against the target APK.
