#include "loader/game/game_ui_layout.hpp"

#include <algorithm>

namespace gdhub::game {

GameUiLayout calculate_game_ui_layout(Size visible, Insets safe, float ui_scale) {
    const float scale = std::max(0.5f, std::min(ui_scale, 2.0f));
    const float left = std::max(0.0f, safe.left);
    const float bottom = std::max(0.0f, safe.bottom);
    const float right = std::max(left, visible.width - std::max(0.0f, safe.right));
    const float top = std::max(bottom, visible.height - std::max(0.0f, safe.top));
    const float usable_w = std::max(1.0f, right - left);
    const float usable_h = std::max(1.0f, top - bottom);

    const float button_size = 42.0f * scale;
    const float button_margin = 16.0f * scale;
    const float button_x = std::max(left, right - button_margin - button_size);
    const float button_y = std::max(bottom, bottom + button_margin);

    const float panel_margin = 18.0f * scale;
    const float panel_w = std::min(560.0f * scale,
                                   std::max(1.0f, usable_w - 2.0f * panel_margin));
    const float panel_h = std::min(420.0f * scale,
                                   std::max(1.0f, usable_h - 2.0f * panel_margin));

    GameUiLayout result;
    result.floating_button = {button_x, button_y, button_size, button_size};
    result.panel = {
        left + (usable_w - panel_w) * 0.5f,
        bottom + (usable_h - panel_h) * 0.5f,
        panel_w,
        panel_h
    };
    return result;
}

} // namespace gdhub::game
