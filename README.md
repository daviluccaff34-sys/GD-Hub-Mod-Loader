# DL Loader

An independent Android Geometry Dash mod loader, evolved from the GD Hub project. The current target is Geometry Dash **2.2081**.

## Product identity
- APK display name: **DL Loader**
- Launcher branding: DL monogram
- Android targets: ARM32 (`armeabi-v7a`) and ARM64 (`arm64-v8a`)
- Native core: C++17
- Current mod package: `.gdhb` (manifest and DashCode source; not compiled native code)

## Existing foundation
- DashCode interpreter and bounded execution runtime.
- GDHB v1 package reader and Python packager.
- Android package manager and JNI runtime.
- Existing Android Geometry Dash host and Cocos runtime symbol probe.
- Native tests and Android ARM32/ARM64 build workflows.

## DL Loader direction
The GD Hub floating launcher ball and its overlay panel are being retired. The intended in-game entry point is a small DL icon integrated beside the Newgrounds button in the Geometry Dash main menu. Activating it will open a dedicated DL Loader screen, separate from the game's original main-menu screen. DL Loader will also have its own sound effects.

The native integration is being developed around `DLCocosBridge`. The current symbol probe only checks whether selected Cocos2d-x symbols are present in the already-loaded game library. It does **not** yet call those functions or create Cocos nodes. The bridge introduced in this migration first reports verified symbol availability; actual Cocos calls will be added only with confirmed signatures, render-thread checks, and device tests.

## Build native tests

    cmake -S . -B build -DBUILD_TESTING=ON
    cmake --build build
    ctest --test-dir build --output-on-failure

## Create a mod package

    python3 packager/gdhb_pack.py --manifest examples/hello_dash/manifest.json --source examples/hello_dash/main.dash --output hello-dash.gdhb

The packager preserves DashCode source; it does not compile it.

## Android
Open the repository in Android Studio and build the `app` module, or use the APK artifact produced by GitHub Actions.

See `docs/ROADMAP.md`, `docs/FORMAT_GDHB.md`, `docs/LANGUAGE.md`, `docs/C_API.md`, `docs/ANDROID_TESTING.md` and `docs/DL_LOADER_MIGRATION.md`.
