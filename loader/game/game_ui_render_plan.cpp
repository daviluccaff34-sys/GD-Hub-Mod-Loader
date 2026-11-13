#include "loader/game/game_ui_render_plan.hpp"

#include <algorithm>

namespace gdhub::game {
namespace {

constexpr UiColor kNavy{10, 18, 42, 245};
constexpr UiColor kPanelBlue{18, 35, 72, 250};
constexpr UiColor kCyan{35, 205, 245, 255};
constexpr UiColor kLime{190, 245, 70, 255};
constexpr UiColor kYellow{255, 218, 75, 255};
constexpr UiColor kWhite{245, 250, 255, 255};
constexpr UiColor kMuted{135, 160, 190, 255};
constexpr UiColor kOff{65, 82, 108, 255};

UiDrawItem item(UiDrawKind kind, Rect bounds, UiColor fill, UiColor stroke,
                float stroke_width, float radius, const std::string& text = {},
                float font_size = 0.0f, bool active = false) {
    return {kind, bounds, fill, stroke, stroke_width, radius, font_size, text, active};
}

} // namespace

GameUiRenderPlan build_game_ui_render_plan(
    const GameUiLayout& layout,
    bool panel_open,
    const std::vector<ModEntry>& mods) {
    GameUiRenderPlan plan;

    plan.items.push_back(item(UiDrawKind::Launcher, layout.floating_button,
                              kNavy, kCyan, 2.5f, layout.floating_button.width * 0.5f,
                              "GD", 15.0f, panel_open));
    plan.hit_targets.push_back({UiHitAction::TogglePanel, layout.floating_button, {}});

    if (!panel_open) return plan;

    const Rect panel = layout.panel;
    const float scale = std::max(0.5f, std::min(2.0f, panel.width / 560.0f));
    const float pad = 14.0f * scale;
    const float header_h = std::min(54.0f * scale, panel.height * 0.22f);
    const float close_size = std::max(0.0f, std::min(30.0f * scale, header_h - 8.0f * scale));

    plan.items.push_back(item(UiDrawKind::Panel, panel, kNavy, kCyan, 2.5f,
                              12.0f * scale));
    plan.items.push_back(item(UiDrawKind::Header,
                              {panel.x, panel.y + panel.height - header_h,
                               panel.width, header_h},
                              kPanelBlue, kCyan, 1.0f, 10.0f * scale));
    plan.items.push_back(item(UiDrawKind::Text,
                              {panel.x + pad, panel.y + panel.height - header_h,
                               panel.width - 2.0f * pad - close_size, header_h},
                              kPanelBlue, kPanelBlue, 0.0f, 0.0f,
                              "GD HUB  /  MODS", 18.0f * scale));
    const Rect close_bounds{
        panel.x + panel.width - pad - close_size,
        panel.y + panel.height - (header_h + close_size) * 0.5f,
        close_size, close_size
    };
    plan.items.push_back(item(UiDrawKind::CloseButton, close_bounds,
                              kPanelBlue, kYellow, 1.8f, close_size * 0.25f,
                              "X", 16.0f * scale));
    plan.hit_targets.push_back({UiHitAction::ClosePanel, close_bounds, {}});

    const float row_h = 42.0f * scale;
    const float row_gap = 5.0f * scale;
    const float content_top = panel.y + panel.height - header_h - pad;
    const float content_bottom = panel.y + pad;
    const float available = std::max(0.0f, content_top - content_bottom);
    const std::size_t visible_count = std::min(
        mods.size(), static_cast<std::size_t>(available / (row_h + row_gap)));

    for (std::size_t i = 0; i < visible_count; ++i) {
        const float y = content_top - row_h - static_cast<float>(i) * (row_h + row_gap);
        const Rect row{panel.x + pad, y, panel.width - 2.0f * pad, row_h};
        const Rect toggle{row.x + row.width - 52.0f * scale,
                          row.y + 8.0f * scale,
                          42.0f * scale, 26.0f * scale};
        const Rect knob{mods[i].enabled ? toggle.x + toggle.width - 22.0f * scale
                                        : toggle.x + 4.0f * scale,
                        toggle.y + 4.0f * scale,
                        18.0f * scale, 18.0f * scale};
        plan.items.push_back(item(UiDrawKind::ModRow, row, kPanelBlue, kCyan,
                                  0.8f, 7.0f * scale, mods[i].name));
        plan.items.push_back(item(UiDrawKind::Text,
                                  {row.x + 9.0f * scale, row.y,
                                   row.width - 70.0f * scale, row.height},
                                  kPanelBlue, kPanelBlue, 0.0f, 0.0f,
                                  mods[i].name, 13.0f * scale));
        plan.items.push_back(item(UiDrawKind::ToggleTrack, toggle,
                                  mods[i].enabled ? kLime : kOff,
                                  mods[i].enabled ? kLime : kMuted,
                                  1.0f, toggle.height * 0.5f, {}, 0.0f,
                                  mods[i].enabled));
        plan.items.push_back(item(UiDrawKind::ToggleKnob, knob, kWhite,
                                  kWhite, 0.5f, knob.width * 0.5f, {}, 0.0f,
                                  mods[i].enabled));
        plan.hit_targets.push_back({UiHitAction::ToggleMod, toggle, mods[i].id});
    }

    if (mods.empty()) {
        plan.items.push_back(item(UiDrawKind::Text,
                                  {panel.x + pad, content_bottom,
                                   panel.width - 2.0f * pad, available},
                                  kPanelBlue, kPanelBlue, 0.0f, 0.0f,
                                  "NO MODS INSTALLED", 13.0f * scale));
    } else if (visible_count < mods.size()) {
        plan.items.push_back(item(UiDrawKind::Text,
                                  {panel.x + pad, content_bottom,
                                   panel.width - 2.0f * pad, 18.0f * scale},
                                  kPanelBlue, kPanelBlue, 0.0f, 0.0f,
                                  "MORE MODS HIDDEN", 10.0f * scale));
    }

    return plan;
}

std::optional<UiHitTarget> hit_test_game_ui(
    const GameUiRenderPlan& plan, float x, float y) {
    for (auto it = plan.hit_targets.rbegin(); it != plan.hit_targets.rend(); ++it) {
        const Rect& bounds = it->bounds;
        if (bounds.width <= 0.0f || bounds.height <= 0.0f) continue;
        if (x >= bounds.x && x <= bounds.x + bounds.width &&
            y >= bounds.y && y <= bounds.y + bounds.height) {
            return *it;
        }
    }
    return std::nullopt;
}

} // namespace gdhub::game
