# Android targets

Native ABIs:
- armeabi-v7a (ARM32)
- arm64-v8a (ARM64)

The root CMake project builds libgdhub_loader.so for both ABIs. The Android companion app in app/ packages those libraries into a debug APK using Android Gradle Plugin and the NDK.

GitHub Actions is configured to:
- build the native library separately for ARM32 and ARM64;
- build the companion APK containing both ABIs;
- publish the native libraries and APK as workflow artifacts.

A configured workflow is not proof of a successful build. Check the latest Actions run and test the APK on a physical device before calling the Android build verified.

The companion app is separate from Geometry Dash. No in-game loading, hooks or game actions are implemented yet.
