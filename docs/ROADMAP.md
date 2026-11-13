# Roadmap

## 0.1.0-alpha — core prototype
- [x] Create repository layout
- [x] Add C++17 project and CMake configuration
- [x] Define Android ABI targets
- [x] Define GDHB v1 container format
- [x] Implement bounded GDHB reader and basic manifest extraction
- [x] Connect package reader to interpreter with ModRuntime
- [x] Implement first DashCode interpreter instructions
- [x] Add Python GDHB packager
- [x] Add native tests and GitHub Actions CI workflow
- [x] Add Android companion manager UI and JNI bridge
- [x] Add ARM32/ARM64 APK build workflow
- [x] Add runtime execution limits
- [x] Add versioned C ABI for future native hosts
- [ ] Verify native CI, Android shared-library builds and APK artifact
- [ ] Run APK on a physical Android device
- [ ] Complete JSON manifest validation and package permissions
- [ ] Expand DashCode grammar and error messages

## 0.2.0 — Android runtime
- [ ] Build reproducible ARM32 and ARM64 Android artifacts with the NDK
- [ ] Add safe bridge between the in-game component and the runtime
- [ ] Define a version-specific integration boundary for Geometry Dash 2.2081
- [ ] Add runtime API for supported game actions
- [x] Add companion mod manager interface (outside the game)
- [ ] Add in-game mod manager interface through a documented integration boundary

## Important boundaries
- A .gdhb is a package containing source code, not a compiled mod.
- The reader returns src/main.dash as text; ModRuntime passes it to the DashCode interpreter. Package execution is available in the companion app, not inside Geometry Dash.
- The current project is not yet an operational in-game mod loader. No injection, hooks, or game modification are implemented.
