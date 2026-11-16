#pragma once

#include "loader/game/cocos_runtime_symbols.hpp"

namespace gdhub::game {

// Diagnostic boundary for the game's already-running Cocos2d-x instance.
// Symbol detection is deliberately separate from hook installation/readiness.
enum class DLCocosStatus {
    LibraryUnavailable,
    CoreSymbolsIncomplete,
    SceneLifecycleSymbolsIncomplete,
    SymbolsDetected
};

struct DLCocosSnapshot {
    DLCocosStatus status = DLCocosStatus::LibraryUnavailable;
    CocosRuntimeSymbols symbols{};

    // A future, independently tested hook backend must explicitly set this.
    // Symbol resolution alone must never authorize attachment.
    bool hook_backend_ready = false;

    bool symbols_detected() const {
        return status == DLCocosStatus::SymbolsDetected;
    }

    bool can_attach() const {
        return symbols_detected() && hook_backend_ready;
    }
};

class DLCocosBridge {
public:
    // Safe diagnostic probe. Call after Geometry Dash has initialized Cocos.
    // No Cocos function is called and no hook is installed by this operation.
    DLCocosSnapshot inspect() const;
};

} // namespace gdhub::game
