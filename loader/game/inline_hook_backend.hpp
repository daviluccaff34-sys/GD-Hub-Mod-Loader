#pragma once

namespace gdhub::game {

// Installs an inline detour using the source-built Dobby static library.
// On success, original receives a callable trampoline to the original code.
// Only Android ARM32/ARM64 are supported by the current game target.
bool install_inline_hook(void* target, void* replacement, void** original);

} // namespace gdhub::game
