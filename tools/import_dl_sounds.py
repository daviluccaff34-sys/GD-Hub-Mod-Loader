#!/usr/bin/env python3
"""Import the approved DL Loader OGG files from sounds.zip without extracting other entries."""
from pathlib import Path
from zipfile import ZipFile
import argparse
import sys

EXPECTED = (
    "newNotif00.ogg",
    "newNotif01.ogg",
    "newNotif02.ogg",
    "newNotif03.ogg",
    "byeNotif00.ogg",
)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", nargs="?", default="sounds.zip", help="Path to sounds.zip")
    parser.add_argument(
        "--root",
        default=".",
        help="DL Loader repository root (default: current directory)",
    )
    args = parser.parse_args()

    archive = Path(args.archive)
    root = Path(args.root)
    destination = root / "app" / "src" / "main" / "assets" / "sounds"

    if not archive.is_file():
        print(f"Archive not found: {archive}", file=sys.stderr)
        return 2

    try:
        with ZipFile(archive) as bundle:
            members = {}
            for name in EXPECTED:
                candidates = (f"sounds/{name}", name)
                match = next((candidate for candidate in candidates if candidate in bundle.namelist()), None)
                if match is None:
                    raise ValueError(f"Missing required audio: {name}")
                members[name] = bundle.read(match)

            if any(not data for data in members.values()):
                raise ValueError("One or more OGG files are empty")

            destination.mkdir(parents=True, exist_ok=True)
            for name, data in members.items():
                (destination / name).write_bytes(data)
                print(f"Imported {name}: {len(data)} bytes")
    except Exception as error:
        print(f"Import failed: {error}", file=sys.stderr)
        return 1

    print(f"All {len(EXPECTED)} OGG files imported into {destination}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
