# API Notes for Minecraft/Fabric 1.21.8

This file records the local Yarn/Fabric API lookups already done for this project so future work does not repeat slow mapping searches.

## Version Set

- Minecraft: `1.21.8`
- Yarn mappings: `1.21.8+build.1`
- Fabric API: `0.136.1+1.21.8`
- Fabric Loader: `0.18.4`
- Java: `21`
- Gradle wrapper target: `8.14.3`

## Local Reference Cache

- Yarn mappings: `.codex-cache/mappings/mappings.tiny`
- Yarn jar: `.codex-cache/yarn-1.21.8+build.1-v2.jar`
- Fabric API source jars: `.codex-cache/fabric-sources/`
- Extracted Fabric API sources: `.codex-cache/fabric-src-extract/`

`rg` currently fails in this environment with `Access is denied`; use PowerShell `Select-String` / `Get-ChildItem` for local searches.

## Networking

Named classes from mappings:

- `net.minecraft.network.packet.CustomPayload` = intermediary `class_8710`
- `CustomPayload.Id<T>` = intermediary `class_8710$class_9154`
- `net.minecraft.network.RegistryByteBuf` = intermediary `class_9129`
- `net.minecraft.network.PacketByteBuf` = intermediary `class_2540`
- `net.minecraft.network.codec.PacketCodec` = intermediary `class_9139`

Fabric API:

- `PayloadTypeRegistry.playS2C().register(CustomPayload.Id<T>, PacketCodec<? super RegistryByteBuf, T>)`
- `PayloadTypeRegistry.playC2S().register(CustomPayload.Id<T>, PacketCodec<? super RegistryByteBuf, T>)`
- `ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> ...)`
- `ServerPlayNetworking.send(ServerPlayerEntity, payload)`
- `ServerPlayNetworking.canSend(ServerPlayerEntity, CustomPayload.Id<?>)`
- `ClientPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> ...)`
- `ClientPlayNetworking.send(payload)`
- `ClientPlayNetworking.canSend(CustomPayload.Id<?>)`

Packet codec notes:

- `PacketCodec.ofStatic(Writer, Reader)` is available.
- `RegistryByteBuf` supports `readBoolean/writeBoolean`, `readVarLong/writeVarLong`, `readUuid/writeUuid`, and inherits standard byte buf methods such as `readFloat/writeFloat`.
- Empty C2S action packets can use a record with `PacketCodec.ofStatic(...)`; the reader returns a new payload instance and the writer is empty. Existing examples: `SurrenderPayload`, `HandsUpTogglePayload`.

## Fabric Events

Server lifecycle/tick:

- `ServerLifecycleEvents.SERVER_STARTED.register(server -> ...)`
- `ServerLifecycleEvents.SERVER_STOPPING.register(server -> ...)`
- `ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> ...)`
- `ServerTickEvents.END_SERVER_TICK.register(server -> ...)`

Server play connection:

- `ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ...)`
- `ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ...)`
- `ServerPlayNetworkHandler#player` is named `player` from `field_14140`.

Player interaction callbacks:

- `AttackEntityCallback`: `(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) -> ActionResult`
- `UseEntityCallback`: same parameter shape as attack.
- `UseBlockCallback`: `(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) -> ActionResult`
- `UseItemCallback`: `(PlayerEntity player, World world, Hand hand) -> ActionResult`
- `PlayerBlockBreakEvents.BEFORE`: `(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) -> boolean`

`ActionResult` named constants confirmed:

- `ActionResult.PASS`
- `ActionResult.FAIL`
- `ActionResult.SUCCESS`
- `ActionResult.CONSUME`

## Entity and Player Methods

Damage:

- `Entity#damage(ServerWorld, DamageSource, float): boolean`
- `DamageSource#getName(): String`
- `DamageSources#genericKill()` exists via `player.getDamageSources().genericKill()`

Health and pose:

- `LivingEntity#getHealth()`
- `LivingEntity#setHealth(float)`
- `Entity#setPose(EntityPose)`
- `Entity#reinitDimensions()`
- `EntityPose.STANDING`
- `EntityPose.SLEEPING`

Movement:

- `Entity#setVelocity(Vec3d)`
- `Entity#velocityModified` is named and writable.
- `Entity#fallDistance` is named and writable.
- `Entity#setSprinting(boolean)`
- `Entity#squaredDistanceTo(Entity)`
- `LivingEntity#jump()` is named `jump` and mapped from `method_6043`.

Player:

- `PlayerEntity#getInventory(): PlayerInventory`
- `PlayerEntity#openHandledScreen(NamedScreenHandlerFactory): OptionalInt`
- `PlayerEntity#closeHandledScreen()` exists. On `ServerPlayerEntity` it is public and closes the server/client handled screen pair.
- `PlayerEntity#currentScreenHandler` is public and can be inspected server-side to detect whether a player is currently viewing the mod's loot handler.
- `PlayerEntity#dropItem(ItemStack, boolean): ItemEntity`
- `PlayerEntity#dropSelectedItem(boolean): boolean`
- `PlayerEntity#isCreative()`
- `PlayerEntity#isSpectator()`
- `PlayerEntity#isSneaking()`

Player inventory:

- `PlayerInventory#getMainStacks()`
- `PlayerInventory#getSelectedSlot()`
- `PlayerInventory#setSelectedSlot(int)`
- `PlayerInventory#offerOrDrop(ItemStack)`
- `PlayerInventory` implements `Inventory`; prefer `getInventory().size()/getStack()/setStack()/removeStack()` for broad loot access.

## Items and Recipes

Item registration:

- `RegistryKey.of(RegistryKeys.ITEM, Identifier)` creates the item registry key.
- `new Item.Settings().registryKey(key)` is required for 1.21.8 item construction; constructing an item without a registry key can trip intrusive-holder validation.
- `Registry.register(Registries.ITEM, key, item)` registers the item.
- `ItemStack#isOf(Item)` is available for held-item checks.

Static item group constants in `ItemGroups` are private in 1.21.8 mappings, so avoid relying on direct `ItemGroups.TOOLS` style access unless a later lookup confirms a public API path.

Recipe data:

- Shaped recipe files live under `src/main/resources/data/<modid>/recipe/*.json` for this project/version.
- Recipe result uses `id`, not older `item`, e.g. `{ "id": "downed_players:detector", "count": 1 }`.

Item model/resource data:

- 1.21.8 uses item definition JSON under `assets/<modid>/items/<item>.json`.
- The item definition can point at a model: `{ "model": { "type": "minecraft:model", "model": "<modid>:item/<item>" } }`.
- The model remains under `assets/<modid>/models/item/<item>.json` and can use `parent: "minecraft:item/generated"` with `textures.layer0`.
- Texture path for a custom 16x16 item PNG is `assets/<modid>/textures/item/<item>.png`.

## Containers

Vanilla generic 9x6 inventory screen:

- `GenericContainerScreenHandler.createGeneric9x6(int syncId, PlayerInventory playerInventory, Inventory inventory)`
- `SimpleNamedScreenHandlerFactory(ScreenHandlerFactory, Text)`
- `ScreenHandlerFactory#createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player)`
- `NamedScreenHandlerFactory#getDisplayName()`

This project uses vanilla `GENERIC_9X6`; no custom screen handler type is currently needed.

## Client

Client entrypoint:

- `ClientModInitializer#onInitializeClient()`

Client tick:

- `ClientTickEvents.END_CLIENT_TICK.register(MinecraftClient -> ...)`

Key binding:

- `KeyBinding(String translationKey, InputUtil.Type type, int code, String category)`
- `KeyBinding#wasPressed()`
- `KeyBinding#isPressed()`
- `KeyBinding#matchesKey(int keyCode, int scanCode)` is available and useful from `Screen#keyPressed(...)` mixins because `wasPressed()` is normally handled during client ticks, not while a handled screen consumes keyboard events.
- `KeyBindingHelper.registerKeyBinding(KeyBinding)`

Screen close/key mixin:

- `Screen#keyPressed(int keyCode, int scanCode, int modifiers): boolean` is a client mixin target.
- `MinecraftClient#currentScreen` is available client-side.
- `ClientPlayerEntity#closeHandledScreen()` is public and sends the normal close-screen flow from the client.
- `HandledScreen<?>` can be used to restrict a custom keybind to inventory/container screens.

HUD mixin target:

- `InGameHud#render(DrawContext, RenderTickCounter)`
- `DrawContext#getScaledWindowWidth()`
- `DrawContext#getScaledWindowHeight()`
- `DrawContext#fill(int x1, int y1, int x2, int y2, int color)`
- `DrawContext#drawTextWithShadow(TextRenderer, Text, int x, int y, int color)`

Client crosshair/revive heartbeat:

- `MinecraftClient#crosshairTarget`
- `EntityHitResult#getEntity()`
- `MinecraftClient#options.useKey`

## Detector / Hands-Up Flow

Current implementation notes for version `5.0.0`:

- `H` sends `HandsUpTogglePayload` C2S.
- The server stores raised-hands state in an in-memory `Set<UUID>`; it is intentionally not persisted across restart.
- If a player lowers hands or becomes downed, any open `DownedLootScreenHandler` viewing that player is closed server-side.
- A player holding `ModItems.DETECTOR` can right click another non-downed player with raised hands to open the same `DownedLootScreenHandler` used for downed-player looting.
- Detector access reuses `loot_open_distance_blocks` and `allow_multiple_looters_at_once`.
- Detector access does not require `allow_full_inventory_looting`; that config remains scoped to downed-player Shift + right click looting.
- There is no arm-raise animation yet. The server state is authoritative, and the only player-facing feedback is the actionbar chat message when status messages are enabled.

## Current Design Decisions

- Do not use Minecraft `PersistentState` yet; 1.21.8 moved toward codec-based `PersistentStateType`, which is higher compile risk without local Gradle checks.
- Persist downed state as JSON in Fabric config dir: `config/downed_players_state.json`.
- Use manual `PacketCodec.ofStatic(...)` payload codecs instead of tuple codecs to reduce generic inference risk.
- Use server-enforced movement/action blocking as authority. Client HUD and local drop blocking are convenience only.
- Use vanilla `EntityPose.SLEEPING` as the lying visual fallback. This is not a custom animation and may render imperfectly, but the server lock is authoritative.
- `preserve_downed_state_on_logout=false` clears downed state during `ServerPlayConnectionEvents.DISCONNECT`; default config keeps it.
- Reuse `DownedLootInventory` and `DownedLootScreenHandler` for Detector inspection instead of introducing a second inventory screen.

## Do Not Run Locally

Per project constraint, do not run:

```bash
./gradlew build
./gradlew test
./gradlew check
gradle build
```

Gradle compilation and jar production must happen in GitHub Actions.
