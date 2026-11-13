#pragma once

#include <string>
#include <cstddef>
#include <vector>
#include <unordered_map>
#include <variant>

namespace gdhub::language {

using Value = std::variant<double, bool, std::string>;

class DashInterpreter {
public:
    // Executes a DashCode source string and collects output from 'say'.
    bool run(const std::string& source, std::vector<std::string>& output);

    const std::string& last_error() const { return m_error; }

private:
    using Variables = std::unordered_map<std::string, Value>;

    bool execute_block(const std::vector<std::string>& lines,
                       std::size_t& index,
                       Variables& variables,
                       std::vector<std::string>& output,
                       std::size_t& steps,
                       bool stop_on_else,
                       bool expect_end);

    bool evaluate_condition(const std::string& condition,
                            const Variables& variables,
                            bool& result);

    bool evaluate_value(const std::string& expression,
                        const Variables& variables,
                        Value& value);

    std::string interpolate(const std::string& text,
                            const Variables& variables) const;

    std::string m_error;
};

}
