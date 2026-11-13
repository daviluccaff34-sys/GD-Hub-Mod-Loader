# Geometry Dash 2.2081 APK: native-library audit

This audit was performed against the user-provided APK in the current conversation. The APK itself is not copied into this repository.

## Verified archive contents

- APK size: 179,868,202 bytes.
- ZIP entries: 6,177.
- DEX: `classes.dex`.
- Native Cocos library is present for both requested ABIs:
  - `lib/arm64-v8a/libcocos2dcpp.so` — 19,077,352 bytes.
  - `lib/armeabi-v7a/libcocos2dcpp.so` — 11,290,708 bytes.
- FMOD is also present for both ABIs.

## ELF identification

| APK library | ELF class | ELF machine |
|---|---:|---:|
| arm64-v8a/libcocos2dcpp.so | ELF64 | AArch64 (183) |
| armeabi-v7a/libcocos2dcpp.so | ELF32 | ARM (40) |

SHA-256:
- arm64 Cocos: `dda3752ab3a912fd2293561e157ecc98eca799056abd9223a331c1c514175df7`
- ARM32 Cocos: `3223da83c9232bb3d59d19c2c0becd5c7deb53c71580110089e6e45c80a4c8af`

## Cocos symbols/strings observed in both libraries

The native libraries contain strings/symbol names for:
- `JNI_OnLoad`
- `cocos2d::CCDirector::sharedDirector()`
- `cocos2d::CCSceneDelegate::willSwitchToScene(cocos2d::CCScene*)`
- `cocos2d::CCSprite::create(char const*)`
- `cocos2d::CCArray::create()`
- `cocos2d::CCObject::retain()`
- Cocos Android renderer JNI entry points.

These findings establish that the supplied APK packages a Cocos2d-based native game library for both ABIs. A string match alone does not prove that a symbol is exported or safe to call; dynamic symbol-table inspection and exact ABI/signature validation are still required.

## Integration consequences

1. The project can target the real `libcocos2dcpp.so` in both ABIs rather than guessing a generic library name.
2. Do not link GD Hub against a second copy of Cocos. The game owns the Cocos runtime; an eventual in-process adapter must use the target process's existing runtime.
3. Before any Cocos object creation, verify exported symbols, class declarations, object ownership, scene lifecycle, and game-thread dispatch separately for ARM32 and ARM64.
4. The decompiled repository remains a reference only. Its reconstructed declarations must be compared with the supplied APK.
5. No injection, hook, or in-game UI code is claimed to be implemented by this audit.

## Next engineering task

Inspect dynamic symbols and the Java/native startup path, then define a version-specific adapter boundary. Only after a safe, verified load point is identified should the native Cocos view implementation be added.

## Dynamic symbol-table verification

The two `libcocos2dcpp.so` files were also inspected with the ELF dynamic symbol table (not only printable strings). The following Cocos symbols are present as dynamic function symbols in both ABIs:

- `_ZN7cocos2d10CCDirector14sharedDirectorEv` — `CCDirector::sharedDirector()`
- `_ZN7cocos2d6CCNode8addChildEPS0_` — `CCNode::addChild(CCNode*)`
- `_ZN7cocos2d6CCNode11setPositionEff` — `CCNode::setPosition(float, float)`
- `_ZN7cocos2d6CCNode8setScaleEf` — `CCNode::setScale(float)`
- `_ZN7cocos2d6CCNode26removeFromParentAndCleanupEb` — `CCNode::removeFromParentAndCleanup(bool)`
- `_ZN7cocos2d8CCSprite6createEPKc` — `CCSprite::create(char const*)`
- `_ZN7cocos2d13CCLabelBMFont6createEPKcS2_` — `CCLabelBMFont::create(char const*, char const*)`

The scene delegate method `_ZN7cocos2d15CCSceneDelegate17willSwitchToSceneEPNS_7CCSceneE` is also present as a weak dynamic symbol in both ABI libraries. This does not by itself establish that replacing or intercepting it is safe.

## GD Hub symbol probe

Added `loader/game/cocos_runtime_symbols.hpp/.cpp`. On Android it checks whether the game's already-loaded `libcocos2dcpp.so` can be found with `RTLD_NOLOAD`, then resolves the verified dynamic symbols from that library handle. It does not load a second Cocos runtime, call any game functions, create nodes, or install hooks. It reports readiness only when the required symbols are present.

The symbol probe has host-side tests using a fake resolver. The Android build must still confirm that the target NDK supports the no-load probe and that the library is visible in the cloned game's linker namespace.
