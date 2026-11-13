#include "gdhb_reader.hpp"
#include <fstream>
#include <limits>
#include <set>
#include <utility>
#include <cstdint>
#include <cctype>

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
bool valid_utf8(const std::string& s) {
    for (std::size_t i=0;i<s.size();) {
        unsigned char c=static_cast<unsigned char>(s[i]);
        if(c<0x80){++i;continue;}
        std::size_t n; std::uint32_t cp;
        if(c>=0xc2&&c<=0xdf){n=2;cp=c&31;}
        else if(c>=0xe0&&c<=0xef){n=3;cp=c&15;}
        else if(c>=0xf0&&c<=0xf4){n=4;cp=c&7;}
        else return false;
        if(i+n>s.size())return false;
        for(std::size_t j=1;j<n;++j){unsigned char d=static_cast<unsigned char>(s[i+j]);if((d&0xc0)!=0x80)return false;cp=(cp<<6)|(d&63);}
        if((n==3&&cp<0x800)||(n==4&&cp<0x10000)||(cp>=0xd800&&cp<=0xdfff)||cp>0x10ffff)return false;
        i+=n;
    }
    return true;
}
class JsonCheck {
    const std::string& s; std::size_t p=0;
    void ws(){while(p<s.size()&&(s[p]==' '||s[p]=='\n'||s[p]=='\r'||s[p]=='\t'))++p;}
    bool str(std::string* out=nullptr){
        if(p>=s.size()||s[p++]!='"')return false;
        if(out)out->clear();
        while(p<s.size()){
            unsigned char c=static_cast<unsigned char>(s[p++]);
            if(c=='"')return true;
            if(c<0x20)return false;
            if(c!='\\'){if(out)*out+=static_cast<char>(c);continue;}
            if(p>=s.size())return false;
            char e=s[p++];
            if(e=='"'||e=='\\'||e=='/'){if(out)*out+=e;}
            else if(e=='b'||e=='f'||e=='n'||e=='r'||e=='t'){if(out)*out+=(e=='b'?'\b':e=='f'?'\f':e=='n'?'\n':e=='r'?'\r':'\t');}
            else if(e=='u'){for(int i=0;i<4;++i){if(p>=s.size()||!std::isxdigit(static_cast<unsigned char>(s[p++])))return false;}}
            else return false;
        }
        return false;
    }
    bool value(){
        ws();if(p>=s.size())return false;
        if(s[p]=='"')return str();
        if(s[p]=='{')return object(nullptr,nullptr,nullptr);
        if(s[p]=='['){++p;ws();if(p<s.size()&&s[p]==']'){++p;return true;}while(true){if(!value())return false;ws();if(p<s.size()&&s[p]==']'){++p;return true;}if(p>=s.size()||s[p++]!=',')return false;}}
        for(const char* x:{"true","false","null"}){std::string t=x;if(s.compare(p,t.size(),t)==0){p+=t.size();return true;}}
        std::size_t b=p;if(s[p]=='-')++p;if(p>=s.size())return false;
        if(s[p]=='0')++p;else{if(s[p]<'1'||s[p]>'9')return false;while(p<s.size()&&std::isdigit(static_cast<unsigned char>(s[p])))++p;}
        if(p<s.size()&&s[p]=='.'){++p;std::size_t b2=p;while(p<s.size()&&std::isdigit(static_cast<unsigned char>(s[p])))++p;if(b2==p)return false;}
        if(p<s.size()&&(s[p]=='e'||s[p]=='E')){++p;if(p<s.size()&&(s[p]=='+'||s[p]=='-'))++p;std::size_t b2=p;while(p<s.size()&&std::isdigit(static_cast<unsigned char>(s[p])))++p;if(b2==p)return false;}
        return p>b;
    }
    bool object(std::string* id,std::string* name,std::string* ver){
        ++p;ws();if(p<s.size()&&s[p]=='}'){++p;return true;}
        std::set<std::string> keys;
        while(p<s.size()){
            std::string key;if(!str(&key)||!keys.insert(key).second)return false;ws();
            if(p>=s.size()||s[p++]!=':')return false;ws();
            if(id&&(key=="id"||key=="name"||key=="version")){
                std::string v;if(!str(&v))return false;
                if(key=="id")*id=v;else if(key=="name")*name=v;else *ver=v;
            }else if(!value())return false;
            ws();if(p<s.size()&&s[p]=='}'){++p;return true;}
            if(p>=s.size()||s[p++]!=',')return false;ws();
        }return false;
    }
public:
    explicit JsonCheck(const std::string& text):s(text){}
    bool parse(std::string& id,std::string& name,std::string& ver){ws();if(p>=s.size()||s[p]!='{'||!object(&id,&name,&ver))return false;ws();return p==s.size();}
};
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
        if (!valid_utf8(path) || !safe_path(path) || !names.insert(path).second) {
            m_error = "Caminho inseguro ou duplicado no pacote."; return false;
        }
        if (path == "manifest.json") manifest = std::move(data);
        if (path == "src/main.dash") main_source = std::move(data);
    }
    if (total != static_cast<std::uint64_t>(file_size)) { m_error = "Existem dados extras no fim do pacote."; return false; }
    if (manifest.empty() || main_source.empty()) {
        m_error = "O pacote precisa de manifest.json e src/main.dash."; return false;
    }
    if (!valid_utf8(manifest) || !valid_utf8(main_source)) {
        m_error = "Manifesto e código DashCode precisam usar UTF-8 válido."; return false;
    }
    JsonCheck json(manifest);
    if (!json.parse(out_info.id, out_info.name, out_info.version) ||
        out_info.id.empty() || out_info.name.empty() || out_info.version.empty()) {
        out_info = {};
        m_error = "Manifesto inválido: informe id, name e version como textos."; return false;
    }
    out_info.main_source = std::move(main_source);
    return true;
}
}
