#include "loader/game/dl_cocos_bridge.hpp"

namespace gdhub::game {

DLCocosSnapshot DLCocosBridge::inspect() const {
    DLCocosSnapshot snapshot;
    snapshot.symbols = inspect_loaded_cocos_runtime();

    if (!snapshot.symbols.library_loaded) {
        snapshot.status = DLCocosStatus::LibraryUnavailable;
    } else if (!snapshot.symbols.has_core_symbols()) {
        snapshot.status = DLCocosStatus::CoreSymbolsIncomplete;
    } else if (!snapshot.symbols.has_scene_lifecycle_symbols()) {
        snapshot.status = DLCocosStatus::SceneLifecycleSymbolsIncomplete;
    } else {
        snapshot.status = DLCocosStatus::SymbolsDetected;
    }

    // Deliberately false: this project has not implemented or validated
    // a hook backend for the target game build yet.
    snapshot.hook_backend_ready = false;
    return snapshot;
}

} // namespace gdhub::game
