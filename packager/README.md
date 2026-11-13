# GDHB Packager

Run from the repository root:

    python3 packager/gdhb_pack.py --manifest manifest.json --source main.dash --output my-mod.gdhb

The manifest must contain id, name, and version as strings. The tool writes a GDHB v1 package containing manifest.json and src/main.dash.

**This is packaging, not compilation.** The original DashCode source is preserved inside the package. Current packager limits are 4 MiB per entry, 16 MiB per package, and 128 entries.
