#pragma once

#include <string>

namespace gdhub::runtime {
struct PackageInfo {
    std::string id;
    std::string name;
    std::string version;
};

class GdhbReader {
public:
    // Placeholder API. Package parsing and validation are not implemented yet.
    bool inspect(const std::string& file_path, PackageInfo& out_info) const;
};
}
