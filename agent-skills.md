# Agent Skills — Workflow Rules

This file configures AI coding agents (Codebuff, Claude Code, Cursor, etc.) working on the **Bubbles** project. Skills are packaged instructions that extend agent capabilities.

> **Skills** are located in [`skills/`](./skills/). Each skill lives at `skills/<skill-name>/SKILL.md`.

---

## Core Rules

- If a task matches a skill, you MUST invoke it
- Skills are located in `skills/<skill-name>/SKILL.md`
- Never implement directly if a skill applies
- Always follow the skill instructions exactly (do not partially apply them)

---

## Intent → Skill Mapping

The agent should automatically map user intent to skills:

| Intent | Skill(s) |
|--------|----------|
| Feature / new functionality | `spec-driven-development`, then `incremental-implementation`, `test-driven-development` |
| Planning / breakdown | `planning-and-task-breakdown` |
| Bug / failure / unexpected behavior | `debugging-and-error-recovery` |
| Code review | `code-review-and-quality` |
| Refactoring / simplification | `code-simplification` |
| API or interface design | `api-and-interface-design` |
| UI work | `frontend-ui-engineering` |
| Git operations | `git-workflow-and-versioning` |
| Shipping / releasing | `shipping-and-launch` |
| Performance issues | `performance-optimization` |
| Security concerns | `security-and-hardening` |
| Documentation | `documentation-and-adrs` |

---

## Lifecycle Mapping (Implicit Commands)

The agent must internally follow this lifecycle:

| Phase | Skill |
|-------|-------|
| DEFINE | `spec-driven-development` |
| PLAN | `planning-and-task-breakdown` |
| BUILD | `incremental-implementation` + `test-driven-development` |
| VERIFY | `debugging-and-error-recovery` |
| REVIEW | `code-review-and-quality` |
| SHIP | `shipping-and-launch` |

---

## Execution Model

For every request:

1. Determine if any skill applies (even 1% chance)
2. Invoke the appropriate skill using the `skill` tool
3. Follow the skill workflow strictly
4. Only proceed to implementation after required steps (spec, plan, etc.) are complete

---

## Anti-Rationalization

The following thoughts are incorrect and must be ignored:

- "This is too small for a skill"
- "I can just quickly implement this"
- "I'll gather context first"

Correct behavior:

- Always check for and use skills first

---

## Design System Enforcement

When working on UI, these rules are **mandatory**:

| Rule | Enforcement |
|------|-------------|
| No hardcoded colors | Use `LuxuryTokens.kt` |
| No hardcoded typography | Use `LuxuryTypography.kt` |
| No plain `Surface` cards | Use `GlassSurface` |
| No emoji icons | Use `LuxuryIcons.kt` vector paths |
| No inline animations | Use `LuxuryMotion.kt` springs |
| No inline haptics | Use `SoundManager` patterns |

This ensures all agents behave consistently with full workflow enforcement.
