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

    # Build the ARM32/ARM64 inline-hook engine from source and link it
    # statically into DL Loader. The shared Dobby target is excluded from
    # the default build; no third-party .so is loaded or shipped.
    include(FetchContent)
    FetchContent_Declare(
        dobby_source
        GIT_REPOSITORY https://github.com/jmpews/Dobby.git
        GIT_TAG 5dfc8546954ce3b3198132ab13fddb89ee92cdd7
        GIT_SHALLOW FALSE
    )
    FetchContent_GetProperties(dobby_source)
    if(NOT dobby_source_POPULATED)
        FetchContent_Populate(dobby_source)

        # Compatibility fixes for the pinned Dobby revision with the Android NDK:
        # its ARM register header includes Cpu.h, while this revision provides
        # the base declaration in CpuRegister.h.
        file(MAKE_DIRECTORY "${dobby_source_SOURCE_DIR}/source/core/arch")
        file(WRITE "${dobby_source_SOURCE_DIR}/source/core/arch/Cpu.h"
            "#pragma once\n#include \"core/arch/CpuRegister.h\"\n")

        # Dobby's AArch64 closure bridge uses Mach-O @PAGE/@PAGEOFF relocations
        # unconditionally. Android ELF/Clang requires the ELF :lo12: form.
        set(_dobby_arm64_bridge
            "${dobby_source_SOURCE_DIR}/source/TrampolineBridge/ClosureTrampolineBridge/arm64/closure_bridge_arm64.asm")
        file(READ "${_dobby_arm64_bridge}" _dobby_arm64_asm)
        string(REPLACE
            "adrp TMP_REG_0, cdecl(common_closure_bridge_handler)@PAGE"
            "adrp TMP_REG_0, common_closure_bridge_handler"
            _dobby_arm64_asm "${_dobby_arm64_asm}")
        string(REPLACE
            "add TMP_REG_0, TMP_REG_0, cdecl(common_closure_bridge_handler)@PAGEOFF"
            "add TMP_REG_0, TMP_REG_0, :lo12:common_closure_bridge_handler"
            _dobby_arm64_asm "${_dobby_arm64_asm}")
        file(WRITE "${_dobby_arm64_bridge}" "${_dobby_arm64_asm}")

        add_subdirectory(${dobby_source_SOURCE_DIR} ${dobby_source_BINARY_DIR} EXCLUDE_FROM_ALL)
    endif()

    target_link_libraries(gdhub_loader PRIVATE dl dobby_static)
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
