# Third-party notices

## Geode Android Launcher reference

Repository: https://github.com/geode-sdk/android-launcher
Reference revision inspected: `9e136f728d35bdd60b8e66f382e0d1e405376854`
License: **Boost Software License 1.0** (often abbreviated BSL-1.0)
License text: https://github.com/geode-sdk/android-launcher/blob/9e136f728d35bdd60b8e66f382e0d1e405376854/LICENSE.txt

GD Hub uses this project's Android in-process launcher architecture as a technical reference. This commit does not copy Geode source files. GD Hub's host Activity and runtime bridge are to be implemented as GD Hub-owned code while preserving the JNI interfaces required by Geometry Dash. If source code is incorporated in a future change, its original copyright notices and the complete Boost Software License 1.0 text must accompany it.

## Version mapping reference

The Geode Android launcher's `GamePackageUtils` maps Android package `versionCode 41` to Geometry Dash `2.2081`. The APK's Android `versionName` may differ (for example, `2.2.144`); GD Hub uses `versionCode` to identify the unified game version.
