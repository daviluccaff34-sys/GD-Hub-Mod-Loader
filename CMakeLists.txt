cmake_minimum_required(VERSION 3.22)
project(GDHubModLoader VERSION 0.1.0 LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(gdhub_loader SHARED
    loader/core/loader.cpp
    loader/game/game_ui_bridge.cpp
    loader/game/game_ui_layout.cpp
    loader/game/game_ui_render_plan.cpp
    loader/game/cocos_runtime_symbols.cpp
    runtime/package/gdhb_reader.cpp
    runtime/language/dash_interpreter.cpp
    runtime/mod_runtime.cpp
    runtime/c_api/gdhub_c_api.cpp
)
target_include_directories(gdhub_loader PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
if(ANDROID)
    target_compile_definitions(gdhub_loader PRIVATE GDHUB_ANDROID=1)
    target_link_libraries(gdhub_loader PRIVATE dl)
    target_sources(gdhub_loader PRIVATE loader/jni/jni_bridge.cpp)
endif()

include(CTest)
if(BUILD_TESTING AND NOT ANDROID)
    add_executable(gdhub_tests tests/test_main.cpp)
    target_link_libraries(gdhub_tests PRIVATE gdhub_loader)
    add_test(NAME gdhub_tests COMMAND gdhub_tests)
endif()
