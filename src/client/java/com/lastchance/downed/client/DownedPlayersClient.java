package com.lastchance.downed.client;

import com.lastchance.downed.network.payload.ReviveHeartbeatPayload;
import com.lastchance.downed.network.payload.ReviveProgressPayload;
import com.lastchance.downed.network.payload.StateSyncPayload;
import com.lastchance.downed.network.payload.SurrenderPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public final class DownedPlayersClient implements ClientModInitializer {
    private static KeyBinding surrenderKey;
    private static boolean localDowned;
    private static long enteredAtMillis;
    private static long deadlineMillis;
    private static boolean surrenderEnabled;
    private static boolean reviveActive;
    private static float reviveProgress;
    private static int heartbeatCooldownTicks;
    private static UUID reviveTargetUuid;

    public static boolean isLocalDowned() {
        return localDowned;
    }

    @Override
    public void onInitializeClient() {
        surrenderKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.downed_players.surrender",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.downed_players"
        ));

        ClientPlayNetworking.registerGlobalReceiver(StateSyncPayload.ID, (payload, context) -> {
            localDowned = payload.downed();
            enteredAtMillis = payload.enteredAtMillis();
            deadlineMillis = payload.deadlineMillis();
            surrenderEnabled = payload.surrenderEnabled();
        });

        ClientPlayNetworking.registerGlobalReceiver(ReviveProgressPayload.ID, (payload, context) -> {
            reviveActive = payload.active();
            reviveProgress = payload.progress();
        });

        ClientTickEvents.END_CLIENT_TICK.register(DownedPlayersClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null) {
            localDowned = false;
            reviveActive = false;
            return;
        }

        while (surrenderKey.wasPressed()) {
            if (localDowned && surrenderEnabled && ClientPlayNetworking.canSend(SurrenderPayload.ID)) {
                ClientPlayNetworking.send(new SurrenderPayload());
            }
        }

        if (heartbeatCooldownTicks > 0) {
            heartbeatCooldownTicks--;
        }

        if (!client.options.useKey.isPressed() || client.player.isSneaking() || client.currentScreen != null) {
            reviveTargetUuid = null;
            return;
        }

        if (client.crosshairTarget instanceof EntityHitResult hitResult) {
            Entity entity = hitResult.getEntity();
            if (entity instanceof PlayerEntity target) {
                reviveTargetUuid = target.getUuid();
            }
        }

        if (heartbeatCooldownTicks == 0 && reviveTargetUuid != null && ClientPlayNetworking.canSend(ReviveHeartbeatPayload.ID)) {
            ClientPlayNetworking.send(new ReviveHeartbeatPayload(reviveTargetUuid));
            heartbeatCooldownTicks = 4;
        }
    }

    public static void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || (!localDowned && !reviveActive)) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        if (localDowned) {
            long remainingSeconds = Math.max(0L, (deadlineMillis - System.currentTimeMillis() + 999L) / 1000L);
            String title = "DOWNED";
            String timer = "Time left: " + remainingSeconds + "s";
            String surrender = surrenderEnabled ? "Press R to surrender" : "Wait for a revive";
            drawCenteredLine(context, client, title, width / 2, height / 2 - 34, 0xFFFF5555);
            drawCenteredLine(context, client, timer, width / 2, height / 2 - 20, 0xFFFFFFFF);
            drawCenteredLine(context, client, surrender, width / 2, height / 2 - 8, 0xFFE0E0E0);
        }

        if (reviveActive) {
            int barWidth = 120;
            int barHeight = 8;
            int x = (width - barWidth) / 2;
            int y = height - 64;
            int filled = Math.round(barWidth * Math.min(1.0F, Math.max(0.0F, reviveProgress)));
            context.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xAA000000);
            context.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);
            context.fill(x, y, x + filled, y + barHeight, 0xFF55FF99);
            drawCenteredLine(context, client, "Reviving", width / 2, y - 12, 0xFFFFFFFF);
        }
    }

    private static void drawCenteredLine(DrawContext context, MinecraftClient client, String text, int centerX, int y, int color) {
        int x = centerX - client.textRenderer.getWidth(text) / 2;
        context.drawTextWithShadow(client.textRenderer, Text.literal(text), x, y, color);
    }
}
