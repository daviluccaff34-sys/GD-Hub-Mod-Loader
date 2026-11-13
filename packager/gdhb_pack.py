#!/usr/bin/env python3
"""Create a GDHB v1 package. Source files are packed, never compiled."""
import argparse
import json
import pathlib
import struct
import sys

MAX_FILE = 4 * 1024 * 1024
MAX_PACKAGE = 16 * 1024 * 1024
MAX_ENTRIES = 128

def safe_name(name: str) -> bool:
    p = pathlib.PurePosixPath(name)
    return bool(name) and not p.is_absolute() and "\\" not in name and all(x not in ("", ".", "..") for x in name.split("/"))

def main() -> int:
    parser = argparse.ArgumentParser(description="Empacotador GD Hub (.gdhb), sem compilação")
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--source", required=True, help="Arquivo principal .dash")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    manifest_path = pathlib.Path(args.manifest)
    source_path = pathlib.Path(args.source)
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    for key in ("id", "name", "version"):
        if not isinstance(manifest.get(key), str) or not manifest[key]:
            raise SystemExit(f"Manifesto: campo '{key}' precisa ser texto.")
    entries = [("manifest.json", manifest_path.read_bytes()), ("src/main.dash", source_path.read_bytes())]
    if len(entries) > MAX_ENTRIES or any(len(data) > MAX_FILE for _, data in entries):
        raise SystemExit("Limite de quantidade/tamanho excedido.")
    blob = bytearray(b"GDHB" + struct.pack("<HH", 1, len(entries)))
    for name, data in entries:
        if not safe_name(name):
            raise SystemExit("Caminho inválido.")
        encoded = name.encode("utf-8")
        blob += struct.pack("<HI", len(encoded), len(data)) + encoded + data
    if len(blob) > MAX_PACKAGE:
        raise SystemExit("Pacote maior que 16 MiB.")
    pathlib.Path(args.output).write_bytes(blob)
    print(f"Pacote criado: {args.output} ({len(blob)} bytes). Código-fonte preservado.")
    return 0

if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"Erro: {error}", file=sys.stderr)
        sys.exit(1)
