package com.lastchance.downed.core.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lastchance.downed.DownedPlayersMod;
import com.lastchance.downed.config.DownedPlayersConfig;
import com.lastchance.downed.mixin.EntityAccessor;
import com.lastchance.downed.network.payload.ReviveProgressPayload;
import com.lastchance.downed.network.payload.StateSyncPayload;
import com.lastchance.downed.screen.gui.DownedLootInventory;
import com.lastchance.downed.screen.gui.DownedLootScreenHandler;
import com.lastchance.downed.util.DownedText;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DownedStateManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_PATH = FabricLoader.getInstance().getConfigDir().resolve("downed_players_state.json");
    private static final DownedStateManager INSTANCE = new DownedStateManager();

    private final Map<UUID, DownedState> downed = new HashMap<>();
    private final Map<UUID, ReviveSession> reviveSessions = new HashMap<>();
    private final Map<UUID, UUID> lootLocks = new HashMap<>();
    private final Set<UUID> deathBypass = new HashSet<>();
    private MinecraftServer server;
    private boolean dirty;

    private DownedStateManager() {
    }

    public static DownedStateManager get() {
        return INSTANCE;
    }

    public static boolean isDowned(PlayerEntity player) {
        return player != null && INSTANCE.downed.containsKey(player.getUuid());
    }

    public static boolean isDeathBypassing(PlayerEntity player) {
        return player != null && INSTANCE.deathBypass.contains(player.getUuid());
    }

    public void load(MinecraftServer server) {
        this.server = server;
        downed.clear();

        if (!DownedPlayersConfig.get().preserve_downed_state_on_server_restart || !Files.exists(STATE_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(STATE_PATH)) {
            PersistedState persisted = GSON.fromJson(reader, PersistedState.class);
            if (persisted != null && persisted.downed != null) {
                long now = now();
                for (DownedState state : persisted.downed) {
                    if (state.deadlineMillis() > now) {
                        downed.put(state.uuid(), state);
                    }
                }
            }
        } catch (IOException exception) {
            DownedPlayersMod.LOGGER.warn("Unable to load persisted downed player state", exception);
        }
    }

    public void save(MinecraftServer server) {
        if (!DownedPlayersConfig.get().preserve_downed_state_on_server_restart) {
            return;
        }

        try {
            Files.createDirectories(STATE_PATH.getParent());
            PersistedState persisted = new PersistedState();
            persisted.downed = new ArrayList<>(downed.values());

            try (Writer writer = Files.newBufferedWriter(STATE_PATH)) {
                GSON.toJson(persisted, writer);
            }

            dirty = false;
        } catch (IOException exception) {
            DownedPlayersMod.LOGGER.warn("Unable to save persisted downed player state", exception);
        }
    }

    public void tick(MinecraftServer server) {
        this.server = server;
        long now = now();
        List<ServerPlayerEntity> expired = new ArrayList<>();

        for (DownedState state : downed.values()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(state.uuid());
            if (player == null) {
                continue;
            }

            lockMovement(player);
            if (state.deadlineMillis() <= now) {
                expired.add(player);
            }
        }

        for (ServerPlayerEntity player : expired) {
            killFinally(player, "timeout");
        }

        tickRevives(server, now);
        if (dirty) {
            save(server);
        }
    }

    public boolean tryEnterDowned(ServerPlayerEntity player, DamageSource source, float amount) {
        if (isDowned(player) || isDeathBypassing(player) || player.isCreative() || player.isSpectator()) {
            return false;
        }

        long now = now();
        long deadline = now + DownedPlayersConfig.get().downed_duration_seconds * 1000L;
        String damageName = source.getName();
        downed.put(player.getUuid(), new DownedState(player.getUuid(), now, deadline, damageName));
        dirty = true;

        player.setHealth(1.0F);
        player.setPose(EntityPose.SLEEPING);
        reinitDimensions(player);
        lockMovement(player);
        cancelRevivesFor(player.getUuid());
        sync(player);

        if (DownedPlayersConfig.get().enable_chat_status_messages) {
            player.sendMessage(DownedText.prefix(Text.literal("You are downed. Hold still for a revive or surrender.")), false);
        }

        return true;
    }

    public void surrender(ServerPlayerEntity player) {
        if (!DownedPlayersConfig.get().enable_surrender_button || !isDowned(player)) {
            return;
        }

        killFinally(player, "surrender");
    }

    public ActionResult tryFinishDowned(PlayerEntity attacker, ServerPlayerEntity target) {
        if (!DownedPlayersConfig.get().allow_finishing_downed_player) {
            return ActionResult.FAIL;
        }

        killFinally(target, "finished");
        return ActionResult.SUCCESS;
    }

    public void beginRevive(ServerPlayerEntity reviver, ServerPlayerEntity target) {
        if (!canRevive(reviver, target)) {
            return;
        }

        long now = now();
        reviveSessions.put(reviver.getUuid(), new ReviveSession(reviver.getUuid(), target.getUuid(), now, now));
        sendReviveProgress(reviver, true, 0.0F);

        if (DownedPlayersConfig.get().enable_chat_status_messages) {
            reviver.sendMessage(DownedText.prefix(Text.literal("Reviving " + target.getName().getString() + "...")), true);
            target.sendMessage(DownedText.prefix(Text.literal(reviver.getName().getString() + " is reviving you.")), true);
        }
    }

    public void heartbeatRevive(ServerPlayerEntity reviver, UUID targetUuid) {
        ReviveSession session = reviveSessions.get(reviver.getUuid());
        if (session == null || !session.targetUuid().equals(targetUuid)) {
            ServerPlayerEntity target = server == null ? null : server.getPlayerManager().getPlayer(targetUuid);
            if (target != null) {
                beginRevive(reviver, target);
            }

            return;
        }

        ServerPlayerEntity target = server == null ? null : server.getPlayerManager().getPlayer(targetUuid);
        if (target == null || !canRevive(reviver, target)) {
            cancelRevive(reviver.getUuid());
            return;
        }

        long now = now();
        session.lastHeartbeatMillis = now;
        float progress = reviveProgress(session, now);
        sendReviveProgress(reviver, true, progress);

        if (progress >= 1.0F) {
            completeRevive(reviver, target);
        }
    }

    public void cancelRevivesFor(UUID playerUuid) {
        cancelRevive(playerUuid);

        Iterator<Map.Entry<UUID, ReviveSession>> iterator = reviveSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ReviveSession> entry = iterator.next();
            if (entry.getValue().targetUuid().equals(playerUuid)) {
                sendReviveProgress(entry.getKey(), false, 0.0F);
                iterator.remove();
            }
        }

        lootLocks.entrySet().removeIf(entry -> entry.getKey().equals(playerUuid) || entry.getValue().equals(playerUuid));
    }

    public void handleDisconnect(ServerPlayerEntity player) {
        cancelRevivesFor(player.getUuid());
        if (!DownedPlayersConfig.get().preserve_downed_state_on_logout && downed.remove(player.getUuid()) != null) {
            player.setPose(EntityPose.STANDING);
            reinitDimensions(player);
            dirty = true;
        }
    }

    public void openLoot(ServerPlayerEntity looter, ServerPlayerEntity target) {
        DownedPlayersConfig config = DownedPlayersConfig.get();
        if (!config.allow_full_inventory_looting || !isDowned(target)) {
            return;
        }

        double maxDistanceSquared = config.loot_open_distance_blocks * config.loot_open_distance_blocks;
        if (looter.squaredDistanceTo(target) > maxDistanceSquared) {
            return;
        }

        UUID existing = lootLocks.get(target.getUuid());
        if (!config.allow_multiple_looters_at_once && existing != null && !existing.equals(looter.getUuid())) {
            looter.sendMessage(DownedText.prefix(Text.literal("Another player is already looting this inventory.")), true);
            return;
        }

        lootLocks.put(target.getUuid(), looter.getUuid());
        DownedLootInventory inventory = new DownedLootInventory(target, () -> releaseLootLock(target.getUuid(), looter.getUuid()));
        looter.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, player) -> new DownedLootScreenHandler(syncId, playerInventory, inventory),
                Text.literal(target.getName().getString() + "'s inventory")
        ));
    }

    public void releaseLootLock(UUID targetUuid, UUID looterUuid) {
        if (looterUuid.equals(lootLocks.get(targetUuid))) {
            lootLocks.remove(targetUuid);
        }
    }

    public void sync(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }

        DownedState state = downed.get(player.getUuid());
        StateSyncPayload payload = state == null
                ? new StateSyncPayload(false, 0L, 0L, DownedPlayersConfig.get().enable_surrender_button)
                : new StateSyncPayload(true, state.enteredAtMillis(), state.deadlineMillis(), DownedPlayersConfig.get().enable_surrender_button);

        if (ServerPlayNetworking.canSend(player, StateSyncPayload.ID)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private void completeRevive(ServerPlayerEntity reviver, ServerPlayerEntity target) {
        downed.remove(target.getUuid());
        reviveSessions.remove(reviver.getUuid());
        lootLocks.remove(target.getUuid());
        dirty = true;

        target.setHealth((float) DownedPlayersConfig.get().hp_after_revive);
        target.setPose(EntityPose.STANDING);
        reinitDimensions(target);
        sync(target);
        sendReviveProgress(reviver, false, 1.0F);

        if (DownedPlayersConfig.get().enable_chat_status_messages) {
            reviver.sendMessage(DownedText.prefix(Text.literal("Revive complete.")), true);
            target.sendMessage(DownedText.prefix(Text.literal("You were revived.")), false);
        }
    }

    private void killFinally(ServerPlayerEntity player, String reason) {
        if (!isDowned(player)) {
            return;
        }

        downed.remove(player.getUuid());
        cancelRevivesFor(player.getUuid());
        dirty = true;
        sync(player);

        deathBypass.add(player.getUuid());
        try {
            player.setPose(EntityPose.STANDING);
            reinitDimensions(player);
            player.damage((ServerWorld) player.getWorld(), player.getDamageSources().genericKill(), Float.MAX_VALUE);
        } finally {
            deathBypass.remove(player.getUuid());
        }

        if (DownedPlayersConfig.get().enable_chat_status_messages) {
            player.sendMessage(DownedText.prefix(Text.literal("You died from " + reason + ".")), false);
        }
    }

    private void tickRevives(MinecraftServer server, long now) {
        Iterator<Map.Entry<UUID, ReviveSession>> iterator = reviveSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ReviveSession> entry = iterator.next();
            ReviveSession session = entry.getValue();
            ServerPlayerEntity reviver = server.getPlayerManager().getPlayer(session.reviverUuid());
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(session.targetUuid());

            if (reviver == null || target == null || !canRevive(reviver, target)) {
                sendReviveProgress(session.reviverUuid(), false, 0.0F);
                iterator.remove();
                continue;
            }

            float progress = reviveProgress(session, now);
            if (progress >= 1.0F) {
                iterator.remove();
                completeRevive(reviver, target);
                continue;
            }

            sendReviveProgress(reviver, true, progress);
        }
    }

    private boolean canRevive(ServerPlayerEntity reviver, ServerPlayerEntity target) {
        if (reviver.equals(target) || isDowned(reviver) || !isDowned(target)) {
            return false;
        }

        double maxDistanceSquared = DownedPlayersConfig.get().revive_distance_blocks * DownedPlayersConfig.get().revive_distance_blocks;
        return reviver.squaredDistanceTo(target) <= maxDistanceSquared;
    }

    private float reviveProgress(ReviveSession session, long now) {
        long requiredMillis = DownedPlayersConfig.get().revive_duration_seconds * 1000L;
        return Math.min(1.0F, (float) (now - session.startedAtMillis()) / (float) requiredMillis);
    }

    private void cancelRevive(UUID reviverUuid) {
        ReviveSession removed = reviveSessions.remove(reviverUuid);
        if (removed != null) {
            sendReviveProgress(reviverUuid, false, 0.0F);
        }
    }

    private void sendReviveProgress(UUID reviverUuid, boolean active, float progress) {
        if (server == null) {
            return;
        }

        ServerPlayerEntity reviver = server.getPlayerManager().getPlayer(reviverUuid);
        if (reviver != null) {
            sendReviveProgress(reviver, active, progress);
        }
    }

    private void sendReviveProgress(ServerPlayerEntity reviver, boolean active, float progress) {
        if (ServerPlayNetworking.canSend(reviver, ReviveProgressPayload.ID)) {
            ServerPlayNetworking.send(reviver, new ReviveProgressPayload(active, progress));
        }
    }

    private void lockMovement(ServerPlayerEntity player) {
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.fallDistance = 0.0F;
        player.setSprinting(false);
    }

    private void reinitDimensions(ServerPlayerEntity player) {
        ((EntityAccessor) player).downed_players$reinitDimensions();
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static final class PersistedState {
        private List<DownedState> downed = List.of();
    }

    private static final class ReviveSession {
        private final UUID reviverUuid;
        private final UUID targetUuid;
        private final long startedAtMillis;
        private long lastHeartbeatMillis;

        private ReviveSession(UUID reviverUuid, UUID targetUuid, long startedAtMillis, long lastHeartbeatMillis) {
            this.reviverUuid = reviverUuid;
            this.targetUuid = targetUuid;
            this.startedAtMillis = startedAtMillis;
            this.lastHeartbeatMillis = lastHeartbeatMillis;
        }

        private UUID reviverUuid() {
            return reviverUuid;
        }

        private UUID targetUuid() {
            return targetUuid;
        }

        private long startedAtMillis() {
            return startedAtMillis;
        }
    }
}
