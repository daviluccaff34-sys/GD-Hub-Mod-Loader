# GDHB package format (proposal)

A `.gdhb` package will contain:
- `manifest.json`: package identity, version and required runtime version;
- `src/`: source files written in the GD Hub language;
- optional `assets/`: images, audio and other mod resources.

## Important
- Packaging is **not compilation**.
- Source files remain source files inside the package.
- The loader runtime will interpret supported language instructions.
- The exact archive container, manifest schema, permissions and API will be finalized before the first compatible release.
- Packages must be validated before execution; untrusted packages must not receive unrestricted file or network access.
