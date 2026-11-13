#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace gdhub::runtime {
struct PackageInfo {
    std::string id;
    std::string name;
    std::string version;
    std::string main_source;
};

class GdhbReader {
public:
    // GDHB v1 is a small, documented container; it stores source, never compiled code.
    bool inspect(const std::string& file_path, PackageInfo& out_info) const;
    const std::string& last_error() const { return m_error; }
private:
    mutable std::string m_error;
};
}
