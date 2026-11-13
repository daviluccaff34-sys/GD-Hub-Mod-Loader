#include "loader/game/game_ui_bridge.hpp"

namespace gdhub::game {

GameUiBridge::GameUiBridge(GameUiHost& host, ModCatalog& catalog)
    : m_host(host), m_catalog(catalog) {}

bool GameUiBridge::initialize() {
    if (m_initialized || !m_host.is_game_thread()) return false;

    // The host creates a native Cocos button at the lower-right screen edge.
    if (!m_host.create_floating_button([this]() { toggle_panel(); })) return false;
    m_initialized = true;
    return true;
}

void GameUiBridge::toggle_panel() {
    if (!m_initialized || !m_host.is_game_thread()) return;
    if (m_panel_open) close_panel();
    else open_panel();
}

void GameUiBridge::open_panel() {
    if (!m_initialized || !m_host.is_game_thread()) return;
    m_host.show_mod_panel(
        m_catalog.list_mods(),
        [this](const std::string& id, bool enabled) {
            handle_toggle(id, enabled);
        },
        [this]() { close_panel(); });
    m_panel_open = true;
}

void GameUiBridge::close_panel() {
    if (!m_initialized || !m_host.is_game_thread() || !m_panel_open) return;
    m_host.hide_mod_panel();
    m_panel_open = false;
}

void GameUiBridge::refresh_panel() {
    if (!m_panel_open || !m_host.is_game_thread()) return;
    m_host.refresh_mod_panel(m_catalog.list_mods());
}

void GameUiBridge::handle_toggle(const std::string& id, bool enabled) {
    if (!m_initialized || !m_panel_open || !m_host.is_game_thread()) return;
    // The catalog owns activation; UI state is refreshed from its real result.
    m_catalog.set_enabled(id, enabled);
    refresh_panel();
}

} // namespace gdhub::game
