#include "loader/game/dl_cocos_bridge.hpp"

namespace gdhub::game {

DLCocosSnapshot DLCocosBridge::inspect() const {
    DLCocosSnapshot snapshot;
    snapshot.symbols = inspect_loaded_cocos_runtime();

    if (!snapshot.symbols.library_loaded) {
        snapshot.status = DLCocosStatus::LibraryUnavailable;
    } else if (!snapshot.symbols.has_required_symbols()) {
        snapshot.status = DLCocosStatus::SymbolsIncomplete;
    } else {
        snapshot.status = DLCocosStatus::SymbolsDetected;
    }

    return snapshot;
}

} // namespace gdhub::game
