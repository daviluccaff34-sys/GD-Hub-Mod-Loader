#pragma once

#include <functional>
#include <string>
#include <vector>

namespace gdhub::game {

struct ModEntry {
    std::string id;
    std::string name;
    bool enabled = false;
};

// Implemented by the version-specific Geometry Dash adapter.
// Every method that touches game UI must run on the game's render thread.
class GameUiHost {
public:
    virtual ~GameUiHost() = default;
    virtual bool is_game_thread() const = 0;
    virtual bool create_floating_button(std::function<void()> on_tap) = 0;
    virtual void show_mod_panel(const std::vector<ModEntry>& mods,
                                std::function<void(const std::string&, bool)> on_toggle,
                                std::function<void()> on_close) = 0;
    virtual void refresh_mod_panel(const std::vector<ModEntry>& mods) = 0;
    virtual void hide_mod_panel() = 0;
};

// Implemented by the game-side package catalog. It must only report a mod as
// enabled after its runtime activation has actually succeeded.
class ModCatalog {
public:
    virtual ~ModCatalog() = default;
    virtual std::vector<ModEntry> list_mods() = 0;
    virtual bool set_enabled(const std::string& id, bool enabled) = 0;
};

class GameUiBridge {
public:
    GameUiBridge(GameUiHost& host, ModCatalog& catalog);
    bool initialize();
    void toggle_panel();
    void close_panel();
    bool panel_open() const { return m_panel_open; }

private:
    void open_panel();
    void refresh_panel();
    void handle_toggle(const std::string& id, bool enabled);

    GameUiHost& m_host;
    ModCatalog& m_catalog;
    bool m_initialized = false;
    bool m_panel_open = false;
};

} // namespace gdhub::game
