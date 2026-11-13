# GD Hub Mod Loader

Independent Android mod-loader research project targeting Geometry Dash **2.2081**.

## Targets
- Android ARM32: `armeabi-v7a`
- Android ARM64: `arm64-v8a`
- Native core: C++17
- Package: `.gdhb` — manifest and source files, **not compiled code**

## Implemented prototype
- DashCode interpreter: variables, output, addition, conditions, loops, text interpolation and UI instructions (`ui title`, `ui label`, `ui button`, `ui clear`).
- Runtime limits for source size, instruction count and output.
- GDHB v1 reader with size bounds, safe relative paths, duplicate detection and required-file checks.
- ModRuntime connects the package reader to the interpreter.
- Python packager at `packager/gdhb_pack.py`.
- Android app in `app/`: imports, lists, removes and runs packages through JNI; hosts Geometry Dash and renders DashCode-generated UI in a touchable layer above the game.
- GitHub Actions configured for native tests, ARM32/ARM64 shared-library builds and an APK artifact.

**Integration boundary:** DashCode-generated labels, titles and buttons are rendered in a touchable Android view layer above the hosted game. This is not a Cocos scene node and does not create editor objects. Real Geometry Dash actions (blocks, portals, triggers and editor changes) still require a verified, version-specific game adapter and device testing. The Cocos symbol probe is diagnostic only; it does not call unverified game functions.

## Build native tests

    cmake -S . -B build -DBUILD_TESTING=ON
    cmake --build build
    ctest --test-dir build --output-on-failure

## Create a package

    python3 packager/gdhb_pack.py --manifest examples/hello_dash/manifest.json --source examples/hello_dash/main.dash --output hello-dash.gdhb

The packager preserves the DashCode source. It does not compile it.

## Android app

Open the repository in Android Studio and build the `app` module, or download the debug APK from the `GD-Hub-Android-APK` artifact of a successful GitHub Actions run.

See `docs/ROADMAP.md`, `docs/FORMAT_GDHB.md`, `docs/LANGUAGE.md`, `docs/C_API.md` and `docs/ANDROID_TESTING.md`.
