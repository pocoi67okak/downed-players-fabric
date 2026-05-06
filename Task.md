# Task Status

## Project Goal

Create a complete Minecraft Java Edition 1.21.8 Fabric mod named **Downed Players / Last Chance**.

The mod should replace immediate vanilla death from lethal damage with a downed state, block all downed-player actions, support surrender, timeout death, server-side revive, loot access to the downed player's full inventory, persistence, client HUD/screen, and GitHub Actions-only builds.

Important constraint: **do not build, test, or run Gradle locally on the user's PC**. All Gradle build/test/check work and `.jar` production must happen through GitHub Actions.

## Fixed Versions

- Minecraft: `1.21.8`
- Yarn mappings: `1.21.8+build.1`
- Fabric API: `0.136.1+1.21.8`
- Fabric Loader: `0.18.4`
- Java: `21`
- Gradle distribution planned in wrapper: `8.14.3`
- Archive base name: `downed-players-lastchance-fabric-1.21.8`
- Current mod version: `4.0.0`

## What Was Completed Before Stop

- Confirmed the workspace was empty and not yet a git repository.
- Looked up current Fabric Maven metadata for Minecraft `1.21.8`.
- Created the base Fabric/Gradle project structure.
- Added Gradle files:
  - `settings.gradle`
  - `build.gradle`
  - `gradle.properties`
  - `gradlew`
  - `gradlew.bat`
  - `gradle/wrapper/gradle-wrapper.properties`
  - `gradle/wrapper/gradle-wrapper.jar`
- Copied an existing Gradle wrapper jar from another local project because direct download from GitHub/Gradle was failing with TLS handshake errors.
- Added repository metadata:
  - `.gitignore`
  - `LICENSE`
- Added Fabric metadata:
  - `src/main/resources/fabric.mod.json`
  - `src/main/resources/downed_players.mixins.json`
  - `src/client/resources/downed_players.client.mixins.json`
- Created package directories:
  - `core/state`
  - `event`
  - `network/payload`
  - `screen/gui`
  - `config`
  - `util`
  - `mixin`
  - client-side entry/mixin/gui packages
- Implemented initial config code:
  - `src/main/java/com/lastchance/downed/config/DownedPlayersConfig.java`
- Implemented initial text helper:
  - `src/main/java/com/lastchance/downed/util/DownedText.java`
- Downloaded and inspected Yarn `1.21.8+build.1` mappings into `.codex-cache/` for local API name lookup only.

## What Was Completed On 2026-05-05 Resume

- Re-read this task file and confirmed the project is a Fabric mod, not a Spigot/Bukkit plugin.
- Confirmed the workspace is still not a git repository.
- Confirmed `rg` currently fails in this environment with `Access is denied`; use PowerShell file search commands instead.
- Downloaded Fabric API source jars into `.codex-cache/fabric-sources/` for local API lookup only:
  - `fabric-lifecycle-events-v1`
  - `fabric-networking-api-v1`
  - `fabric-events-interaction-v0`
  - `fabric-entity-events-v1`
  - `fabric-screen-api-v1`
  - `fabric-screen-handler-api-v1`
  - `fabric-key-binding-api-v1`
- Inspected Fabric API callback signatures for:
  - `ServerTickEvents`
  - `ServerLifecycleEvents`
  - `ClientTickEvents`
  - `ServerPlayConnectionEvents`
  - `ServerPlayNetworking`
  - `ClientPlayNetworking`
  - `PayloadTypeRegistry`
  - `AttackEntityCallback`
  - `UseEntityCallback`
  - `UseBlockCallback`
  - `UseItemCallback`
  - `PlayerBlockBreakEvents`
- Inspected more Yarn names for:
  - `GenericContainerScreenHandler`
  - `ScreenHandler`
  - `ScreenHandlerType`
  - `PlayerInventory`
  - `Inventory`
  - `ItemStack`
  - `ServerPlayerEntity`
  - `ServerPlayNetworkHandler`
  - `ServerPlayerInteractionManager`
  - `ItemEntity`
  - `EntityPose`
  - `ActionResult`
  - `Identifier`
  - `DamageSource`
  - `DamageSources`
- Decided not to use Minecraft `PersistentState` for the first implementation because 1.21.8 moved this API to codec-based `PersistentStateType`, which increases compile risk without a local Gradle check. Use a dedicated server-side JSON state manager saved under Fabric config/server context instead; it still supports logout/restart restoration.
- Planned to use vanilla `GenericContainerScreenHandler.createGeneric9x6(...)` for the downed-player loot UI instead of registering a custom screen handler type.
- Planned networking payloads:
  - `StateSyncPayload` server to client
  - `ReviveProgressPayload` server to client
  - `SurrenderPayload` client to server
  - `ReviveHeartbeatPayload` client to server
- Planned server action flow:
  - lethal damage interception enters downed state and sets health to 1
  - timeout/surrender/finish uses a bypass flag and `DamageSources#genericKill()`
  - right-click on downed player starts revive
  - client heartbeat maintains revive hold
  - shift + right-click opens the 9x6 loot screen
- Planned action blocking:
  - Fabric events for attack/entity use/block use/item use/block break
  - mixins for lethal damage, movement, jump, drop, pickup, hotbar selection, inventory opening, and server movement packets
- No source files were edited during this resume before the user interrupted.

## What Was Completed On 2026-05-05 Continued Implementation

- Added API lookup cache notes in `API_NOTES.md` so future work can avoid repeating slow Yarn/Fabric mapping searches.
- Added main server entrypoint:
  - `src/main/java/com/lastchance/downed/DownedPlayersMod.java`
- Added networking payloads and registration:
  - `StateSyncPayload` server to client
  - `ReviveProgressPayload` server to client
  - `SurrenderPayload` client to server
  - `ReviveHeartbeatPayload` client to server
  - Payloads use `PacketCodec.ofStatic(...)` and `RegistryByteBuf`.
- Added server downed-state implementation:
  - `src/main/java/com/lastchance/downed/core/state/DownedState.java`
  - `src/main/java/com/lastchance/downed/core/state/DownedStateManager.java`
  - JSON persistence at Fabric config path `downed_players_state.json`
  - timeout death
  - surrender death
  - final death bypass flag
  - revive session start/heartbeat/progress/success/cancel
  - server-side movement lock
  - player sync payloads
  - `preserve_downed_state_on_logout=false` clears downed state during disconnect
- Added downed-player loot inventory wrapper:
  - `src/main/java/com/lastchance/downed/screen/gui/DownedLootInventory.java`
  - Uses vanilla `GenericContainerScreenHandler.createGeneric9x6(...)`
  - Supports single-looter lock when `allow_multiple_looters_at_once=false`
- Added server-side action blocking mixins:
  - `EntityDamageMixin`
  - `ItemEntityMixin`
  - `LivingEntityMixin`
  - `PlayerEntityMixin`
  - `ServerPlayNetworkHandlerMixin`
  - `ServerPlayerInteractionManagerMixin`
- Added client implementation:
  - `src/client/java/com/lastchance/downed/client/DownedPlayersClient.java`
  - `ClientPlayerEntityMixin`
  - `InGameHudMixin`
  - Client HUD for downed timer/surrender prompt/revive progress
  - Surrender key default `R`
  - Hold-use revive heartbeat while targeting another player
- Updated client mixin config to include `InGameHudMixin`.
- Added GitHub Actions build workflow:
  - `.github/workflows/build.yml`
  - Java 21
  - Gradle setup action
  - `./gradlew build`
  - uploads `build/libs/*.jar` as `downed-players-lastchance-fabric-1.21.8`
- Added README:
  - `README.md`
  - usage, config, controls, Actions artifact instructions, limitations
- Initialized a local git repository.
- Created initial commit:
  - `790aed6 Initial Fabric downed players mod`
- Created follow-up status commit:
  - `032acd5 Update task status after initial commit`
- Created GitHub repository:
  - `https://github.com/pocoi67okak/downed-players-fabric`
- Renamed local branch to `main`.
- Added remote:
  - `origin https://github.com/pocoi67okak/downed-players-fabric.git`
- Pushed `main` to GitHub.
- GitHub Actions build started automatically from push:
  - run id `25377670058`
  - job id `74417064260`
- First GitHub Actions build failed during the `Build` step:
  - exit code `126`
  - likely next thing to inspect is executable permission / line endings for `gradlew` on Linux.
  - workflow also emitted a Node.js 20 deprecation warning for GitHub actions, but that was not the build failure.
- Fixed GitHub Actions build failures:
  - `586d7af Make Gradle wrapper executable` fixed Linux `./gradlew: Permission denied`.
  - `8bcec06 Use Gradle 8 compatible Fabric Loom` changed Fabric Loom from `1.16.1` to `1.13.6` because `1.16.1` requires Gradle plugin API `9.4.0`, while the wrapper is Gradle `8.14.3`.
  - `e3c6ad2 Allow Loom to add remap repositories` removed strict `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, which blocked Loom's own remap repositories.
  - `535240b Invoke entity dimension refresh through mixin` replaced direct protected `Entity#reinitDimensions()` calls with a Mixin invoker.
- Confirmed GitHub Actions build succeeded:
  - run id `25378248828`
  - URL `https://github.com/pocoi67okak/downed-players-fabric/actions/runs/25378248828`
  - head SHA `535240b1a8411e0fa31a98b6483bed0e72fad49e`
- Confirmed uploaded Actions artifact:
  - name `downed-players-lastchance-fabric-1.21.8`
  - artifact id `6807136577`
  - digest `sha256:4303aaf35c69fa45e7e9ae70bbec9ea966ae1dfaa2a1bebd9540b880b566d7d3`
  - expires `2026-08-03T13:07:47Z`
- Adjusted the Fabric Loader requirement to match the tested ElyPrismLauncher instance:
  - `gradle.properties` `loader_version=0.18.4`
  - `fabric.mod.json` `fabricloader >=0.18.4`
  - This addresses the launcher error where Fabric Loader `0.18.4` rejected the mod because it required `>=0.19.2`.
- Changed revive progress to be server-timed once started instead of requiring continuous client heartbeat packets:
  - This avoids revive cancellation when the client loses crosshair `EntityHitResult` on a downed/sleeping player.
  - Bumped `mod_version` to `2.0.0`.
- Confirmed GitHub Actions build succeeded after the revive/version change:
  - commit `927c03f Make revive progress server timed`
  - run id `25380059174`
  - URL `https://github.com/pocoi67okak/downed-players-fabric/actions/runs/25380059174`
  - head SHA `927c03f0e93db96379cdf11cb07f4ce09aa71706`
- Confirmed uploaded `2.0.0` Actions artifact:
  - name `downed-players-lastchance-fabric-1.21.8`
  - expected jar name `downed-players-lastchance-fabric-1.21.8-2.0.0.jar`
  - artifact id `6807918058`
  - digest `sha256:1e0aab835bbdbf8b8784c5e41ce7824659d84e36a1e73152acf6730f563f9447`
  - expires `2026-08-03T13:43:06Z`

## Current Stop Point

Core implementation files now exist, are pushed to GitHub, and GitHub Actions builds successfully at mod version `2.0.0`.

The latest practical stop point is runtime QA of artifact `downed-players-lastchance-fabric-1.21.8-2.0.0.jar` in the user's ElyPrismLauncher Fabric 1.21.8 instance.

No local Gradle build was run.

The local git repository exists, branch `main` tracks `origin/main`, and the GitHub repository exists at `https://github.com/pocoi67okak/downed-players-fabric`.

GitHub Actions run `25380059174` completed successfully and uploaded the `2.0.0` mod artifact.

## Important Implementation Notes Already Discovered

- Player pose methods exist in Yarn `1.21.8+build.1`:
  - `Entity#setPose(EntityPose)`
  - `Entity#getPose()`
  - `Entity#reinitDimensions()`
- `PlayerInventory` in 1.21.8 has changed equipment storage compared with older versions:
  - main stacks are available through `getMainStacks()`
  - selected slot methods include `getSelectedSlot()` and `setSelectedSlot(int)`
  - equipment is represented through the newer equipment API, so armor/offhand inventory access needs careful implementation.
- `DamageSources` includes:
  - `generic()`
  - `genericKill()`
  - `playerAttack(PlayerEntity)`
  - other vanilla damage source factories.
- `Entity#damage(ServerWorld, DamageSource, float)` is the mapped damage entrypoint for this version.
- More detailed Yarn/Fabric notes are now in `API_NOTES.md`.

## Remaining Work

1. Install/test artifact `downed-players-lastchance-fabric-1.21.8-2.0.0.jar` from GitHub Actions run `25380059174`.
2. Verify in-game:
   - lethal damage enters downed state instead of vanilla death
   - reviver right-click starts revive
   - revive progress fills without requiring perfect crosshair heartbeat
   - revived player stands up with configured health
3. Address any new runtime crash or gameplay bug with follow-up commits and GitHub Actions builds.

## Local Build Rule

Do not run:

```bash
./gradlew build
gradle build
./gradlew test
./gradlew check
```

All such checks must be done by GitHub Actions only.
