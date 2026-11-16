#pragma once

#include "loader/game/cocos_runtime_symbols.hpp"

namespace gdhub::game {

// A deliberately conservative boundary between the loader and the game's
// already-running Cocos2d-x instance. This first migration stage only reports
// runtime readiness; it does not invoke engine functions.
enum class DLCocosStatus {
    LibraryUnavailable,
    SymbolsIncomplete,
    SymbolsDetected
};

struct DLCocosSnapshot {
    DLCocosStatus status = DLCocosStatus::LibraryUnavailable;
    CocosRuntimeSymbols symbols{};

    bool can_attach() const {
        return status == DLCocosStatus::SymbolsDetected;
    }
};

class DLCocosBridge {
public:
    // Safe diagnostic probe. Call after Geometry Dash has initialized Cocos.
    // No Cocos function is called by this operation.
    DLCocosSnapshot inspect() const;
};

} // namespace gdhub::game
