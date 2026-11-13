#include "runtime/language/dash_interpreter.hpp"
#include "runtime/package/gdhb_reader.hpp"
#include "runtime/mod_runtime.hpp"
#include <cstdio>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>

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
    gdhub::runtime::ModRuntime runtime;
    std::vector<std::string> package_output;
    gdhub::runtime::PackageInfo runtime_info;
    ok &= check(runtime.run_package(package_path, runtime_info, package_output),
                "GDHB package runs through ModRuntime");
    ok &= check(package_output.size() == 1 && package_output[0] == "hello",
                "packaged DashCode produces expected output");
    std::remove(package_path.c_str());
    return ok ? 0 : 1;
}
