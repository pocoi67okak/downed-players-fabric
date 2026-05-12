package com.lastchance.downed;

import com.lastchance.downed.config.DownedPlayersConfig;
import com.lastchance.downed.core.state.DownedStateManager;
import com.lastchance.downed.item.ModItems;
import com.lastchance.downed.network.payload.HandsUpTogglePayload;
import com.lastchance.downed.network.payload.ReviveHeartbeatPayload;
import com.lastchance.downed.network.payload.ReviveProgressPayload;
import com.lastchance.downed.network.payload.StateSyncPayload;
import com.lastchance.downed.network.payload.SurrenderPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DownedPlayersMod implements ModInitializer {
    public static final String MOD_ID = "downed_players";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModItems.register();
        DownedPlayersConfig.load();
        registerPayloads();
        registerServerReceivers();
        registerServerEvents();
    }

    private static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(StateSyncPayload.ID, StateSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ReviveProgressPayload.ID, ReviveProgressPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SurrenderPayload.ID, SurrenderPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ReviveHeartbeatPayload.ID, ReviveHeartbeatPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HandsUpTogglePayload.ID, HandsUpTogglePayload.CODEC);
    }

    private static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(SurrenderPayload.ID, (payload, context) ->
                DownedStateManager.get().surrender(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ReviveHeartbeatPayload.ID, (payload, context) ->
                DownedStateManager.get().heartbeatRevive(context.player(), payload.targetUuid()));
        ServerPlayNetworking.registerGlobalReceiver(HandsUpTogglePayload.ID, (payload, context) ->
                DownedStateManager.get().toggleHandsUp(context.player()));
    }

    private static void registerServerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(DownedStateManager.get()::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(DownedStateManager.get()::save);
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> DownedStateManager.get().save(server));
        ServerTickEvents.END_SERVER_TICK.register(DownedStateManager.get()::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                DownedStateManager.get().sync(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                DownedStateManager.get().handleDisconnect(handler.player));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (DownedStateManager.isDowned(player)) {
                return ActionResult.FAIL;
            }

            if (!world.isClient() && entity instanceof ServerPlayerEntity target && DownedStateManager.isDowned(target)) {
                return DownedStateManager.get().tryFinishDowned(player, target);
            }

            return ActionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (DownedStateManager.isDowned(player)) {
                return ActionResult.FAIL;
            }

            if (!world.isClient() && player instanceof ServerPlayerEntity reviver && entity instanceof ServerPlayerEntity target
                    && DownedStateManager.isDowned(target)) {
                if (player.isSneaking()) {
                    DownedStateManager.get().openLoot(reviver, target);
                }

                return ActionResult.SUCCESS;
            }

            if (!world.isClient() && player instanceof ServerPlayerEntity looter && entity instanceof ServerPlayerEntity target
                    && player.getStackInHand(hand).isOf(ModItems.DETECTOR)
                    && DownedStateManager.isHandsUp(target)) {
                DownedStateManager.get().openDetectorInspection(looter, target);
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                DownedStateManager.isDowned(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) ->
                DownedStateManager.isDowned(player) ? ActionResult.FAIL : ActionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !DownedStateManager.isDowned(player));
    }
}
