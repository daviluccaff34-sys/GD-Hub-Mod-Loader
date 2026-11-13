#pragma once
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct gdhub_runtime_handle gdhub_runtime_handle;
#define GDHUB_C_API_VERSION 1u

uint32_t gdhub_api_version(void);
gdhub_runtime_handle* gdhub_runtime_create(void);

/* 0 success, -1 invalid arguments, -2 runtime error, -3 output buffer too small.
   output_required includes the final NUL byte. */
int gdhub_runtime_run_package(gdhub_runtime_handle* handle,
                              const char* package_path,
                              char* output,
                              size_t output_capacity,
                              size_t* output_required);

const char* gdhub_runtime_last_error(gdhub_runtime_handle* handle);
void gdhub_runtime_destroy(gdhub_runtime_handle* handle);

#ifdef __cplusplus
}
#endif
