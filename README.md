# GD Hub Mod Loader

Independent Android mod-loader research project targeting Geometry Dash **2.2081**.

## Targets
- Android ARM32: `armeabi-v7a`
- Android ARM64: `arm64-v8a`
- Native core: C++17
- Package: `.gdhb` — manifest and source files, **not compiled code**

## Implemented prototype
- DashCode interpreter: variables, output, addition, conditions, loops and text interpolation.
- Runtime limits for source size, instruction count and output.
- GDHB v1 reader with size bounds, safe relative paths, duplicate detection and required-file checks.
- ModRuntime connects the package reader to the interpreter.
- Python packager at `packager/gdhb_pack.py`.
- Android companion app in `app/`: imports, lists, removes and runs packages through JNI.
- GitHub Actions configured for native tests, ARM32/ARM64 shared-library builds and an APK artifact.

**Important:** the companion app is not an in-game overlay. The project does not yet connect to Geometry Dash's process or expose real game actions. The in-game integration is a separate milestone and must use a verified, version-specific integration boundary.

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
