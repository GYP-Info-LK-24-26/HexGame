# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew run          # Start the game (runs :ui:run)
./gradlew build        # Compile all modules
./gradlew test         # Run tests across all modules
./gradlew :ui:run      # Run only the UI client
./gradlew :logic:test  # Run tests for a single module
```

## Project Architecture

HexGame is a Java implementation of the Hex board game — a multi-module Gradle 8.14 project (Kotlin DSL settings, Groovy build files).

### Modules

- **logic** — Core game engine: board state, rules, player interface, event system (GameObserver, PlayerMoveListener, PlayerWinListener). No external dependencies. All other modules depend on this.
- **ui** — OpenGL client built on IgelEngine. Contains GUI screens (MainGUI, PlayGUI, SettingsGUI, ConnectGUI, HostGUI, WinGUI) and networking (HexClient/HexServer using Netty). Entry point: `de.igelstudios.ClientMain`, engine init: `de.hexgame.ui.Initializer`.
- **algorithm** — AI players using Minimax with Alpha-Beta pruning. Includes transposition table and move ordering. Depends on logic.
- **nn** — TensorFlow-based AI using Monte Carlo Tree Search (MCTS). CNNPlayer runs 400 simulations per move. Includes training pipeline (Trainer, ExperienceBuffer). Depends on logic.
- **ui:IgelEngine** — Git submodule. Custom OpenGL game engine (LWJGL). Provides rendering, GUI widgets, input handling, audio, and networking infrastructure.

### Key Patterns

- **Player interface** (`logic`): Strategy pattern — implemented by RandomPlayer, AlgorithmPlayer, CNNPlayer, RemotePlayer, UIPlayer
- **Dynamic player loading**: `playerClasses.txt` in ui resources lists available player classes
- **Threading**: Game logic runs in its own thread; UI/rendering on the main thread. ClientEngine has task queues for thread-safe OpenGL operations.
- **Observer pattern**: Game events dispatched via listener interfaces

### Build Convention

`buildSrc/src/main/kotlin/buildlogic.java-conventions.gradle.kts` applies to all modules:
- Java toolchain: version 21
- Lombok 1.18.38
- Logback for logging
- JUnit 5

Version properties are in `ui/gradle.properties` (lwjgl, netty, joml, gson versions).

### IgelEngine Submodule

`ui/IgelEngine` is a git submodule. Clone with `--recursive`. The submodule has its own build.gradle with GraalVM native-image support and cross-platform LWJGL native bindings.

## Language

The codebase and README are in German. Localization files: `ui/src/main/resources/lang/en_us.json` and `de_de.json`.
