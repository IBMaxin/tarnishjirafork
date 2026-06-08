# Documentation — Tarnish Jira Fork

## Root Files (always available)

| File | Purpose |
|------|---------|
| `AGENTS.md` | Project map, build commands, common patterns — **read first** |
| `README.md` | Repository overview (from upstream) |
| `code_index.json` | Instant file → class name lookup (2,663 entries) |
| `generate_index.sh` | Regenerate code_index.json |
| `rag_index.py` | Semantic code search (FAISS, optional) |

---

## Docs

| Doc | Content |
|-----|---------|
| [game-scope.md](game-scope.md) | Game inventory — activities, skills (18), economy, commands |
| [feature-inventory.md](feature-inventory.md) | Fuller code-grounded inventory of bosses, shops, donor systems, broadcasts, custom systems |
| [improvement-plan.md](improvement-plan.md) | **Single source of truth:** completed work, testing phases, feature integrity test plan, live hardening, go-live gate |
| [test-plan.md](test-plan.md) | Offline test strategy, current baseline, test expansion plan |

### Workflows

Step-by-step recipes for the highest-churn systems. Each is a self-contained "how to add X" guide with real code patterns from this codebase.

→ **Workflows index:** [workflows/README.md](workflows/README.md)

| System | Guide |
|--------|-------|
| Commands | [workflows/commands.md](workflows/commands.md) |
| Items | [workflows/items.md](workflows/items.md) |
| NPCs | [workflows/npcs.md](workflows/npcs.md) |
| Shops | [workflows/shops.md](workflows/shops.md) |
| Plugins | [workflows/plugins.md](workflows/plugins.md) |
| Skills | [workflows/skills.md](workflows/skills.md) |

---

## Prompt Pack

35 self-contained AI agent prompts for audit, test, and development. Located in `prompts/`.

| Section | Prompts |
|---------|---------|
| 00 Cross-Cutting | client-server-boundary |
| 01 Build & Verify | compile, test, server-smoke |
| 02 Data Audit | JSON parse, item consistency, NPC consistency, profile rights |
| 03 Security | command audit, privilege escalation, packet injection, economy risks, config leak |
| 04 Systems | combat, skills, activities, economy, plugins, networking |
| 05 Content Dev | add item, add NPC, add shop, add command, add skill action |
| 06 Client | client overview, client cache, client security |
| 07 Deep Systems | bot system, discord, database, consumables, teleport, dialogue, gambling, clan channel |

Full index: [prompts/README.md](../prompts/README.md)

---

## When to use what

| You want to... | Read... |
|---------------|--------|
| Understand project structure | `AGENTS.md` |
| Find a file/class quickly | `code_index.json` |
| Know what's in the game | `docs/game-scope.md` |
| Add a new command | `docs/workflows/commands.md` |
| Add a new item | `docs/workflows/items.md` |
| Audit security | `prompts/03-security/` |
| Run the server | `AGENTS.md` §Quick Start |
| Write a test | `docs/improvement-plan.md` §Testing Phases |
