# Third-party notices

## Geode Android Launcher

Repository: https://github.com/geode-sdk/android-launcher
Source revision used for the Android bridge files: `9e136f728d35bdd60b8e66f382e0d1e405376854`
License: Boost Software License 1.0 (BSL-1.0)
License text is included at [third_party/geode/LICENSE.txt](third_party/geode/LICENSE.txt).

The following source files were adapted from the Geode Android Launcher:
- `Cocos2dxRenderer.kt`
- `Cocos2dxAccelerometer.kt`
- `Cocos2dxBitmap.kt`
- `Cocos2dxTypefaces.kt`
- `FMOD.kt`
- `AudioDevice.kt`
- `MediaCodec.kt`
- `JniToCpp.kt`

The GD Hub host Activity, Cocos2d-x surface view, helper, runtime bridge and UI overlay are GD Hub-specific implementations. Adapted files retain their upstream package names where required by Geometry Dash JNI compatibility. The upstream project license and notices are preserved here.

## Cocos2d-x accelerometer source notice

`Cocos2dxAccelerometer.kt` also retains the upstream cocos2d-x MIT notice in its source header.
