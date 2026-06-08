# Pre-Live Fix Plan

Status: Slayer shop guard and totalPoints fixes are complete and verified. Remaining 6 highest-risk issues pending.

Constraints: `.\\gradlew.bat :game-server:test` must stay green after every phase.

---

## Phase 0 — Plan a clean base

- Lock in live design decisions (see open questions at end).
- Create this file (already done).
- Optionally re-grep `System.out.println` across `game-server/plugins/**` and `game-server/src/main/java/**` to confirm scope of cleanup.

---

## Phase 1 — Highest-Risk Fixes (must land before live)

### [DONE] 1.2 `Slayer.java` — fix the shop slot guard
File: `game-server/src/main/java/com/osroyale/content/skill/impl/slayer/Slayer.java:248`

- Change `if (slot < 0 && slot > STORE_ITEMS.length) {` → `if (slot < 0 || slot >= STORE_ITEMS.length) {`.
- Add JUnit test `SlayerStoreSlotGuardTest` under `game-server/src/test/.../slayer/`:
  - Verify guard rejects `slot = -1`, `slot = 0` (valid), `slot = STORE_ITEMS.length` (invalid), `slot = STORE_ITEMS.length - 1` (valid).
  - Recommend extracting slot-validation to a package-private static method or making the test exercise via reflection on the existing store method. Prefer the static-method extraction for testability.

### [DONE] 1.3 `Slayer.java` — also bump `totalPoints` on natural completion
File: `game-server/src/main/java/com/osroyale/content/skill/impl/slayer/Slayer.java:176`

- Add `totalPoints += rewardPoints;` immediately after `points += rewardPoints;`. Matches field semantics: lifetime points earned.
- Add a unit test that simulates a kill chain that completes a task and asserts `getTotalPoints() > 0`.

### 1.7 `LogEvent.java` — decouple logs from forum integration
File: `game-server/src/main/java/com/osroyale/game/event/impl/log/LogEvent.java:16`

- Change the gate from `!Config.FORUM_INTEGRATION || !Config.LOG_PLAYER` to `!Config.LOG_PLAYER`.
- Add a comment explaining the policy: "Player event logs are independent of forum integration. Forum-related logs (e.g. donation claims) should override `onLog` and re-check forum prerequisites there."
- Add a unit test that asserts a sample `LogEvent` is suppressed when `LOG_PLAYER=false` and proceeds when `LOG_PLAYER=true` (independent of `FORUM_INTEGRATION`).

---

## Phase 2 — Live Hardening (gating + cleanup)

### 2.1 `Config.DEV_COMMANDS` flag for admin/owner commands
Per plan decision, gate dev commands via a config flag.

Files:
- `game-server/src/main/java/com/osroyale/Config.java` — add:
  ```java
  public static final boolean DEV_COMMANDS_ENABLED;
  ```
- `game-server/src/main/java/com/osroyale/Config.java:340` area — bind to `parser.getBoolean("server.dev_commands_enabled")`.
- `game-server/settings.toml` — add under `[server]`: `dev_commands_enabled = false` (live default).
- `game-server/plugins/plugin/command/AdminCommandPlugin.java:358` — change `canAccess` to:
  ```java
  return Config.DEV_COMMANDS_ENABLED && PlayerRight.isAdministrator(player);
  ```
- Same for `OwnerCommandPlugin.java:504` — gate `OWNER` commands. Pragmatic split: keep moderation always-on (`::save`, `::unipmute`, `::ipmute`, `::ipban`, `::ban`, `::unban`, `::resetplayer`, `::doubleexp`, `::wildplayers`, `::settitle`, `::setpt`, `::randomevent`); gate the rest (`::bombs`, `::fight`, `::alltome`, `::giveitem`, `::giveexp`, `::kill`, `::setrank`, `::checkaccs`, `::demote`, `::starterbank`, `::bigbank`, etc.). See open question on granularity.

### 2.2 Remove stray `System.out.println` calls
- Re-grep across `game-server/plugins/**` and `game-server/src/main/java/**` and clean any that remain in plugin/command code paths. Initial grep found zero in `ObjectFirstClickPlugin.java` (the original note may have been from an older read).
- Replace with `logger.debug` or remove outright.

### 2.3 `PlayerRight` isAdministrator / isExtreme / isElite / isKing spillover
File: `game-server/src/main/java/com/osroyale/game/world/entity/mob/player/PlayerRight.java:90-141`

- Confirm whether staff (Admin/Manager/Developer/Owner) intentionally count as donator tiers for drop-rate, presets, blood-money, and deposit-amount checks.
- If unintentional: change `isExtreme`/`isElite`/`isKing` to drop the `isAdministrator` short-circuit. The donator check (`isDonator`) already includes staff via `isModerator` which is a different intent.
- Add unit tests asserting the matrix if changed.

### 2.4 Donation claiming is config-disabled
File: `game-server/settings.toml:15` — `donations_enabled = false`. Confirm whether this is intended for launch (i.e. bonds are in-game rewards only, not real-money claims) and the staff-side `setCredits` and `::points` paths are the only way to mint credits at launch.

### 2.5 Boss entrance gap (Zulrah has no slayer check)
File: `game-server/plugins/plugin/click/object/ObjectFirstClickPlugin.java:821`

- Per inspection note: Vorkath, Kraken, Cerberus gated by slayer, Zulrah not. Confirm whether Zulrah is intentionally free-to-enter. If not, add a slayer task check matching the others. Audit Vorkath's entrance in the same pass.

### 2.6 Fall-through bug at Kraken entrance
File: `game-server/plugins/plugin/click/object/ObjectFirstClickPlugin.java:813-820`

- The Kraken case has no `break;` after the slayer-task-rejection branch. The `else { break; }` is there, but if the player DOES have a Kraken task, the case body runs and then **falls through** to case 10068 (Zulrah). This is a real bug. Add `break;` at the end of the Kraken-success path.

---

## Phase 3 — Go-Live Gate (smoke tests)

Per the go-live gate, with the dev-commands flag plan this becomes:

- [ ] `:game-server:test` green
- [ ] Manual boot smoke: `Loaded: 133 plugins`, `Startup service finished`
- [ ] Port 43594 listening
- [ ] Login as a regular player → staff command denied (`::points` from non-admin returns nothing)
- [ ] Login as a dev account with `dev_commands_enabled = true` → `::points` works
- [ ] Login as a regular player → `::item 4151` does nothing (gated)
- [ ] Donor bond redemption path (after enabling `donations_enabled`): turn in bond, confirm credits
- [ ] Slayer: assign task → cancel (with cost) → reassign → kill count → points increment → `totalPoints` increments
- [ ] Slayer shop: invalid slot returns no crash; valid slot deducts points and adds item
- [ ] Boss entrances: each gated boss blocks without task, allows with task; Zulrah behavior matches the design call
- [ ] Shop buy/sell
- [ ] Trade (with logging entry in `LogEvent` chain)
- [ ] Drop/pickup (logging entry)
- [ ] Logout/restart persistence: position, inventory, bank, slayer task, points, totalPoints

---

## Phase 4 — Documentation

- Update `docs/workflows/commands.md` with: "Adding commands is single-shot in `register()`. Never add commands from inside `execute()` — the multimap is finalized at `onInit()`."
- Update `docs/knowledge-bank.md` (or new `docs/dev-commands.md`) with the `Config.DEV_COMMANDS_ENABLED` flag and live-vs-dev semantics.
- Add a `docs/pre-live-checklist.md` (or extend existing `docs/test-plan.md`) with the smoke-test list above.

---

## Open questions before executing

1. **Owner-command split (2.1):** single `DEV_COMMANDS_ENABLED` flag, or finer split (moderation always-on, items/teleports gated)?
2. **Zulrah slayer check (2.5):** is it intentional that Zulrah doesn't require a task? Also: should I audit Vorkath and any other boss entrances?
3. **PlayerRight staff-as-donator (2.3):** keep current behavior, or drop the staff short-circuit in `isExtreme`/`isElite`/`isKing`?
4. **LogReader bootstrap (1.6):** who creates `backup/logs/referrals.txt` on first run — `LogReader` itself, the donation subsystem, or `Starter`?
