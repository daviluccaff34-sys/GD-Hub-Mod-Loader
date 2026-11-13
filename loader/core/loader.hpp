#pragma once

#include <string>

namespace gdhub {
class Loader {
public:
    bool initialize();
    const std::string& version() const;

private:
    std::string m_version = "0.1.0-alpha";
};
}
