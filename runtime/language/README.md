# DashCode runtime

DashCode is the simple interpreted language used by GD Hub packages.

Implemented in the current prototype:
- variables with text, number and boolean values;
- say output and variable interpolation;
- set and numeric add;
- if/else conditions;
- repeat loops;
- comments;
- source, instruction and output limits.

The current interpreter is a small line-based evaluator, not yet a full lexer/parser/AST language engine. Functions, lists, maps, modules, events, editor actions and the Geometry Dash API are planned but not implemented.

See docs/LANGUAGE.md for the supported syntax and limitations.
