# GD Hub C API v1

The C API is a small, versioned boundary for future native hosts. It lets a host create a runtime, execute a GDHB package and receive text output without depending on C++ classes.

Header: runtime/c_api/gdhub_c_api.h

Functions:
- gdhub_api_version(): returns API version 1.
- gdhub_runtime_create(): creates a runtime handle.
- gdhub_runtime_run_package(...): validates and runs a package.
- gdhub_runtime_last_error(...): reads the last error.
- gdhub_runtime_destroy(...): releases the handle.

Return codes for gdhub_runtime_run_package:
- 0: success.
- -1: invalid arguments.
- -2: package or DashCode runtime error.
- -3: output buffer is too small; output_required reports the required byte count including the final NUL.

This API is not yet connected to Geometry Dash. It is the runtime boundary that a future, verified host integration can call.
