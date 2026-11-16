#include "loader/game/inline_hook_backend.hpp"

#if defined(__ANDROID__) && (defined(__arm__) || defined(__aarch64__))
#include <dobby.h>
#endif

namespace gdhub::game {

bool install_inline_hook(void* target, void* replacement, void** original) {
    if (!target || !replacement || !original) return false;
    *original = nullptr;

#if defined(__ANDROID__) && (defined(__arm__) || defined(__aarch64__))
    // Dobby performs instruction relocation and creates the original-code
    // trampoline for both supported Android ARM ABIs.
    if (DobbyHook(target, replacement, original) != 0 || !*original) {
        *original = nullptr;
        return false;
    }
    return true;
#else
    return false;
#endif
}

} // namespace gdhub::game
