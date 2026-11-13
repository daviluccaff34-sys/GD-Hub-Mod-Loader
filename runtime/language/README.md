# DashFlow runtime

DashFlow is the proposed interpreted language for GD Hub mods.

Planned implementation stages:
1. Lexer: tokenize source files.
2. Parser: validate syntax and build an AST.
3. Runtime values: numbers, strings, booleans, lists and maps.
4. Evaluator: variables, expressions, conditions, loops and functions.
5. Standard library: safe logging, math and mod-owned storage.
6. Event bridge: deliver approved game events to scripts.
7. Permission checks and execution limits.

No interpreter is implemented in this directory yet. See [the language specification](../../docs/LANGUAGE.md).
