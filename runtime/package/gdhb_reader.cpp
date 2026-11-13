#include "gdhb_reader.hpp"

namespace gdhub::runtime {
bool GdhbReader::inspect(const std::string& file_path, PackageInfo& out_info) const {
    (void)file_path;
    (void)out_info;
    // TODO: implement safe archive reading and manifest validation.
    return false;
}
}
