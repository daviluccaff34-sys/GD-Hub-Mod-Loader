# GD Hub Mod Loader

Independent Android mod-loader research project for Geometry Dash **2.2081**.

## Initial targets
- Android ARM32: `armeabi-v7a`
- Android ARM64: `arm64-v8a`
- Native core: C++17
- Mod package: `.gdhb` (packed source + manifest; **not compiled**)

## Project status
**0.1.0-alpha — project scaffold.** This repository currently contains the initial structure and build configuration only. It does not yet inject into, hook, or modify Geometry Dash.

## Planned components
- `loader/`: loader core and game integration boundary
- `runtime/`: future interpreter, API and event system for the GD Hub language
- `packager/`: validation and packaging of source files into `.gdhb`
- `android/`: Android ABI/build support
- `tests/`: format and runtime tests

## Package principle
A `.gdhb` file is a package, not a compiled binary. The loader will eventually read its manifest and source files and execute supported instructions through its own runtime.

## Build
The first milestone is a native library build for Android. A successful build does **not** mean in-game integration is complete.

See [docs/ROADMAP.md](docs/ROADMAP.md) and [docs/FORMAT_GDHB.md](docs/FORMAT_GDHB.md).
