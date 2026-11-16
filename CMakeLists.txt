cmake_minimum_required(VERSION 3.22)
project(DLLoader VERSION 0.3.0 LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(gdhub_loader SHARED
    loader/core/loader.cpp
    loader/game/game_ui_bridge.cpp
    loader/game/game_ui_layout.cpp
    loader/game/game_ui_render_plan.cpp
    loader/game/cocos_runtime_symbols.cpp
    loader/game/inline_hook_backend.cpp
    loader/game/dl_cocos_bridge.cpp
    runtime/package/gdhb_reader.cpp
    runtime/language/dash_interpreter.cpp
    runtime/mod_runtime.cpp
    runtime/c_api/gdhub_c_api.cpp
)
target_include_directories(gdhub_loader PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
if(ANDROID)
    target_compile_definitions(gdhub_loader PRIVATE GDHUB_ANDROID=1)

    # Build Dobby from source and link only its static target into DL Loader.
    # This pinned revision contains the ARM32 CodeBuffer headers and avoids
    # the broken ARM/ARM64 source layout in newer upstream revisions.
    include(FetchContent)
    FetchContent_Declare(
        dobby_source
        GIT_REPOSITORY https://github.com/jmpews/Dobby.git
        GIT_TAG 0932d69c320e786672361ab53825ba8f4245e9d3
        GIT_SHALLOW FALSE
    )
    FetchContent_GetProperties(dobby_source)
    if(NOT dobby_source_POPULATED)
        FetchContent_Populate(dobby_source)
        add_subdirectory(${dobby_source_SOURCE_DIR} ${dobby_source_BINARY_DIR} EXCLUDE_FROM_ALL)
    endif()

    # The shared Dobby target is never linked or packaged.
    target_link_libraries(gdhub_loader PRIVATE dl log dobby_static)
    target_sources(gdhub_loader PRIVATE loader/jni/jni_bridge.cpp)

    add_library(gdhub_launcherfix SHARED
        loader/launcherfix/launcher-fix.cpp
        loader/launcherfix/patcher.cpp
    )
    target_include_directories(gdhub_launcherfix PRIVATE loader/launcherfix)
    target_compile_features(gdhub_launcherfix PRIVATE cxx_std_20)
    target_link_libraries(gdhub_launcherfix PRIVATE dl log)
endif()

include(CTest)
if(BUILD_TESTING AND NOT ANDROID)
    add_executable(gdhub_tests tests/test_main.cpp)
    target_link_libraries(gdhub_tests PRIVATE gdhub_loader)
    add_test(NAME gdhub_tests COMMAND gdhub_tests)
endif()
