# GD Hub Mod Loader

Independent Android mod-loader research project for Geometry Dash **2.2081**.

## Targets
- Android ARM32: armeabi-v7a
- Android ARM64: arm64-v8a
- Native core: C++17
- Package: .gdhb (manifest + source files; **not compiled**)

## Current prototype
- DashCode interpreter with variables, output, arithmetic addition, conditions, loops and text interpolation.
- GDHB v1 reader with file-size bounds, safe-path checks, duplicate detection and required-file validation.
- Python packager at packager/gdhb_pack.py.
- Native tests run through CMake/CTest and GitHub Actions.

The reader currently returns src/main.dash as text. The loader does not yet install packages, expose game actions, or integrate into the Geometry Dash process.

## Build native tests
    cmake -S . -B build -DBUILD_TESTING=ON
    cmake --build build
    ctest --test-dir build --output-on-failure

## Create a package
    python3 packager/gdhb_pack.py --manifest manifest.json --source main.dash --output example.gdhb

The package manifest must contain text fields id, name, and version. The source remains DashCode and is interpreted later.

See docs/ROADMAP.md and docs/FORMAT_GDHB.md.
