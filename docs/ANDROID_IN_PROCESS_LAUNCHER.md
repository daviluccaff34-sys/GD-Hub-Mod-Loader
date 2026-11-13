# Android in-process game launcher

## Target version

The Geode Android Launcher maps Android package `versionCode 41` to unified Geometry Dash `2.2081`. The manifest `versionName` may be `2.2.144`; GD Hub uses the version code mapping rather than that display string.

## Architecture implemented in GD Hub

The launcher does not start the separate Geometry Dash Activity. It hosts the game engine in `GeometryDashActivity`, an Activity declared in the GD Hub manifest and running in the GD Hub process.

Current executable pieces:

- `GeometryDashActivity` reads the installed Geometry Dash package, adds its base and split APK assets to the host AssetManager, and resolves the device process ABI.
- It extracts or copies `libfmod.so` and `libcocos2dcpp.so` into GD Hub's private directory, then loads FMOD first and Cocos2d-x second.
- The adapted Kotlin bridge preserves the JNI package/class names expected by Geometry Dash: `org.cocos2dx.lib`, `org.fmod`, and `com.customRobTop.JniToCpp`.
- `Cocos2dxGLSurfaceView` creates an OpenGL ES 2 surface and forwards touch and key events to the Cocos renderer.
- `Cocos2dxRenderer` calls the game's native initialization after the GL surface receives its size, then asks `GdHubRuntime` to inspect the already-loaded Cocos runtime.
- `LauncherFix` ports Geode's Android path redirection and compatibility patcher. It redirects the game's old `/data/data/com.robtopx.geometryjump` paths to GD Hub's private save directory and enables the exception-symbol patch for version codes 39 and newer.
- `FMOD` lifecycle and the Cocos accelerometer helper are connected to the Activity lifecycle.
- A GD Hub floating button is layered over the game at the lower-right. It opens the installed `.gdhb` list and can run a package through the native DashCode interpreter, showing its output in the panel.

## Loading order

1. Add Geometry Dash base/split APK assets to the host AssetManager.
2. Initialize FMOD's Android bridge.
3. Initialize the Cocos helper and its writable save path.
4. Load `libfmod.so`.
5. Load `libcocos2dcpp.so`.
6. Configure and execute `LauncherFix` while the game library is loaded.
7. Set the APK path and HSS asset paths expected by Geometry Dash.
8. Create the OpenGL surface; its renderer calls the game's native Cocos initialization.
9. After native initialization, load `libgdhub_loader.so` on demand and inspect the loaded Cocos symbols.
10. Show the GD Hub overlay and execute selected `.gdhb` packages through the interpreter.

## Important remaining work

The current overlay is a real Android view layered above the game, not yet a Cocos scene node. The DashCode interpreter currently implements text output, variables, conditions and repeat blocks; it does not yet expose Geometry Dash scene mutation, triggers, editor objects, or a Cocos UI API. Therefore a package can execute and display interpreter output, but it cannot yet change Geometry Dash's actual scene or gameplay.

The code has not yet been installed and tested on a physical Android device. CI builds are necessary but do not prove that the installed Geometry Dash binary starts successfully on a phone. Device testing must verify the exact game build, ARM ABI, native library load, asset access, save path, touch input, audio and pause/resume lifecycle before claiming full compatibility.

## Upstream source and license

The Kotlin Cocos/FMOD bridge and native path patcher are adapted from the Geode Android Launcher revision `9e136f728d35bdd60b8e66f382e0d1e405376854`. Their Boost Software License 1.0 is included in `third_party/geode/LICENSE.txt`, and adapted files are listed in `THIRD_PARTY_NOTICES.md`.
