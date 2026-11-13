#include "loader.hpp"

namespace gdhub {
bool Loader::initialize() {
    // Initial scaffold only. Game-process integration is a later milestone.
    return true;
}

const std::string& Loader::version() const {
    return m_version;
}
}
