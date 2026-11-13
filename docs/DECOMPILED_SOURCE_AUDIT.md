# Decompiled source audit: Geometry Dash 2.2081

Repository inspected: [iArtie/GD-2.2081-Decompiled](https://github.com/iArtie/GD-2.2081-Decompiled)

## Confirmed by the upstream README

- The repository describes itself as a C++ reconstruction of Geometry Dash 2.2081.
- It contains 1,216 reconstructed C++ source/header files under `src/`.
- The author says it has never been compiled.
- The author warns that some methods are missing and large classes may be incomplete or incorrectly reconstructed.
- The expected integration environment is Geometry Dash's modified Cocos2d-x 2.2.3.

## Direct source inspection

- `src/Cocos/CCMenuItemSpriteExtra.h/.cpp` reconstructs a Geometry Dash-specific menu item with activation, selection, scale/move animation and optional sound behavior.
- `src/Platform/cocos2d.h` is not a conventional Cocos2d-x public header. It begins with a self-include of `Platform/cocos2d.h`, includes `CCSize.h`, and declares a class named `cocos2d` containing many generated declarations.
- A search of the repository tree found no separate core headers named `CCNode.h`, `CCDirector.h`, `CCScene.h`, `CCLayer.h`, `CCSprite.h`, or `CCMenu.h`.
- `src/Platform/cocos2d.cpp` contains reconstructed function bodies with decompiler residue and unresolved-looking symbols such as `sub_*` and global data labels.

## Engineering conclusion

Do not add this source tree to GD Hub's build as if it were a clean Cocos SDK. Use it as a behavior/API reference for the target game, while keeping GD Hub's renderer-independent UI controller and render plan isolated from the game binary.

Before a real adapter is connected, verify the exact 2.2081 APK's native libraries, exported symbols, ABI, object lifecycle, and the method signatures/layouts required by the adapter. Do not infer runtime offsets or call reconstructed functions solely from these files.
