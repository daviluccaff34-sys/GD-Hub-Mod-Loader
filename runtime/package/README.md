# GDHB reader

The v1 reader validates the GDHB container header, entry bounds, safe relative paths, duplicates, required manifest fields, and the main DashCode source. It does not extract files to disk. It returns source text in memory for the runtime.

Current limits: 16 MiB per package, 4 MiB per entry, 128 entries, and 240-byte paths. The manifest field reader is intentionally small and is not a complete JSON parser.
