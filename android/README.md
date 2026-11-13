# Android targets

Planned and CI build targets:
- armeabi-v7a (ARM32)
- arm64-v8a (ARM64)

Use the Android NDK CMake toolchain. These ABI targets are configuration targets until the Android CI build completes successfully; do not treat them as proof of in-game compatibility.

Native tests run on the host in GitHub Actions. Android artifacts are shared-library builds only; they do not yet load into Geometry Dash.
