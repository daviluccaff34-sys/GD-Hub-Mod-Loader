# Uploaded Geometry Dash APK audit — version mismatch found

This audit was performed against the user-provided APK in the current conversation. The APK itself is not copied into this repository.

> **Important correction:** the uploaded APK is **not identified as Geometry Dash 2.2081**. Its Android manifest reports versionName `2.2.144` and versionCode `41`. Therefore, every native-library observation below applies to the uploaded **2.2.144** APK only. Do not treat these symbols or the Cocos probe as verified compatibility with 2.2081. A matching 2.2081 APK is required before implementing the version-specific game adapter.

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

These findings establish that the supplied 2.2.144 APK packages a Cocos2d-based native game library for both ABIs. A string match alone does not prove that a symbol is exported or safe to call; dynamic symbol-table inspection and exact ABI/signature validation are still required.

## Integration consequences

1. The project can target the real `libcocos2dcpp.so` in both ABIs rather than guessing a generic library name.
2. Do not link GD Hub against a second copy of Cocos. The game owns the Cocos runtime; an eventual in-process adapter must use the target process's existing runtime.
3. Before any Cocos object creation, verify exported symbols, class declarations, object ownership, scene lifecycle, and game-thread dispatch separately for ARM32 and ARM64.
4. The decompiled repository remains a reference only. Its reconstructed declarations must be compared with the supplied APK.
5. No injection, hook, or in-game UI code is claimed to be implemented by this audit.

## Next engineering task

Inspect dynamic symbols and the Java/native startup path, then define a version-specific adapter boundary. Only after a safe, verified load point is identified should the native Cocos view implementation be added.

## Dynamic symbol-table verification

The two `libcocos2dcpp.so` files from the uploaded 2.2.144 APK were also inspected with the ELF dynamic symbol table (not only printable strings). The following Cocos symbols are present as dynamic function symbols in both ABIs:

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

## Android manifest and startup path

Decoded from the uploaded APK's binary Android manifest and DEX class definitions:

- Package: `com.robtopx.geometryjump`.
- Manifest version name: `2.2.144`.
- Manifest version code: `41`.
- Minimum Android SDK: `23`.
- Target Android SDK: `35`.
- Launcher activity: `com.robtopx.geometryjump.GeometryJump`.
- DEX superclass: `org.cocos2dx.lib.Cocos2dxActivity`.
- Application metadata `android.app.lib_name`: `cocos2dcpp`.
- The DEX contains the Cocos2d Android Java runtime classes, including `Cocos2dxActivity`, `Cocos2dxGLSurfaceView`, and `Cocos2dxRenderer`.

Both native libraries export `JNI_OnLoad` and Cocos renderer JNI functions including `nativeInit`, `nativeRender`, `nativeOnResume`, `nativeTouchesBegin`, `nativeTouchesEnd`, and `nativeTouchesCancel`. The ARM64 library also exports several Java JNI callbacks for `com.customRobTop.JniToCpp`.

## Important hook warning

`CCSceneDelegate::willSwitchToScene(CCScene*)` appears as a **weak** dynamic symbol with a very small symbol size in both uploaded libraries (4 bytes in ARM64 and 2 bytes in ARM32). This is not evidence of a usable scene hook. Do not patch or replace it based on the symbol name alone.

The verified exported Cocos functions are useful for compatibility research, but their presence does not prove a safe initialization point, thread context, ownership rules, or ABI-compatible C++ declarations. No native function is called by the current symbol probe.
