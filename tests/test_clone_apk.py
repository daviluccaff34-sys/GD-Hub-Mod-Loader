import importlib.util
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "packager" / "clone_apk.py"
SPEC = importlib.util.spec_from_file_location("clone_apk", SCRIPT)
clone_apk = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(clone_apk)


class StartupInjectionTests(unittest.TestCase):
    def make_decoded(self, directive):
        temp = tempfile.TemporaryDirectory()
        root = Path(temp.name)
        smali = root / "smali" / "com" / "robtopx" / "geometryjump"
        smali.mkdir(parents=True)
        (smali / "GeometryJump.smali").write_text(
            ".class public Lcom/robtopx/geometryjump/GeometryJump;\n"
            ".super Lorg/cocos2dx/lib/Cocos2dxActivity;\n\n"
            ".method public onCreate(Landroid/os/Bundle;)V\n"
            f"    {directive}\n"
            "    invoke-super {p0, p1}, Lorg/cocos2dx/lib/Cocos2dxActivity;->onCreate(Landroid/os/Bundle;)V\n"
            "    return-void\n"
            ".end method\n",
            encoding="utf-8",
        )
        return temp, root, smali / "GeometryJump.smali"

    def test_injects_load_library_and_expands_locals(self):
        temp, root, path = self.make_decoded(".locals 2")
        with temp:
            clone_apk.inject_startup_library_load(root)
            source = path.read_text(encoding="utf-8")
            self.assertIn(".locals 3", source)
            self.assertIn('const-string v2, "gdhub_loader"', source)
            self.assertIn("System;->loadLibrary(Ljava/lang/String;)V", source)

    def test_registers_form_and_idempotence(self):
        temp, root, path = self.make_decoded(".registers 4")
        with temp:
            clone_apk.inject_startup_library_load(root)
            clone_apk.inject_startup_library_load(root)
            source = path.read_text(encoding="utf-8")
            self.assertIn(".registers 5", source)
            self.assertEqual(source.count('"gdhub_loader"'), 1)

    def test_refuses_unrecognized_activity(self):
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(RuntimeError):
                clone_apk.inject_startup_library_load(Path(temp))


if __name__ == "__main__":
    unittest.main()
