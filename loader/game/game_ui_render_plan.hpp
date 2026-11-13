#pragma once

#include "loader/game/game_ui_bridge.hpp"
#include "loader/game/game_ui_layout.hpp"

#include <string>
#include <vector>

namespace gdhub::game {

struct UiColor {
    unsigned char r = 0;
    unsigned char g = 0;
    unsigned char b = 0;
    unsigned char a = 255;
};

enum class UiDrawKind {
    Launcher,
    Panel,
    Header,
    CloseButton,
    ModRow,
    ToggleTrack,
    ToggleKnob,
    Text
};

enum class UiHitAction {
    OpenPanel,
    ClosePanel,
    ToggleMod
};

struct UiDrawItem {
    UiDrawKind kind = UiDrawKind::Text;
    Rect bounds;
    UiColor fill;
    UiColor stroke;
    float stroke_width = 0.0f;
    float corner_radius = 0.0f;
    float font_size = 0.0f;
    std::string text;
    bool active = false;
};

struct UiHitTarget {
    UiHitAction action = UiHitAction::OpenPanel;
    Rect bounds;
    std::string mod_id;
};

struct GameUiRenderPlan {
    std::vector<UiDrawItem> items;
    std::vector<UiHitTarget> hit_targets;
};

// Produces renderer-independent drawing and hit-test instructions. A verified
// Cocos adapter may translate these into Cocos nodes on the game thread.
GameUiRenderPlan build_game_ui_render_plan(
    const GameUiLayout& layout,
    bool panel_open,
    const std::vector<ModEntry>& mods);

} // namespace gdhub::game
