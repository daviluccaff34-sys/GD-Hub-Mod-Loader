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
- [x] Add a Publicar Mods tab with GDHB selection, metadata form, local draft saving, and multipart upload client
- [x] Add ARM32/ARM64 APK build workflow
- [x] Add runtime execution limits
- [x] Add versioned C ABI for future native hosts
- [x] Add tests for malformed manifests, duplicate keys and C API null handles
- [x] Verify native CI, Android shared-library builds and APK artifact (run 43379430103)
- [ ] Run APK on a physical Android device
- [ ] Expand DashCode grammar and error messages

## 0.2.0 — Android runtime and game integration
- [x] Build reproducible ARM32 and ARM64 Android shared libraries with the NDK (workflow run 43384744508)
- [x] Build and upload the companion APK artifact in CI (workflow run 43384744508)\n- [ ] Install and validate the companion APK on a physical device
- [x] Document the version-specific integration boundary and staged plan for Geometry Dash 2.2081
- [ ] Port the complete in-process Cocos2d-x Android host (Activity, Java bridge, FMOD bridge, asset redirection and lifecycle) from a license-compatible upstream baseline\n- [ ] Add a safe bridge between a verified in-game component and the runtime
- [ ] Define package permissions and enforce them in the runtime
- [ ] Add runtime API for supported game actions
- [ ] Add in-game mod manager interface through a documented integration boundary

## Important boundaries
- A .gdhb is a package containing source code, not a compiled mod.
- The reader validates the package and returns src/main.dash as text; ModRuntime passes it to the DashCode interpreter.
- Package execution is available in the companion app, not inside Geometry Dash.
- The current project is not yet an operational in-game mod loader. No injection, hooks, or game modification are implemented.
- Game integration cannot be completed safely by guessing private offsets or undocumented memory layouts. It requires a verified, version-specific integration point and real-device testing.

## Mod catalog status

- [x] Add a moderated Cloudflare Worker API for GDHB submissions, approved catalog listings, and downloads.
- [x] Validate GDHB v1 package structure on the server before accepting a submission.
- [x] Create Workers KV namespace `GDHUB_MOD_CATALOG` and bind it to the deployed catalog Worker.
- [x] Deploy `gdhub-mod-catalog` and configure its HTTPS API URL in the Android build.
- [ ] Add the `ADMIN_TOKEN` Worker secret, then test upload, pending review, approval, listing, and download.

## Geometry Dash APK identity warning

Geode Android Launcher's version map identifies Android `versionCode 41` as Geometry Dash `2.2081`. The Android manifest's `versionName` may show `2.2.144`; use the version-code mapping, not that string alone, for target identification.
