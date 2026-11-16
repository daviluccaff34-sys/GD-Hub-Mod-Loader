# Uploaded Geometry Dash APK audit — target 2.2081

This audit was performed against the user-provided `Geometry Dash_2.2081.apk`. The APK is not copied into this repository.

## Version identification

- Project/compatibility target: **Geometry Dash 2.2081**.
- Android manifest `versionName`: **2.2.144**.
- Android manifest `versionCode`: **41**.

For this project, the uploaded file is the intended **2.2081** target. The manifest's internal versionName is a separate identifier; it is not grounds to change the DL Loader target to another public release. All observations below refer to this exact uploaded APK and must not be generalized to other builds without another audit.

## Verified archive contents

- APK size: 179,868,202 bytes.
- ZIP entries: 6,177.
- DEX: `classes.dex`.
- Package: `com.robtopx.geometryjump`.
- Launcher activity: `com.robtopx.geometryjump.GeometryJump`.
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

## Dynamic Cocos symbols verified in both ABIs

The following symbols are present in the dynamic function symbol tables:

- `_ZN7cocos2d10CCDirector14sharedDirectorEv` — `CCDirector::sharedDirector()`
- `_ZN7cocos2d6CCNode8addChildEPS0_` — `CCNode::addChild(CCNode*)`
- `_ZN7cocos2d6CCNode11setPositionEff` — `CCNode::setPosition(float, float)`
- `_ZN7cocos2d6CCNode8setScaleEf` — `CCNode::setScale(float)`
- `_ZN7cocos2d6CCNode26removeFromParentAndCleanupEb` — `CCNode::removeFromParentAndCleanup(bool)`
- `_ZN7cocos2d8CCSprite6createEPKc` — `CCSprite::create(char const*)`
- `_ZN7cocos2d13CCLabelBMFont6createEPKcS2_` — `CCLabelBMFont::create(char const*, char const*)`

## Geometry Dash entry-point symbols

The uploaded libraries also export the following game-specific initialization methods:

| Method | ARM64 ELF value | ARM32 ELF value |
|---|---:|---:|
| `MenuLayer::init()` | `0x5c2e7c` | `0x30a735` |
| `PlayLayer::init(GJGameLevel*, bool, bool)` | `0x5e5fd0` | `0x31c8b1` |
| `LevelEditorLayer::init(GJGameLevel*, bool)` | `0x67c170` | `0x376439` |

These are ELF symbol values from the respective ABI libraries, not absolute runtime addresses. In particular, ARM32 values may carry Thumb-state encoding. Do not hardcode them as callable pointers or copy them across ABIs. Prefer resolving the exact mangled symbols from the already-loaded game library.

The corresponding mangled names are:
- `_ZN9MenuLayer4initEv`
- `_ZN9PlayLayer4initEP11GJGameLevelbb`
- `_ZN16LevelEditorLayer4initEP11GJGameLevelb`

Their presence gives concrete methods for further lifecycle research. It does not establish that detouring these methods is safe, nor does it establish the correct hook timing, object ownership, thread, or re-entrancy behavior.

## Android startup path

The manifest and DEX identify:

- Minimum Android SDK: 23.
- Target Android SDK: 35.
- DEX superclass of the launcher activity: `org.cocos2dx.lib.Cocos2dxActivity`.
- Application metadata `android.app.lib_name`: `cocos2dcpp`.
- Cocos Java runtime classes include `Cocos2dxActivity`, `Cocos2dxGLSurfaceView`, and `Cocos2dxRenderer`.

Both ABI libraries export `JNI_OnLoad` and Cocos renderer JNI functions. This confirms the native/Java runtime boundary but does not identify a safe point for attaching DL Loader.

## Integration consequences

1. DL Loader remains targeted at **Geometry Dash 2.2081**.
2. The game owns the Cocos runtime. Do not link or load a second Cocos runtime.
3. The in-process adapter must resolve symbols from the already-loaded `libcocos2dcpp.so`.
4. Keep ARM32 and ARM64 symbol resolution and ABI declarations separate.
5. Before calling Cocos or Geometry Dash methods, verify signatures, class layouts, refcount/ownership rules, render-thread dispatch, lifecycle, and re-entrancy.
6. The decompiled repository is a reference only; compare its declarations with this APK.
7. No hook or native in-game UI is claimed to be implemented by this audit.

## Current probe status

The existing `DLCocosBridge` only checks whether the game library is already loaded and whether selected Cocos symbols resolve. It does not call Cocos functions, create nodes, attach UI, or install hooks. The next implementation step is a version-specific, testable symbol resolver for the game entry points above, followed by lifecycle validation before any hook is enabled.

## Native dependency profile

Both ABI variants of `libcocos2dcpp.so` depend on `libfmod.so`, `libGLESv2.so`, `liblog.so`, `libz.so`, `libdl.so`, `libstdc++.so`, `libm.so`, and `libc.so`. The APK contains `libcocos2dcpp.so` and `libfmod.so` for each ABI. This is an archive observation; do not replace these game libraries with third-party binaries.
