# Contributing

Thank you for your interest in contributing to Bubbles!

## Getting Started

1. Fork the repository
2. Clone your fork
3. Create a feature branch: `git checkout -b feature/your-feature`
4. Make your changes
5. Test on device/emulator
6. Commit with a descriptive message
7. Push and open a Pull Request

## Branch Naming

| Prefix | Use Case |
|--------|----------|
| `feature/` | New features |
| `fix/` | Bug fixes |
| `refactor/` | Code refactoring |
| `docs/` | Documentation changes |
| `ui/` | UI/UX improvements |

## Code Style

### Kotlin

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `camelCase` for functions and variables
- Use `PascalCase` for classes and objects
- Prefer `val` over `var`
- Use trailing lambda syntax
- Keep functions under 50 lines when possible

### Compose

- Extract reusable composables into separate files
- Use `Modifier` as the first optional parameter
- Prefix preview functions with `@Preview`
- Use `StateFlow` for state, not `mutableStateOf` in ViewModels
- Prefer `derivedStateOf` for computed state

### Architecture

- **Repositories** go in `domain/repository/` (interfaces) and `data/local/` (implementations)
- **Use cases** go in `domain/usecase/`
- **UI state** lives in `GameViewModel`, exposed via `StateFlow`
- **No business logic in composables** — keep UI pure

## Commit Messages

Use conventional commits:

```
feat: add new bubble type
fix: resolve crash on level transition
refactor: extract sound manager to separate class
docs: update architecture guide
ui: improve glass surface rendering
```

## Testing

- Write unit tests for use cases and repositories
- Write UI tests for critical user flows
- Test on minimum SDK (API 26) emulator
- Verify reduced-motion mode works

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Pull Request Guidelines

1. **One feature per PR** — keep changes focused
2. **Describe what and why** — not just what changed
3. **Include screenshots** — for UI changes
4. **Update docs** — if changing architecture or adding features
5. **No formatting-only changes** — unless part of a larger refactor

## Design System

When adding UI components:

- Use tokens from `LuxuryTokens.kt` — never hardcode colors
- Use `GlassSurface` for cards — not plain `Surface`
- Use `LuxuryTypography` text styles — not raw `TextStyle`
- Use `LuxuryMotion` springs — not hardcoded durations
- Add haptic feedback via `LuxuryHaptics` for interactions

## Questions?

Open a GitHub Discussion or reach out on the issue tracker.
