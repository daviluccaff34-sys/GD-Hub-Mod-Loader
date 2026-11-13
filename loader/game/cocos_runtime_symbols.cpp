#include "loader/game/cocos_runtime_symbols.hpp"

#include <cstddef>

#if defined(__ANDROID__)
#include <dlfcn.h>
#endif

namespace gdhub::game {
namespace {

template <typename Lookup>
CocosRuntimeSymbols inspect(Lookup lookup) {
    CocosRuntimeSymbols result;
    result.director_shared = lookup("_ZN7cocos2d10CCDirector14sharedDirectorEv");
    result.node_add_child = lookup("_ZN7cocos2d6CCNode8addChildEPS0_");
    result.node_set_position = lookup("_ZN7cocos2d6CCNode11setPositionEff");
    result.node_set_scale = lookup("_ZN7cocos2d6CCNode8setScaleEf");
    result.node_remove_from_parent =
        lookup("_ZN7cocos2d6CCNode26removeFromParentAndCleanupEb");
    result.sprite_create = lookup("_ZN7cocos2d8CCSprite6createEPKc");
    result.label_create = lookup("_ZN7cocos2d13CCLabelBMFont6createEPKcS2_");
    return result;
}

} // namespace

bool CocosRuntimeSymbols::has_required_symbols() const {
    return library_loaded && director_shared && node_add_child &&
           node_set_position && node_set_scale && node_remove_from_parent &&
           sprite_create && label_create;
}

CocosRuntimeSymbols inspect_cocos_symbols(CocosSymbolLookup lookup) {
    if (!lookup) return {};
    return inspect(lookup);
}

CocosRuntimeSymbols inspect_loaded_cocos_runtime() {
#if defined(__ANDROID__)
    void* handle = dlopen("libcocos2dcpp.so", RTLD_NOW | RTLD_NOLOAD);
    if (!handle) return {};

    CocosRuntimeSymbols result = inspect([handle](const char* name) {
        return dlsym(handle, name);
    });
    result.library_loaded = true;
    dlclose(handle);
    return result;
#else
    return {};
#endif
}

} // namespace gdhub::game
