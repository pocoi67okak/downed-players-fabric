# Downed Players / Last Chance

Fabric mod for Minecraft Java Edition 1.21.8. Lethal damage puts players into a downed state instead of killing them immediately.

## Features

- Lethal damage interception with a configurable downed timer.
- Downed players are locked in place and blocked from attacking, using items, breaking blocks, opening inventories, dropping items, picking up items, and changing hotbar slots.
- Other players can hold right click on a downed player to revive them.
- Shift + right click opens a 9x6 loot view for the downed player's inventory.
- Optional finishing, surrender, timeout death, chat messages, and persisted downed state.
- Client HUD for downed timer, surrender prompt, and revive progress.

## Configuration

The first server start writes `config/downed_players.json`.

Important options:

- `downed_duration_seconds`: Time before a downed player dies.
- `revive_duration_seconds`: Time a reviver must hold the revive.
- `revive_distance_blocks`: Maximum revive range.
- `loot_open_distance_blocks`: Maximum loot-open range.
- `hp_after_revive`: Health restored after revive.
- `allow_full_inventory_looting`: Enables Shift + right click loot access.
- `allow_multiple_looters_at_once`: Allows multiple players to loot the same downed player at once.
- `allow_downed_player_to_take_damage`: Lets downed players receive normal damage.
- `allow_finishing_downed_player`: Lets attacks finish downed players.
- `enable_surrender_button`: Enables the client surrender key.
- `preserve_downed_state_on_logout`: Keeps downed state while offline.
- `preserve_downed_state_on_server_restart`: Saves downed state to `config/downed_players_state.json`.

## Controls

- Hold right click on a downed player: Revive.
- Shift + right click on a downed player: Loot inventory.
- Press `R` while downed: Surrender, if enabled.

## Building

This project is intended to build only in GitHub Actions. Do not run Gradle locally on the user's PC.

Use the `Build` workflow, then download the `downed-players-fabric-1.21.8` artifact from the workflow run. The mod jar is uploaded from `build/libs/*.jar`.

## Known Limitations

The server enforces the downed state. The visual lying pose uses a vanilla pose fallback and may not render exactly like a custom animation on every client.
