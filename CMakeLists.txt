cmake_minimum_required(VERSION 3.22)
project(GDHubModLoader VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(gdhub_loader SHARED
    loader/core/loader.cpp
    runtime/package/gdhb_reader.cpp
    runtime/language/dash_interpreter.cpp
)

target_include_directories(gdhub_loader PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
)

if(ANDROID)
    target_compile_definitions(gdhub_loader PRIVATE GDHUB_ANDROID=1)
endif()
