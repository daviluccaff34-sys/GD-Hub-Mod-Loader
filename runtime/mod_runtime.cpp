#include "runtime/mod_runtime.hpp"

namespace gdhub::runtime {
bool ModRuntime::run_package(const std::string& package_path, PackageInfo& info,
                             std::vector<std::string>& output) {
    m_error.clear();
    output.clear();
    if (!m_reader.inspect(package_path, info)) {
        m_error = m_reader.last_error();
        return false;
    }
    if (!m_interpreter.run(info.main_source, output)) {
        m_error = m_interpreter.last_error();
        output.clear();
        return false;
    }
    return true;
}
}
