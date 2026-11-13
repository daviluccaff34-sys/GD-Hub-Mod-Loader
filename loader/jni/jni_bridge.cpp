namespace {
std::atomic_bool g_dash_cancel{false};
std::mutex g_dash_run_mutex;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_gdhub_loader_GdHubRuntime_nativeRunDashCode(JNIEnv* env, jclass, jstring java_source) {
    if (!java_source) return env->NewStringUTF("ERROR\nCódigo vazio.");
    const char* chars = env->GetStringUTFChars(java_source, nullptr);
    if (!chars) return env->NewStringUTF("ERROR\nNão foi possível ler o código.");
    const std::string source(chars);
    env->ReleaseStringUTFChars(java_source, chars);
    std::lock_guard<std::mutex> lock(g_dash_run_mutex);
    g_dash_cancel.store(false, std::memory_order_relaxed);
    gdhub::language::DashInterpreter interpreter;
    std::vector<std::string> lines;
    if (!interpreter.run(source, lines, &g_dash_cancel)) {
        const std::string error = "ERROR\n" + interpreter.last_error();
        return env->NewStringUTF(error.c_str());
    }
    std::string result = "OK\n";
    for (const auto& line : lines) result += line + "\n";
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_gdhub_loader_GdHubRuntime_nativeStopDashCode(JNIEnv*, jclass) {
    g_dash_cancel.store(true, std::memory_order_relaxed);
}
#include <jni.h>
#include "runtime/mod_runtime.hpp"
#include "loader/game/cocos_runtime_symbols.hpp"
#include <string>
#include <vector>
#include <atomic>
#include <mutex>

extern "C" JNIEXPORT jstring JNICALL
Java_com_gdhub_loader_MainActivity_nativeRunPackage(JNIEnv* env, jobject,
                                                      jstring java_path) {
    if (java_path == nullptr) {
        return env->NewStringUTF("ERROR\nCaminho do pacote vazio.");
    }
    const char* chars = env->GetStringUTFChars(java_path, nullptr);
    if (chars == nullptr) {
        return env->NewStringUTF("ERROR\nNão foi possível ler o caminho.");
    }
    const std::string path(chars);
    env->ReleaseStringUTFChars(java_path, chars);

    gdhub::runtime::ModRuntime runtime;
    gdhub::runtime::PackageInfo info;
    std::vector<std::string> output;
    if (!runtime.run_package(path, info, output)) {
        const std::string error = "ERROR\n" + runtime.last_error();
        return env->NewStringUTF(error.c_str());
    }

    std::string result = "OK\n" + info.name + " (" + info.id + ") v" + info.version;
    for (const auto& line : output) result += "\n" + line;
    return env->NewStringUTF(result.c_str());
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_gdhub_loader_GdHubRuntime_nativeRunPackage(JNIEnv* env, jclass,
                                                     jstring java_path) {
    if (java_path == nullptr) {
        return env->NewStringUTF("ERROR\nCaminho do pacote vazio.");
    }
    const char* chars = env->GetStringUTFChars(java_path, nullptr);
    if (chars == nullptr) {
        return env->NewStringUTF("ERROR\nNão foi possível ler o caminho.");
    }
    const std::string path(chars);
    env->ReleaseStringUTFChars(java_path, chars);

    gdhub::runtime::ModRuntime runtime;
    gdhub::runtime::PackageInfo info;
    std::vector<std::string> output;
    if (!runtime.run_package(path, info, output)) {
        const std::string error = "ERROR\n" + runtime.last_error();
        return env->NewStringUTF(error.c_str());
    }

    std::string result = "OK\n" + info.name + " (" + info.id + ") v" + info.version;
    for (const auto& line : output) result += "\n" + line;
    return env->NewStringUTF(result.c_str());
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_gdhub_loader_GdHubRuntime_nativeOnGameReady(JNIEnv* env, jclass) {
    const auto symbols = gdhub::game::inspect_loaded_cocos_runtime();
    const char* state = symbols.has_required_symbols()
        ? "Cocos2d-x carregado; símbolos principais encontrados."
        : "Cocos2d-x foi carregado, mas alguns símbolos esperados não foram encontrados.";
    return env->NewStringUTF(state);
}
