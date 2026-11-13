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
- [x] Build and upload the companion APK artifact in CI (workflow run 43384744508)
- [ ] Install and validate the companion APK on a physical device
- [x] Document the version-specific integration boundary and staged plan for Geometry Dash 2.2081
- [x] Add the in-process GeometryDashActivity and Cocos2d-x OpenGL host (implemented; physical-device test pending)
- [x] Add Cocos JNI bridge, FMOD bridge, APK/split asset resolution and native library loading order
- [x] Add Geode-derived save-path redirection and version-code compatibility patcher
- [x] Add pause/resume, back-key, touch forwarding and a GD Hub overlay above the game
- [x] Load the GD Hub native runtime after Cocos native initialization and expose package execution to the overlay\n- [x] Add a first runtime bridge from the in-game overlay to the DashCode package interpreter (scene/game API still pending)
- [ ] Define package permissions and enforce them in the runtime
- [ ] Add runtime API for supported game actions
- [x] Add an Android overlay mod panel above the game surface (not yet a Cocos scene node)

## Important boundaries
- A .gdhb is a package containing source code, not a compiled mod.
- The reader validates the package and returns src/main.dash as text; ModRuntime passes it to the DashCode interpreter.
- The in-game overlay can invoke the DashCode interpreter while Geometry Dash is hosted in GD Hub.
- The in-process host and runtime bridge are implemented in source but are not yet verified on a physical device. The current DashCode runtime does not yet expose Cocos scene changes, gameplay hooks, triggers, or editor-object APIs.
- Full game modification still requires a verified, version-specific Cocos integration and real-device testing; no private offsets or guessed memory layouts are used.

## Mod catalog status

- [x] Add a moderated Cloudflare Worker API for GDHB submissions, approved catalog listings, and downloads.
- [x] Validate GDHB v1 package structure on the server before accepting a submission.
- [x] Create Workers KV namespace `GDHUB_MOD_CATALOG` and bind it to the deployed catalog Worker.
- [x] Deploy `gdhub-mod-catalog` and configure its HTTPS API URL in the Android build.
- [ ] Add the `ADMIN_TOKEN` Worker secret, then test upload, pending review, approval, listing, and download.

## Geometry Dash APK identity warning

Geode Android Launcher's version map identifies Android `versionCode 41` as Geometry Dash `2.2081`. The Android manifest's `versionName` may show `2.2.144`; use the version-code mapping, not that string alone, for target identification.


## GD Hub Mod Creator — First Release preparation (13 Nov 2026)
- [x] Add searchable independent DashPT code-piece library (3,420 fragments; not Scratch blocks).
- [x] Add JavaScript extension API to register, search, retrieve and insert code fragments.
- [x] Add construction draft editor for objects, groups, triggers, properties and removal by ID.
- [x] Export construction draft as `level/build.json` and publish its JSON schema.
- [x] Add Studio catalog page to list approved mods, submit GDHB packages, download approved packages and moderate pending submissions.
- [ ] Connect the Studio to the actual deployed Worker URL and verify CORS from the Pages domain.
- [ ] Add/configure `ADMIN_TOKEN` secret and test real moderation end-to-end.
- [ ] Implement native runtime parsing for `level/build.json`, object/group/trigger actions and safe permission enforcement.
- [ ] Implement verified Cocos2d-x adapter for Geometry Dash 2.2081.
- [ ] Run browser tests, native CI, Android build and physical-device validation before declaring version 1.00.0 released.

The code-piece and construction editors are authoring/export tools. They do not imply that all tokens are executable game functions. Native game integration remains a separate unfinished milestone.

- [x] Reduce the in-game floating launcher from 52dp to 40dp.
- [x] Reduce the companion in-game panel to 240×260dp and allow dragging it by the header; launcher dragging remains enabled.

- [x] Add a Hello DashPT example with source DashPT and compiled DashCode for the current interpreter.
