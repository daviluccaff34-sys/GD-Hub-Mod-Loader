#pragma once
#include "runtime/package/gdhb_reader.hpp"
#include "runtime/language/dash_interpreter.hpp"
#include <string>
#include <vector>

namespace gdhub::runtime {
class ModRuntime {
public:
    // Reads a .gdhb and interprets its src/main.dash in memory.
    bool run_package(const std::string& package_path, PackageInfo& info,
                     std::vector<std::string>& output);
    const std::string& last_error() const { return m_error; }
private:
    GdhbReader m_reader;
    gdhub::language::DashInterpreter m_interpreter;
    std::string m_error;
};
}
