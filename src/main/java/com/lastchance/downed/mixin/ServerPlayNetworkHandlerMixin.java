package com.lastchance.downed.mixin;

import com.lastchance.downed.core.state.DownedStateManager;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockMovementPackets(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (DownedStateManager.isDowned(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "onUpdateSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockHotbarSelection(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {
        if (DownedStateManager.isDowned(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "onClickSlot", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockInventoryClicks(ClickSlotC2SPacket packet, CallbackInfo ci) {
        if (DownedStateManager.isDowned(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockPlayerActions(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (DownedStateManager.isDowned(player)) {
            ci.cancel();
        }
    }
}
