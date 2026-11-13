#include "runtime/c_api/gdhub_c_api.h"
#include "runtime/mod_runtime.hpp"
#include <cstring>
#include <new>
#include <string>
#include <vector>

struct gdhub_runtime_handle {
    gdhub::runtime::ModRuntime runtime;
    std::string error;
};

extern "C" uint32_t gdhub_api_version(void) {
    return GDHUB_C_API_VERSION;
}

extern "C" gdhub_runtime_handle* gdhub_runtime_create(void) {
    return new (std::nothrow) gdhub_runtime_handle();
}

extern "C" int gdhub_runtime_run_package(gdhub_runtime_handle* handle,
                                           const char* package_path,
                                           char* output,
                                           size_t output_capacity,
                                           size_t* output_required) {
    if (output_required) *output_required = 0;
    if (!handle || !package_path) return -1;

    handle->error.clear();
    gdhub::runtime::PackageInfo info;
    std::vector<std::string> lines;
    if (!handle->runtime.run_package(package_path, info, lines)) {
        handle->error = handle->runtime.last_error();
        return -2;
    }

    std::string joined;
    for (std::size_t i = 0; i < lines.size(); ++i) {
        if (i) joined.push_back('\n');
        joined += lines[i];
    }
    const std::size_t required = joined.size() + 1;
    if (output_required) *output_required = required;
    if (!output || output_capacity < required) {
        handle->error = "Buffer de saída insuficiente.";
        return -3;
    }
    std::memcpy(output, joined.c_str(), required);
    return 0;
}

extern "C" const char* gdhub_runtime_last_error(gdhub_runtime_handle* handle) {
    if (!handle) return "Runtime inválido.";
    return handle->error.c_str();
}

extern "C" void gdhub_runtime_destroy(gdhub_runtime_handle* handle) {
    delete handle;
}
