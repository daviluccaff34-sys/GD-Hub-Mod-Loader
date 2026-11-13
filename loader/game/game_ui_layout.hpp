#pragma once

namespace gdhub::game {

struct Insets {
    float left = 0;
    float top = 0;
    float right = 0;
    float bottom = 0;
};

struct Size {
    float width = 0;
    float height = 0;
};

struct Rect {
    float x = 0;
    float y = 0;
    float width = 0;
    float height = 0;
};

struct GameUiLayout {
    Rect floating_button;
    Rect panel;
};

// Pure geometry helper. Coordinates use a bottom-left origin in game points.
// The adapter must pass the game's visible size and safe-area insets.
GameUiLayout calculate_game_ui_layout(Size visible, Insets safe,
                                      float ui_scale = 1.0f);

} // namespace gdhub::game
