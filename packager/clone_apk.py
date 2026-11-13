#!/usr/bin/env python3
"""Create a separately signed Geometry Dash clone with GD Hub native libraries.

This repackages an APK; it does NOT install a game hook or create an in-game UI.
The injected shared library is only bundled in the APK until a verified game
initialization adapter is implemented.
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
PACKAGE_RE = re.compile(r"^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+$")


def run(command: list[str]) -> None:
    print("+", " ".join(map(str, command)), flush=True)
    subprocess.run(command, check=True)


def tool(name: str, override: str | None = None) -> str:
    candidate = override or shutil.which(name)
    if not candidate:
        raise RuntimeError(f"Ferramenta ausente: {name}. Instale-a e tente novamente.")
    return candidate


def inject_startup_library_load(decoded: Path) -> None:
    """Load the bundled GD Hub library in the Geometry Dash process."""
    matches = []
    for folder in decoded.glob("smali*"):
        candidate = folder / "com" / "robtopx" / "geometryjump" / "GeometryJump.smali"
        if candidate.is_file():
            matches.append(candidate)
    if len(matches) != 1:
        raise RuntimeError("Não foi possível identificar exatamente uma classe GeometryJump.smali.")

    path = matches[0]
    source = path.read_text(encoding="utf-8")
    method_match = re.search(
        r"(?m)^\.method[^\n]* onCreate\(Landroid/os/Bundle;\)V\s*\n", source
    )
    if not method_match:
        raise RuntimeError("GeometryJump.onCreate(Bundle) não foi encontrado.")
    method_end = source.find(".end method", method_match.end())
    if method_end < 0:
        raise RuntimeError("Método GeometryJump.onCreate está incompleto.")
    block = source[method_match.start():method_end]
    if '"gdhub_loader"' in block:
        return

    locals_match = re.search(r"(?m)^\s*\.locals\s+(\d+)\s*$", block)
    registers_match = re.search(r"(?m)^\s*\.registers\s+(\d+)\s*$", block)
    if locals_match:
        local_register = int(locals_match.group(1))
        block = block[:locals_match.start()] + re.sub(
            r"(\.locals\s+)\d+",
            lambda m: m.group(1) + str(local_register + 1),
            block[locals_match.start():locals_match.end()],
        ) + block[locals_match.end():]
    elif registers_match:
        register_count = int(registers_match.group(1))
        local_register = register_count - 2
        if local_register < 0:
            raise RuntimeError("Contagem de registradores smali inválida.")
        block = block[:registers_match.start()] + re.sub(
            r"(\.registers\s+)\d+",
            lambda m: m.group(1) + str(register_count + 1),
            block[registers_match.start():registers_match.end()],
        ) + block[registers_match.end():]
    else:
        raise RuntimeError("onCreate não declara .locals nem .registers.")

    super_call = re.search(
        r"(?m)^\s*invoke-super[^\n]*->onCreate\(Landroid/os/Bundle;\)V[^\n]*$", block
    )
    if not super_call:
        raise RuntimeError("Chamada original de onCreate não foi encontrada; APK não foi alterado.")
    insertion = (
        f'\n    const-string v{local_register}, "gdhub_loader"'
        f'\n    invoke-static {{v{local_register}}}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V'
    )
    block = block[:super_call.end()] + insertion + block[super_call.end():]
    source = source[:method_match.start()] + block + source[method_end:]
    path.write_text(source, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path, help="APK original")
    parser.add_argument("--output", required=True, type=Path, help="APK clone de saída")
    parser.add_argument("--package", default="com.gdhub.geometryjump", help="Novo applicationId")
    parser.add_argument("--label", default="Geometry Dash (GD Hub)", help="Nome exibido no Android")
    parser.add_argument("--lib-arm64", required=True, type=Path, help="libgdhub_loader.so arm64-v8a")
    parser.add_argument("--lib-arm32", required=True, type=Path, help="libgdhub_loader.so armeabi-v7a")
    parser.add_argument("--keystore", type=Path, help="Keystore existente; se omitido, cria um keystore de teste")
    parser.add_argument("--alias", default="gdhubclone")
    parser.add_argument("--storepass", default=os.environ.get("GDHUB_KEYSTORE_PASSWORD"))
    parser.add_argument("--keypass", default=os.environ.get("GDHUB_KEY_PASSWORD"))
    parser.add_argument("--apktool", help="Caminho do apktool.jar ou executável")
    parser.add_argument("--zipalign", help="Caminho do zipalign")
    parser.add_argument("--apksigner", help="Caminho do apksigner")
    parser.add_argument("--keytool", help="Caminho do keytool")
    args = parser.parse_args()

    if not args.input.is_file():
        raise RuntimeError(f"APK de entrada não encontrado: {args.input}")
    for lib in (args.lib_arm64, args.lib_arm32):
        if not lib.is_file():
            raise RuntimeError(f"Biblioteca não encontrada: {lib}")
    if not PACKAGE_RE.fullmatch(args.package):
        raise RuntimeError("Application ID inválido. Use, por exemplo, com.gdhub.geometryjump.")
    if args.package == "com.robtopx.geometryjump":
        raise RuntimeError("O clone não pode usar o ID do jogo original.")
    if not args.storepass:
        raise RuntimeError("Defina GDHUB_KEYSTORE_PASSWORD antes de criar a chave de assinatura.")

    if args.apktool and args.apktool.lower().endswith(".jar"):
        apktool_cmd = [tool("java"), "-jar", str(Path(args.apktool).resolve())]
    else:
        apktool_cmd = [tool("apktool", args.apktool)]
    zipalign = tool("zipalign", args.zipalign)
    apksigner = tool("apksigner", args.apksigner)
    keytool = tool("keytool", args.keytool)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="gdhub-clone-") as temp:
        work = Path(temp)
        decoded = work / "decoded"
        unsigned = work / "clone-unsigned.apk"
        aligned = work / "clone-aligned.apk"
        run(apktool_cmd + ["d", "-f", str(args.input), "-o", str(decoded)])

        manifest = decoded / "AndroidManifest.xml"
        if not manifest.is_file():
            raise RuntimeError("AndroidManifest.xml não encontrado após decodificação.")
        tree = ET.parse(manifest)
        root = tree.getroot()
        original_package = root.attrib.get("package", "")
        if original_package != "com.robtopx.geometryjump":
            raise RuntimeError(f"Pacote inesperado: {original_package!r}. Esperado com.robtopx.geometryjump.")
        root.set("package", args.package)
        app = root.find("application")
        if app is None:
            raise RuntimeError("Elemento application ausente no Manifest.")
        app.set(ANDROID_NS + "label", args.label)
        tree.write(manifest, encoding="utf-8", xml_declaration=True)
        inject_startup_library_load(decoded)

        # Inject only the ABI-matching native binary. Never copy ARM64 into ARM32 or vice versa.
        for abi, source in (("arm64-v8a", args.lib_arm64), ("armeabi-v7a", args.lib_arm32)):
            libdir = decoded / "lib" / abi
            libdir.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, libdir / "libgdhub_loader.so")

        run(apktool_cmd + ["b", str(decoded), "-o", str(unsigned)])
        run([zipalign, "-f", "4", str(unsigned), str(aligned)])

        keystore = args.keystore or (args.output.parent / "gdhub-clone-release.jks")
        if not keystore.exists():
            keypass = args.keypass or args.storepass
            run([keytool, "-genkeypair", "-v", "-keystore", str(keystore),
                 "-storepass", args.storepass, "-keypass", keypass,
                 "-alias", args.alias, "-keyalg", "RSA", "-keysize", "2048",
                 "-validity", "10000", "-dname", "CN=GD Hub Clone, OU=GD Hub, O=GD Hub, L=Unknown, ST=Unknown, C=BR"])
        keypass = args.keypass or args.storepass
        run([apksigner, "sign", "--ks", str(keystore), "--ks-key-alias", args.alias,
             "--ks-pass", "pass:" + args.storepass, "--key-pass", "pass:" + keypass,
             "--out", str(args.output), str(aligned)])
        run([apksigner, "verify", "--verbose", str(args.output)])
        print(f"CLONE_APK={args.output}")
        print(f"ORIGINAL_PACKAGE={original_package}")
        print(f"CLONE_PACKAGE={args.package}")
        print("NOTE=Biblioteca incluída, mas ainda sem inicialização/injeção de UI dentro do jogo.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (RuntimeError, subprocess.CalledProcessError, ET.ParseError) as exc:
        print(f"ERRO: {exc}", file=sys.stderr)
        raise SystemExit(2)
