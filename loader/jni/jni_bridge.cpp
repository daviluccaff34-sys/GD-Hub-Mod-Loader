#include <jni.h>
#include "runtime/mod_runtime.hpp"
#include <string>
#include <vector>

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
