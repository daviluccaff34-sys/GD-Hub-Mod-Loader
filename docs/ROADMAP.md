# Roadmap

## 0.1.0-alpha — core prototype
- [x] Create repository layout
- [x] Add C++17 project and CMake configuration
- [x] Define Android ABI targets
- [x] Define GDHB v1 container format
- [x] Implement bounded GDHB reader, UTF-8 checks and full JSON syntax validation
- [x] Connect package reader to interpreter with ModRuntime
- [x] Implement first DashCode interpreter instructions
- [x] Add Python GDHB packager
- [x] Add native tests and GitHub Actions CI workflow
- [x] Add Android companion manager UI and JNI bridge
- [x] Add ARM32/ARM64 APK build workflow
- [x] Add runtime execution limits
- [x] Add versioned C ABI for future native hosts
- [x] Add tests for malformed manifests, duplicate keys and C API null handles
- [ ] Verify native CI, Android shared-library builds and APK artifact
- [ ] Run APK on a physical Android device
- [ ] Expand DashCode grammar and error messages

## 0.2.0 — Android runtime and game integration
- [ ] Build reproducible ARM32 and ARM64 Android artifacts with the NDK
- [ ] Run and validate the companion APK on a physical device
- [ ] Define a version-specific integration boundary for Geometry Dash 2.2081
- [ ] Add a safe bridge between a verified in-game component and the runtime
- [ ] Define package permissions and enforce them in the runtime
- [ ] Add runtime API for supported game actions
- [ ] Add in-game mod manager interface through a documented integration boundary

## Important boundaries
- A .gdhb is a package containing source code, not a compiled mod.
- The reader validates the package and returns src/main.dash as text; ModRuntime passes it to the DashCode interpreter.
- Package execution is available in the companion app, not inside Geometry Dash.
- The current project is not yet an operational in-game mod loader. No injection, hooks, or game modification are implemented.
- Game integration cannot be completed safely by guessing private offsets or undocumented memory layouts. It requires a verified, version-specific integration point and real-device testing.
