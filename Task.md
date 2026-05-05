# Task Status

## Project Goal

Create a complete Minecraft Java Edition 1.21.8 Fabric mod named **Downed Players / Last Chance**.

The mod should replace immediate vanilla death from lethal damage with a downed state, block all downed-player actions, support surrender, timeout death, server-side revive, loot access to the downed player's full inventory, persistence, client HUD/screen, and GitHub Actions-only builds.

Important constraint: **do not build, test, or run Gradle locally on the user's PC**. All Gradle build/test/check work and `.jar` production must happen through GitHub Actions.

## Fixed Versions

- Minecraft: `1.21.8`
- Yarn mappings: `1.21.8+build.1`
- Fabric API: `0.136.1+1.21.8`
- Fabric Loader: `0.19.2`
- Java: `21`
- Gradle distribution planned in wrapper: `8.14.3`
- Archive base name: `downed-players-fabric-1.21.8`

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
  - uploads `build/libs/*.jar` as `downed-players-fabric-1.21.8`
- Added README:
  - `README.md`
  - usage, config, controls, Actions artifact instructions, limitations

## Current Stop Point

Core implementation files now exist. Work stopped before GitHub Actions compilation, so there may still be compile errors that must be discovered by a GitHub Actions run, not local Gradle.

The next practical step is to initialize git, commit the current implementation, create/push the GitHub repository, start the Actions build, then fix any compile failures by follow-up commits.

No local Gradle build was run.

No GitHub repository was initialized or pushed yet.

No GitHub Actions run has been started yet.

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

1. Do a final source-level sanity pass without local Gradle.
2. Initialize git repository.
3. Create the GitHub repository.
4. Push the branch.
5. Start GitHub Actions build.
6. Fix any GitHub Actions compile errors by committing and pushing follow-up fixes until the workflow succeeds.
7. Download/confirm the uploaded Actions artifact once the workflow is green.

## Local Build Rule

Do not run:

```bash
./gradlew build
gradle build
./gradlew test
./gradlew check
```

All such checks must be done by GitHub Actions only.
