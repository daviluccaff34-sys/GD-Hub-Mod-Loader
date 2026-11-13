# Safe Geometry Dash clone plan

## Decision

The original Geometry Dash installation must remain untouched. GD Hub will prepare a **separate clone APK** with a distinct Android application ID, so Android can install it alongside the original.

The clone must never be installed as an update over `com.robtopx.geometryjump`. Re-signing an APK while keeping the original package ID can make Android reject the update; uninstalling the original to work around that can put its private save data at risk.

## Data and persistence

- The original game's private data remains owned by the original installation.
- The clone has its own private data directory and its own save state.
- GD Hub keeps imported `.gdhb` files in its own app-private `files/mods` directory.
- Enabled/disabled choices in the manager are persisted using app preferences.
- Uninstalling GD Hub clears its private mod library unless the user exports or backs it up first.
- The original and clone may each use the game's own account/cloud-save features, but GD Hub must not promise automatic transfer of private save data.

## Clone preparation pipeline (not implemented yet)

1. Let the user select an APK they are authorized to modify, or obtain the installed base APK through Android package APIs where permitted.
2. Verify the APK package name, version, ABI contents, and signing state.
3. Decode and update the manifest using a maintained Android APK tooling library; assign a unique clone application ID and label.
4. Add only the matching GD Hub native library for each ABI. Do not mix ARM32 and ARM64 binaries.
5. Rebuild, zip-align, sign with a GD Hub clone key, and verify the output package before offering installation.
6. Install the clone as a separate app. Keep the original installed and do not request uninstall permissions for it.
7. On failure, keep the source APK and all existing app data unchanged.

## Critical integration gate

Repackaging alone does not create a working Cocos integration. The injected native library must have a verified initialization route for Geometry Dash Android 2.2081, and must use the game's actual modified Cocos2d-x 2.2.3 ABI. No guessed addresses, offsets, or function signatures are acceptable.

The clone pipeline must not be advertised as complete until an output APK has been built, signature-verified, installed alongside the original, and launched successfully on a real device. In-game UI must be separately verified.

## Current status

This document is an implementation contract. The manager now remembers each package's enabled selection and keeps imported package files in app-private storage. The APK clone builder, clone signing pipeline, and game-side Cocos adapter are still pending.
