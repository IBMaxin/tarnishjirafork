# Prompt Pack — Tarnish Jira Fork

Self-contained AI agent prompts for auditing, testing, and developing against this codebase.

**Usage:** Feed any prompt to an AI agent (Claude Code, OpenCode, Cursor, Hermes). Each prompt is standalone — it includes file paths, commands, code examples, and verification steps. No external knowledge needed.

**Command convention:** Commands are shown for both platforms:
- **Windows:** `.\gradlew.bat :game-server:classes`
- **WSL/Linux:** `./gradlew :game-server:classes`

Inline scripts (bash/python one-liners) use bash — those are executed by the AI agent in WSL.

**Root docs to read first:**
- `AGENTS.md` — project structure map
- `code_index.json` — instant file lookup (2,663 entries)
- `prompts/00-cross-cutting/client-server-boundary.md` — when server changes need client work

---

## Index

### 00 — Cross-Cutting
| # | Prompt | Purpose |
|---|--------|---------|
| 1 | [client-server-boundary.md](00-cross-cutting/client-server-boundary.md) | When server work needs client work |

### 01 — Build & Verify
| # | Prompt | Purpose |
|---|--------|---------|
| 2 | [compile.md](01-build-verify/compile.md) | Compile server + client |
| 3 | [test.md](01-build-verify/test.md) | Run tests, add new tests |
| 4 | [server-smoke.md](01-build-verify/server-smoke.md) | Boot server, verify health |

### 02 — Data Audit
| # | Prompt | Purpose |
|---|--------|---------|
| 5 | [json-parse.md](02-data-audit/json-parse.md) | Parse all JSON data files |
| 6 | [item-consistency.md](02-data-audit/item-consistency.md) | Cross-ref item IDs across files |
| 7 | [npc-consistency.md](02-data-audit/npc-consistency.md) | Cross-ref NPC IDs, spawns, drops |
| 8 | [profile-rights.md](02-data-audit/profile-rights.md) | Validate account ranks |

### 03 — Security
| # | Prompt | Purpose |
|---|--------|---------|
| 9 | [command-audit.md](03-security/command-audit.md) | Audit all commands for privilege gaps |
| 10 | [privilege-escalation.md](03-security/privilege-escalation.md) | Find rank bypass paths |
| 11 | [packet-injection.md](03-security/packet-injection.md) | Validate all packet handlers |
| 12 | [economy-risks.md](03-security/economy-risks.md) | Item generation, dupes, arbitrage |
| 13 | [config-leak.md](03-security/config-leak.md) | Find exposed credentials and keys |

### 04 — Systems
| # | Prompt | Purpose |
|---|--------|---------|
| 14 | [combat.md](04-systems/combat.md) | Combat formulas, equipment, modifiers |
| 15 | [skills.md](04-systems/skills.md) | All 23 skills — implementation audit |
| 16 | [activities.md](04-systems/activities.md) | 15 minigames — complete vs stub |
| 17 | [economy.md](04-systems/economy.md) | Currencies, shops, drops, sinks |
| 18 | [plugins.md](04-systems/plugins.md) | 133 plugins — coverage map |
| 19 | [networking.md](04-systems/networking.md) | Packets, login, rate limits |

### 05 — Content Development
| # | Prompt | Purpose |
|---|--------|---------|
| 20 | [add-item.md](05-content-dev/add-item.md) | Add item: definitions, equipment, cache |
| 21 | [add-npc.md](05-content-dev/add-npc.md) | Add NPC: definition, spawn, drops, handler |
| 22 | [add-shop.md](05-content-dev/add-shop.md) | Add shop: items, currency, NPC wiring |
| 23 | [add-command.md](05-content-dev/add-command.md) | Add command: pattern, security, aliases |
| 24 | [add-skill-action.md](05-content-dev/add-skill-action.md) | Add skill action: class + object wiring |

### 06 — Client
| # | Prompt | Purpose |
|---|--------|---------|
| 25 | [client-overview.md](06-client/client-overview.md) | Client architecture: 27K-line monolith, trust boundary |
| 26 | [client-cache.md](06-client/client-cache.md) | Cache integrity: model/sprite injection, JS5 protocol |
| 27 | [client-security.md](06-client/client-security.md) | Modified client attack surface: packet forgery, validation gaps |

### 07 — Deep Systems
| # | Prompt | Purpose |
|---|--------|---------|
| 28 | [bot-system.md](07-deep-systems/bot-system.md) | PlayerBot: economy impact, abuse vectors, architecture |
| 29 | [discord.md](07-deep-systems/discord.md) | Dual Discord bots: token exposure, command surface |
| 30 | [database.md](07-deep-systems/database.md) | 4 DB services: hardcoded creds, SQL injection, PII logs |
| 31 | [consumables.md](07-deep-systems/consumables.md) | Food + potions: heal amounts, edge cases, overflow |
| 32 | [teleport.md](07-deep-systems/teleport.md) | Teleport safety: wilderness bypass, == bug, teleblock |
| 33 | [dialogue.md](07-deep-systems/dialogue.md) | DialogueFactory: chain integrity, format injection |
| 34 | [gambling.md](07-deep-systems/gambling.md) | Gambling: dupe vectors, state machine, disconnect cleanup |
| 35 | [clan-channel.md](07-deep-systems/clan-channel.md) | Clan system: rank hierarchy, permission enforcement |

---

## Priority order

For a new agent session:
1. **01-build-verify** — gate everything on compile + smoke
2. **03-security** — highest value, catches the most bugs
3. **06-client** — client trust boundary, packet forgery surface
4. **02-data-audit** — static checks, no server needed
5. **04-systems** — deep dives on demand
6. **07-deep-systems** — systems the pack previously missed
7. **05-content-dev** — when adding features

---

## How each prompt is structured

1. **Goal** — what to accomplish
2. **Docs** — which project docs to reference (`AGENTS.md`, `code_index.json`, workflow docs, other prompts)
3. **Files** — exact file paths with real data
4. **Steps** — numbered, with real bash/python commands
5. **Client Impact** — whether this touches `game-client/`, referencing `00-cross-cutting/client-server-boundary.md`
6. **Verify** — checklist to confirm success

Every prompt is executable. Feed it to an AI agent with access to a terminal and it produces results.
