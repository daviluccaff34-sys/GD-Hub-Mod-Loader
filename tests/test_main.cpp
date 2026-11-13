#include "runtime/language/dash_interpreter.hpp"
#include "runtime/package/gdhb_reader.hpp"
#include "runtime/mod_runtime.hpp"
#include "runtime/c_api/gdhub_c_api.h"
#include "loader/game/game_ui_bridge.hpp"
#include <cstdio>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>


class TestGameUiHost final : public gdhub::game::GameUiHost {
public:
    bool on_game_thread = true;
    bool button_created = false;
    bool panel_visible = false;
    std::function<void()> tap;
    std::function<void(const std::string&, bool)> toggle;
    std::function<void()> close;
    std::vector<gdhub::game::ModEntry> shown;
    bool is_game_thread() const override { return on_game_thread; }
    bool create_floating_button(std::function<void()> callback) override {
        button_created = true; tap = std::move(callback); return true;
    }
    void show_mod_panel(const std::vector<gdhub::game::ModEntry>& mods,
                        std::function<void(const std::string&, bool)> on_toggle,
                        std::function<void()> on_close) override {
        panel_visible = true; shown = mods; toggle = std::move(on_toggle); close = std::move(on_close);
    }
    void refresh_mod_panel(const std::vector<gdhub::game::ModEntry>& mods) override { shown = mods; }
    void hide_mod_panel() override { panel_visible = false; }
};

class TestModCatalog final : public gdhub::game::ModCatalog {
public:
    bool enabled = false;
    std::vector<gdhub::game::ModEntry> list_mods() override {
        return {{"demo.ui", "UI Demo", enabled}};
    }
    bool set_enabled(const std::string& id, bool value) override {
        if (id != "demo.ui") return false;
        enabled = value; return true;
    }
};

static bool check(bool value, const char* label) {
    if (!value) std::cerr << "FAIL: " << label << '\n';
    else std::cout << "PASS: " << label << '\n';
    return value;
}

int main() {
    bool ok = true;
    gdhub::language::DashInterpreter interpreter;
    std::vector<std::string> output;
    const std::string program =
        "make name = \"GD Hub\"\n"
        "make count = 2\n"
        "add 3 to count\n"
        "say \"{name}: {count}\"\n"
        "if count >= 5\n"
        "say \"ready\"\n"
        "else\n"
        "say \"no\"\n"
        "end\n"
        "repeat 2\n"
        "say \"go\"\n"
        "end\n";
    ok &= check(interpreter.run(program, output), "DashCode runs basic commands");
    ok &= check(output.size() == 4 && output[0] == "GD Hub: 5" &&
                output[1] == "ready" && output[2] == "go" && output[3] == "go",
                "variables, interpolation, if/else and repeat");
    ok &= check(!interpreter.run("say missing\n", output), "unknown variable is rejected");
    ok &= check(!interpreter.run("repeat 1.5\nsay \"bad\"\nend\n", output),
                "repeat rejects fractional counts");
    ok &= check(!interpreter.run("if true\nsay \"missing end\"\n", output),
                "unterminated blocks are rejected");
    ok &= check(!interpreter.run("else\n", output) && !interpreter.run("end\n", output),
                "stray else/end markers are rejected");
    const std::string runaway =
        "repeat 1000\nrepeat 1000\nmake x = 1\nend\nend\n";
    ok &= check(!interpreter.run(runaway, output) && output.empty(),
                "execution budget stops excessive nested loops");

    const std::string package_path = "test_sample.gdhb";
    {
        std::ofstream out(package_path, std::ios::binary);
        const auto u16 = [&out](unsigned v) {
            out.put(static_cast<char>(v & 255));
            out.put(static_cast<char>((v >> 8) & 255));
        };
        const auto u32 = [&out](unsigned v) {
            for (int i = 0; i < 4; ++i) out.put(static_cast<char>((v >> (8 * i)) & 255));
        };
        const auto entry = [&](const std::string& path, const std::string& data) {
            u16(static_cast<unsigned>(path.size()));
            u32(static_cast<unsigned>(data.size()));
            out.write(path.data(), static_cast<std::streamsize>(path.size()));
            out.write(data.data(), static_cast<std::streamsize>(data.size()));
        };
        out.write("GDHB", 4);
        u16(1);
        u16(2);
        entry("manifest.json", "{\"id\":\"sample\",\"name\":\"Sample\",\"version\":\"1.0\"}");
        entry("src/main.dash", "say \"hello\"\n");
    }
    gdhub::runtime::GdhbReader reader;
    gdhub::runtime::PackageInfo info;
    ok &= check(reader.inspect(package_path, info), "GDHB package is read");
    ok &= check(info.id == "sample" && info.main_source == "say \"hello\"\n",
                "manifest and DashCode source are extracted");

    const auto write_test_package = [](const std::string& path, const std::string& manifest) {
        std::ofstream out(path, std::ios::binary);
        auto u16 = [&out](unsigned v) { out.put(static_cast<char>(v & 255)); out.put(static_cast<char>((v >> 8) & 255)); };
        auto u32 = [&out](unsigned v) { for (int i=0;i<4;++i) out.put(static_cast<char>((v >> (8*i)) & 255)); };
        auto entry = [&](const std::string& name, const std::string& data) {
            u16(static_cast<unsigned>(name.size())); u32(static_cast<unsigned>(data.size()));
            out.write(name.data(), static_cast<std::streamsize>(name.size()));
            out.write(data.data(), static_cast<std::streamsize>(data.size()));
        };
        out.write("GDHB",4); u16(1); u16(2);
        entry("manifest.json",manifest); entry("src/main.dash","say \"ok\"\n");
    };
    const std::string invalid_json_path = "test_invalid_json.gdhb";
    write_test_package(invalid_json_path, "{\"id\":\"bad\",\"name\":,\"version\":\"1\"}");
    ok &= check(!reader.inspect(invalid_json_path, info), "GDHB rejects malformed manifest JSON");
    const std::string nested_key_path = "test_nested_manifest.gdhb";
    write_test_package(nested_key_path, "{\"meta\":{\"id\":\"fake\"},\"id\":\"real\",\"name\":\"Name\",\"version\":\"1\"}");
    ok &= check(reader.inspect(nested_key_path, info) && info.id == "real",
                "manifest fields are read only from the top-level object");
    const std::string duplicate_key_path = "test_duplicate_manifest.gdhb";
    write_test_package(duplicate_key_path, "{\"id\":\"one\",\"id\":\"two\",\"name\":\"Name\",\"version\":\"1\"}");
    ok &= check(!reader.inspect(duplicate_key_path, info), "GDHB rejects duplicate JSON keys");
    ok &= check(gdhub_runtime_run_package(nullptr, package_path.c_str(), nullptr, 0, nullptr) == -1,
                "C API rejects null runtime handles");
    gdhub::runtime::ModRuntime runtime;
    std::vector<std::string> package_output;
    gdhub::runtime::PackageInfo runtime_info;
    ok &= check(runtime.run_package(package_path, runtime_info, package_output),
                "GDHB package runs through ModRuntime");
    ok &= check(package_output.size() == 1 && package_output[0] == "hello",
                "packaged DashCode produces expected output");
    ok &= check(gdhub_api_version() == 1, "C API reports version 1");
    gdhub_runtime_handle* c_runtime = gdhub_runtime_create();
    size_t required = 0;
    int c_status = gdhub_runtime_run_package(c_runtime, package_path.c_str(), nullptr, 0, &required);
    ok &= check(c_status == -3 && required > 1, "C API reports required output buffer size");
    std::vector<char> c_output(required);
    c_status = gdhub_runtime_run_package(c_runtime, package_path.c_str(), c_output.data(),
                                         c_output.size(), &required);
    ok &= check(c_status == 0 && std::string(c_output.data()) == "hello",
                "C API executes package and returns output");
    gdhub_runtime_destroy(c_runtime);

    const std::string bad_path = "test_bad_path.gdhb";
    {
        std::ofstream out(bad_path, std::ios::binary);
        const std::string unsafe = "../outside.txt";
        out.write("GDHB", 4);
        out.put(1); out.put(0);
        out.put(1); out.put(0);
        const unsigned path_size = static_cast<unsigned>(unsafe.size());
        out.put(static_cast<char>(path_size & 255));
        out.put(static_cast<char>((path_size >> 8) & 255));
        out.put(1); out.put(0); out.put(0); out.put(0);
        out.write(unsafe.data(), static_cast<std::streamsize>(unsafe.size()));
        out.put('x');
    }
    ok &= check(!reader.inspect(bad_path, info), "GDHB rejects directory traversal paths");

    TestGameUiHost ui_host;
    TestModCatalog mod_catalog;
    gdhub::game::GameUiBridge ui_bridge(ui_host, mod_catalog);
    ok &= check(ui_bridge.initialize() && ui_host.button_created,
                "game UI bridge creates floating-button callback on game thread");
    if (ui_host.tap) ui_host.tap();
    ok &= check(ui_bridge.panel_open() && ui_host.panel_visible &&
                ui_host.shown.size() == 1 && ui_host.shown[0].id == "demo.ui",
                "floating-button callback opens panel with catalog entries");
    if (ui_host.toggle) ui_host.toggle("demo.ui", true);
    ok &= check(mod_catalog.enabled && ui_host.shown[0].enabled,
                "panel toggle updates catalog and refreshes actual enabled state");
    if (ui_host.close) ui_host.close();
    ok &= check(!ui_bridge.panel_open() && !ui_host.panel_visible,
                "panel close callback hides panel");
    TestGameUiHost wrong_thread_host;
    wrong_thread_host.on_game_thread = false;
    TestModCatalog wrong_thread_catalog;
    gdhub::game::GameUiBridge wrong_thread_bridge(wrong_thread_host, wrong_thread_catalog);
    ok &= check(!wrong_thread_bridge.initialize() && !wrong_thread_host.button_created,
                "game UI bridge refuses initialization outside game thread");

    std::remove(invalid_json_path.c_str());
    std::remove(nested_key_path.c_str());
    std::remove(duplicate_key_path.c_str());
    std::remove(bad_path.c_str());
    std::remove(package_path.c_str());
    return ok ? 0 : 1;
}
