# Modernisation Plan — Tarnish Jira Fork

**Status:** Phase 1 complete. Phases 2–5 pending.
**Last updated:** 2026-06-08
**See also:** [improvement-plan.md](improvement-plan.md) (master tracking doc)

---

## Phase 1 — Setup & Onboarding ✅ (Completed 2026-06-08)

| Step | What | Files | Status |
|------|------|-------|--------|
| 1.1 | Simplify `settings.toml` — inline comments, RSA keys documented, prod-only clutter stripped | `game-server/settings.toml` | ✅ |
| 1.2 | Beginner-friendly README — prerequisites, build/run/verify, troubleshooting | `README.md` | ✅ |
| 1.3 | Visual setup guide with screenshots | `SETUP.md` | ✅ |
| 1.4 | Quick-start scripts with Java pre-flight check (version ≥17) | `quickstart.bat`, `quickstart.sh` | ✅ |
| 1.5 | VS Code configs (launch, tasks, extensions) | `.vscode/` | ✅ |

---

## Phase 2 — Code Quality & AI-Friendliness ⌛

| Step | What | Files | Status |
|------|------|-------|--------|
| 2.1 | Remove unused deps (jgroups, pf4j, sentry, joda-time, log4j-core, hamcrest) | `gradle/libs.versions.toml`, both `build.gradle.kts` | ⬜ |
| 2.2 | Replace Joda-Time with `java.time` | `Starter.java`, `DoubleExperienceJob.java` | ⬜ |
| 2.3 | Add `package-info.java` to 6–8 major packages | `content/`, `game/`, `game/plugin/`, etc. | ⬜ |
| 2.4 | Javadoc PluginContext 13-branch dispatch | `PluginContext.java` | ⬜ |
| 2.5 | Javadoc PluginManager classgraph pattern | `PluginManager.java` | ⬜ |
| 2.6 | Javadoc CommandExtension | `CommandExtension.java` | ⬜ |

## Phase 3 — Developer Experience ⌛

| Step | What | Files | Status |
|------|------|-------|--------|
| 3.1 | `LocalSettingsTest` — local dev safety assertions | `LocalSettingsTest.java` | ⬜ |
| 3.2 | Gradle continuous build / `runDev` task | `game-server/build.gradle.kts` | ⬜ |
| 3.3 | Spotless/Checkstyle (non-blocking, legacy exclusions) | `game-server/build.gradle.kts`, `config/checkstyle/` | ⬜ |
| 3.4 | Comment `gradle.properties` | `gradle.properties` | ⬜ |
| 3.5 | Add `.editorconfig` | `.editorconfig` | ⬜ |

## Phase 4 — Documentation ⌛

| Step | What | Files | Status |
|------|------|-------|--------|
| 4.1 | Architecture overview in knowledge-bank.md | `docs/knowledge-bank.md` | ⬜ |
| 4.2 | Architecture doc with Mermaid diagrams | `docs/architecture.md` | ⬜ |
| 4.3 | Glossary (RSPS + project terms) | `docs/glossary.md` | ⬜ |

## Phase 5 — Future Preparation (docs only) ⌛

| Step | What | Files | Status |
|------|------|-------|--------|
| 5.1 | Document PluginContext map-dispatch target | `docs/workflows/plugins.md` | ⬜ |
| 5.2 | Client JDK 11 → 21 upgrade | JDK 21 toolchain in `game-client/build.gradle.kts` | ✅ |
| 5.3 | Docker plan | `docs/docker-plan.md` | ⬜ |

---

## Key Decisions

- **Scope:** Quick wins only. Deep refactors (PluginContext dispatch, client JDK upgrade, Docker) are documented but deferred.
- **Target audience:** Absolute beginners. Every step must be explained.
- **Client JDK:** Upgraded to JDK 21 (matches server). No compatibility issues found.
- **Dual data system:** Already documented in knowledge-bank.md. No changes needed.

## Dependencies

- 1.1 (settings) → 1.2 (README) → 1.4 (quickstart scripts)
- All other steps within Phases 1–5 are independent

## Verification

1. `.\gradlew.bat :game-server:classes` passes
2. `.\gradlew.bat :game-server:test` — 106+ tests, 0 failures
3. Server boots: `Startup service finished`, `Loaded: 133 plugins`, `Server built successfully`
4. Quickstart script: Java absent → clear error; Java 21 → server starts