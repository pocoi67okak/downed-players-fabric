package com.lastchance.downed.mixin;

import com.lastchance.downed.client.DownedPlayersClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockClientDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (DownedPlayersClient.isLocalDowned()) {
            cir.setReturnValue(false);
        }
    }
}
