#pragma once

namespace gdhub::game {

using CocosSymbolLookup = void* (*)(const char* mangled_name);

struct CocosRuntimeSymbols {
    bool library_loaded = false;
    void* director_shared = nullptr;
    void* node_add_child = nullptr;
    void* node_set_position = nullptr;
    void* node_set_scale = nullptr;
    void* node_remove_from_parent = nullptr;
    void* sprite_create = nullptr;
    void* label_create = nullptr;

    bool has_required_symbols() const;
};

// Uses the supplied lookup function. This path is also testable without a game.
CocosRuntimeSymbols inspect_cocos_symbols(CocosSymbolLookup lookup);

// Android-only runtime probe. It checks the already-loaded game library; it
// never loads a second Cocos runtime and never invokes any Cocos function.
CocosRuntimeSymbols inspect_loaded_cocos_runtime();

} // namespace gdhub::game
