# Geometry Dash clone packager

The helper at `packager/clone_apk.py` creates a separately signed clone APK.

## Requirements

- Python 3
- Apktool 2.9+ (APK decode/rebuild)
- Android SDK Build Tools (`zipalign`, `apksigner`)
- Java JDK (`keytool`)
- GD Hub native libraries built for both Android ABIs:
  - `libgdhub_loader.so` for `arm64-v8a`
  - `libgdhub_loader.so` for `armeabi-v7a`

## Run

Set a signing password in the environment; do not commit passwords or keystores:

```sh
export GDHUB_KEYSTORE_PASSWORD='choose-a-private-password'
python3 packager/clone_apk.py \
  --input Geometry-Dash.apk \
  --output Geometry-Dash-GDHub.apk \
  --lib-arm64 build/arm64-v8a/libgdhub_loader.so \
  --lib-arm32 build/armeabi-v7a/libgdhub_loader.so
```

The default clone application ID is `com.gdhub.geometryjump`. The original ID
(`com.robtopx.geometryjump`) is rejected. The script preserves the original APK,
decodes and changes the manifest package/label, adds a startup `System.loadLibrary("gdhub_loader")`
call to `GeometryJump.onCreate`, places each native library only in its matching ABI folder,
rebuilds, zip-aligns, signs, and verifies the output.

A new debug/test keystore is created beside the output when one is not supplied.
Back it up securely: Android treats later builds signed with a different key as
a different signer and will not update the installed clone.

## Important integration limitation

This is the **APK clone/repackaging stage**, not a completed in-game mod loader.
The packager now adds a startup load call, so the native library is loaded into the clone's
process if Android can resolve all of its native dependencies. This is not yet a working
in-game mod menu: the verified Cocos2d-x scene adapter, floating button/panel, and bridge
from game UI to the GDHB package runtime still need implementation and real-device testing.
The companion app launches only the separate clone package; it does not launch or modify the
original Geometry Dash installation.
Do not distribute the clone as if in-game mods already work.
