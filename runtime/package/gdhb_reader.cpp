#include "gdhb_reader.hpp"
#include <fstream>
#include <limits>
#include <set>
#include <utility>

namespace gdhub::runtime {
namespace {
constexpr std::uint32_t kMaxPackageBytes = 16u * 1024u * 1024u;
constexpr std::uint32_t kMaxEntryBytes = 4u * 1024u * 1024u;
constexpr std::uint16_t kMaxEntries = 128;

bool read_u16(std::istream& in, std::uint16_t& value) {
    unsigned char b[2];
    if (!in.read(reinterpret_cast<char*>(b), 2)) return false;
    value = static_cast<std::uint16_t>(b[0] | (b[1] << 8));
    return true;
}
bool read_u32(std::istream& in, std::uint32_t& value) {
    unsigned char b[4];
    if (!in.read(reinterpret_cast<char*>(b), 4)) return false;
    value = static_cast<std::uint32_t>(b[0]) |
            (static_cast<std::uint32_t>(b[1]) << 8) |
            (static_cast<std::uint32_t>(b[2]) << 16) |
            (static_cast<std::uint32_t>(b[3]) << 24);
    return true;
}
bool safe_path(const std::string& path) {
    if (path.empty() || path.front() == '/' || path.find('\\') != std::string::npos) return false;
    std::size_t start = 0;
    while (start <= path.size()) {
        const auto end = path.find('/', start);
        const auto part = path.substr(start, end == std::string::npos ? end : end - start);
        if (part.empty() || part == "." || part == "..") return false;
        if (end == std::string::npos) break;
        start = end + 1;
    }
    return true;
}
std::string json_string(const std::string& json, const std::string& key) {
    const std::string token = "\"" + key + "\"";
    auto at = json.find(token);
    if (at == std::string::npos) return {};
    at = json.find(':', at + token.size());
    if (at == std::string::npos) return {};
    at = json.find('"', at + 1);
    if (at == std::string::npos) return {};
    std::string value;
    for (++at; at < json.size(); ++at) {
        const char c = json[at];
        if (c == '"') return value;
        if (c == '\\') {
            if (++at >= json.size()) return {};
            const char escaped = json[at];
            if (escaped == '"' || escaped == '\\' || escaped == '/') value += escaped;
            else if (escaped == 'n') value += '\n';
            else if (escaped == 't') value += '\t';
            else return {};
        } else value += c;
    }
    return {};
}
}

bool GdhbReader::inspect(const std::string& file_path, PackageInfo& out_info) const {
    m_error.clear();
    out_info = {};
    std::ifstream in(file_path, std::ios::binary | std::ios::ate);
    if (!in) { m_error = "Não foi possível abrir o pacote."; return false; }
    const std::streamoff file_size = static_cast<std::streamoff>(in.tellg());
    if (file_size < 8 || static_cast<std::uint64_t>(file_size) > kMaxPackageBytes) {
        m_error = "Tamanho de pacote inválido (limite: 16 MiB)."; return false;
    }
    in.seekg(0);
    char magic[4];
    if (!in.read(magic, 4) || std::string(magic, 4) != "GDHB") {
        m_error = "Assinatura GDHB inválida."; return false;
    }
    std::uint16_t version = 0, count = 0;
    if (!read_u16(in, version) || !read_u16(in, count) || version != 1 || count == 0 || count > kMaxEntries) {
        m_error = "Cabeçalho GDHB v1 inválido."; return false;
    }
    std::string manifest;
    std::string main_source;
    std::set<std::string> names;
    std::uint64_t total = 8;
    for (std::uint16_t i = 0; i < count; ++i) {
        std::uint16_t path_size = 0;
        std::uint32_t data_size = 0;
        if (!read_u16(in, path_size) || !read_u32(in, data_size) || path_size == 0 ||
            path_size > 240 || data_size > kMaxEntryBytes) {
            m_error = "Entrada GDHB inválida."; return false;
        }
        total += 6u + path_size + data_size;
        if (total > static_cast<std::uint64_t>(file_size)) { m_error = "Pacote truncado."; return false; }
        std::string path(path_size, '\0');
        std::string data(data_size, '\0');
        if (!in.read(path.data(), path_size) || !in.read(data.data(), data_size)) {
            m_error = "Pacote truncado durante a leitura."; return false;
        }
        if (!safe_path(path) || !names.insert(path).second) {
            m_error = "Caminho inseguro ou duplicado no pacote."; return false;
        }
        if (path == "manifest.json") manifest = std::move(data);
        if (path == "src/main.dash") main_source = std::move(data);
    }
    if (total != static_cast<std::uint64_t>(file_size)) { m_error = "Existem dados extras no fim do pacote."; return false; }
    if (manifest.empty() || main_source.empty()) {
        m_error = "O pacote precisa de manifest.json e src/main.dash."; return false;
    }
    out_info.id = json_string(manifest, "id");
    out_info.name = json_string(manifest, "name");
    out_info.version = json_string(manifest, "version");
    if (out_info.id.empty() || out_info.name.empty() || out_info.version.empty()) {
        out_info = {};
        m_error = "Manifesto inválido: informe id, name e version como textos."; return false;
    }
    out_info.main_source = std::move(main_source);
    return true;
}
}
