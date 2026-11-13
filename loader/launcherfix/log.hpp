#pragma once
#include <android/log.h>
#include <utility>

#define LOG_TAG "GDHub-LauncherFix"

namespace log {
template<typename... Args>
inline void debug(const char* message, Args&&...) {
    __android_log_write(ANDROID_LOG_DEBUG, LOG_TAG, message);
}
template<typename... Args>
inline void info(const char* message, Args&&...) {
    __android_log_write(ANDROID_LOG_INFO, LOG_TAG, message);
}
template<typename... Args>
inline void warn(const char* message, Args&&...) {
    __android_log_write(ANDROID_LOG_WARN, LOG_TAG, message);
}
template<typename... Args>
inline void error(const char* message, Args&&...) {
    __android_log_write(ANDROID_LOG_ERROR, LOG_TAG, message);
}
}
