# Workflows

Quick-reference guides for the highest-churn systems in this codebase.

→ **Project map:** `AGENTS.md`
→ **Game scope:** [../game-scope.md](../game-scope.md)
→ **Test plan:** [../test-plan.md](../test-plan.md)
→ **Prompt pack:** [../../prompts/README.md](../../prompts/README.md)
→ **Docs index:** [../README.md](../README.md)

Each doc is a **recipe**, not a tutorial — exact files, patterns, and steps.

| System | Doc | What it covers |
|--------|-----|----------------|
| Commands | [commands.md](commands.md) | Adding chat commands by rank |
| Items | [items.md](items.md) | Item definitions, equipment stats, cache data |
| NPCs | [npcs.md](npcs.md) | NPC definitions, spawns, drops, click handlers |
| Shops | [shops.md](shops.md) | Shop definitions, currencies, NPC wiring |
| Plugins | [plugins.md](plugins.md) | Click handlers, item-on-X, container actions |
| Skills | [skills.md](skills.md) | SkillAction pattern, wiring to objects, skill IDs |

## How to use these

1. Identify the system from AGENTS.md
2. Open the matching workflow doc for exact paths and patterns
3. Use `code_index.json` for class lookup
4. Follow the steps, recompile, verify

## Project file discovery

- `code_index.json` — instant file → class name lookup (2,663 entries)
- `AGENTS.md` — full project structure map
- `.cursorrules` — AI tool hints
- `rag_index.py` — semantic code search (optional, needs `pip install sentence-transformers faiss-cpu`)
