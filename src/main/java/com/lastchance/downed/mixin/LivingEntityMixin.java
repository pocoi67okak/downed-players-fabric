package com.lastchance.downed.mixin;

import com.lastchance.downed.core.state.DownedStateManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockJump(CallbackInfo ci) {
        Object self = (Object) this;
        if (self instanceof PlayerEntity player && DownedStateManager.isDowned(player)) {
            ci.cancel();
        }
    }
}
