#include "dash_interpreter.hpp"

#include <algorithm>
#include <cctype>
#include <cstdlib>
#include <sstream>

namespace gdhub::language {
namespace {

std::string trim(const std::string& value) {
    const auto first = value.find_first_not_of(" \t\r");
    if (first == std::string::npos) return {};
    const auto last = value.find_last_not_of(" \t\r");
    return value.substr(first, last - first + 1);
}

bool is_identifier(const std::string& value) {
    if (value.empty() || !(std::isalpha(static_cast<unsigned char>(value[0])) || value[0] == '_'))
        return false;
    return std::all_of(value.begin() + 1, value.end(), [](char c) {
        return std::isalnum(static_cast<unsigned char>(c)) || c == '_';
    });
}

std::string value_to_string(const Value& value) {
    if (const auto* text = std::get_if<std::string>(&value)) return *text;
    if (const auto* number = std::get_if<double>(&value)) {
        std::ostringstream stream;
        stream << *number;
        return stream.str();
    }
    return std::get<bool>(value) ? "true" : "false";
}

bool numeric(const Value& value, double& out) {
    if (const auto* number = std::get_if<double>(&value)) {
        out = *number;
        return true;
    }
    return false;
}

} // namespace

bool DashInterpreter::run(const std::string& source, std::vector<std::string>& output) {
    m_error.clear();
    output.clear();

    std::vector<std::string> lines;
    std::istringstream input(source);
    std::string line;
    while (std::getline(input, line)) {
        line = trim(line);
        if (line.empty() || line.rfind("#", 0) == 0) continue;
        lines.push_back(line);
    }

    Variables variables;
    std::size_t index = 0;
    if (!execute_block(lines, index, variables, output, false)) return false;
    if (index != lines.size()) {
        m_error = "Bloco incompleto: faltou 'end'.";
        return false;
    }
    return true;
}

bool DashInterpreter::evaluate_value(const std::string& raw,
                                     const Variables& variables,
                                     Value& value) {
    const std::string expression = trim(raw);
    if (expression.size() >= 2 && expression.front() == '"' && expression.back() == '"') {
        value = expression.substr(1, expression.size() - 2);
        return true;
    }
    if (expression == "true") { value = true; return true; }
    if (expression == "false") { value = false; return true; }

    char* end = nullptr;
    const double number = std::strtod(expression.c_str(), &end);
    if (!expression.empty() && end == expression.c_str() + expression.size()) {
        value = number;
        return true;
    }

    if (!is_identifier(expression)) {
        m_error = "Valor inválido: " + expression;
        return false;
    }
    const auto found = variables.find(expression);
    if (found == variables.end()) {
        m_error = "Variável não encontrada: " + expression;
        return false;
    }
    value = found->second;
    return true;
}

std::string DashInterpreter::interpolate(const std::string& text,
                                         const Variables& variables) const {
    std::string result;
    for (std::size_t i = 0; i < text.size();) {
        if (text[i] == '{') {
            const auto close = text.find('}', i + 1);
            if (close != std::string::npos) {
                const std::string key = text.substr(i + 1, close - i - 1);
                const auto found = variables.find(key);
                if (found != variables.end()) {
                    result += value_to_string(found->second);
                    i = close + 1;
                    continue;
                }
            }
        }
        result += text[i++];
    }
    return result;
}

bool DashInterpreter::evaluate_condition(const std::string& condition,
                                         const Variables& variables,
                                         bool& result) {
    const char* operators[] = {" >= ", " <= ", " == ", " != ", " > ", " < "};
    for (const char* op : operators) {
        const std::string token(op);
        const auto at = condition.find(token);
        if (at == std::string::npos) continue;

        Value left, right;
        if (!evaluate_value(condition.substr(0, at), variables, left) ||
            !evaluate_value(condition.substr(at + token.size()), variables, right))
            return false;

        const std::string symbol = trim(token);
        double a = 0, b = 0;
        const bool both_numbers = numeric(left, a) && numeric(right, b);
        if (symbol == "==") result = value_to_string(left) == value_to_string(right);
        else if (symbol == "!=") result = value_to_string(left) != value_to_string(right);
        else if (!both_numbers) {
            m_error = "Comparações >, <, >= e <= precisam de números.";
            return false;
        } else if (symbol == ">") result = a > b;
        else if (symbol == "<") result = a < b;
        else if (symbol == ">=") result = a >= b;
        else if (symbol == "<=") result = a <= b;
        return true;
    }
    m_error = "Condição inválida: " + condition;
    return false;
}

bool DashInterpreter::execute_block(const std::vector<std::string>& lines,
                                    std::size_t& index,
                                    Variables& variables,
                                    std::vector<std::string>& output,
                                    bool stop_on_else) {
    while (index < lines.size()) {
        const std::string line = lines[index];
        if (line == "end") { ++index; return true; }
        if (line == "else") return stop_on_else;

        if (line.rfind("say ", 0) == 0) {
            Value value;
            if (!evaluate_value(line.substr(4), variables, value)) return false;
            output.push_back(interpolate(value_to_string(value), variables));
            ++index;
            continue;
        }

        if (line.rfind("make ", 0) == 0 || line.rfind("set ", 0) == 0) {
            const bool make = line.rfind("make ", 0) == 0;
            const std::string rest = trim(line.substr(make ? 5 : 4));
            const auto equal = rest.find('=');
            const auto to = rest.find(" to ");
            const auto split = make ? equal : to;
            if (split == std::string::npos) {
                m_error = "Use: make nome = valor ou set nome to valor.";
                return false;
            }
            const std::string name = trim(rest.substr(0, split));
            const std::string expression = rest.substr(split + (make ? 1 : 4));
            if (!is_identifier(name)) { m_error = "Nome de variável inválido: " + name; return false; }
            Value value;
            if (!evaluate_value(expression, variables, value)) return false;
            if (!make && variables.find(name) == variables.end()) {
                m_error = "Não existe variável chamada: " + name;
                return false;
            }
            variables[name] = value;
            ++index;
            continue;
        }

        if (line.rfind("add ", 0) == 0) {
            const auto to = line.find(" to ", 4);
            if (to == std::string::npos) { m_error = "Use: add valor to variavel."; return false; }
            Value amount;
            if (!evaluate_value(line.substr(4, to - 4), variables, amount)) return false;
            const std::string name = trim(line.substr(to + 4));
            auto found = variables.find(name);
            double a = 0, b = 0;
            if (found == variables.end() || !numeric(found->second, a) || !numeric(amount, b)) {
                m_error = "'add' precisa de uma variável numérica existente e um número.";
                return false;
            }
            found->second = a + b;
            ++index;
            continue;
        }

        if (line.rfind("repeat ", 0) == 0) {
            Value count_value;
            if (!evaluate_value(line.substr(7), variables, count_value)) return false;
            double count_number = 0;
            if (!numeric(count_value, count_number) || count_number < 0 || count_number > 1000) {
                m_error = "repeat aceita um número entre 0 e 1000.";
                return false;
            }
            ++index;
            const std::size_t body_start = index;
            std::size_t after_body = body_start;
            for (int n = 0; n < static_cast<int>(count_number); ++n) {
                std::size_t cursor = body_start;
                if (!execute_block(lines, cursor, variables, output, false)) return false;
                after_body = cursor;
            }
            if (count_number == 0) {
                std::size_t cursor = body_start;
                int depth = 1;
                while (cursor < lines.size() && depth > 0) {
                    if (lines[cursor].rfind("repeat ", 0) == 0 || lines[cursor].rfind("if ", 0) == 0) ++depth;
                    else if (lines[cursor] == "end") --depth;
                    ++cursor;
                }
                if (depth != 0) { m_error = "Bloco repeat sem end."; return false; }
                after_body = cursor;
            }
            index = after_body;
            continue;
        }

        if (line.rfind("if ", 0) == 0) {
            bool condition = false;
            if (!evaluate_condition(line.substr(3), variables, condition)) return false;
            ++index;
            const bool executed = execute_block(lines, index, variables, output, true);
            if (!executed) return false;
            if (index < lines.size() && lines[index] == "else") {
                ++index;
                if (!condition) {
                    if (!execute_block(lines, index, variables, output, false)) return false;
                } else {
                    std::size_t cursor = index;
                    int depth = 1;
                    while (cursor < lines.size() && depth > 0) {
                        if (lines[cursor].rfind("if ", 0) == 0 || lines[cursor].rfind("repeat ", 0) == 0) ++depth;
                        else if (lines[cursor] == "end") --depth;
                        ++cursor;
                    }
                    if (depth != 0) { m_error = "Bloco else sem end."; return false; }
                    index = cursor;
                }
            } else if (condition) {
                // The true branch was executed and consumed its end.
            }
            continue;
        }

        m_error = "Não entendi esta linha: " + line;
        return false;
    }
    return true;
}

} // namespace gdhub::language
