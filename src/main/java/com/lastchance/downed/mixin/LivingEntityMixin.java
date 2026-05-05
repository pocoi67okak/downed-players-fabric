package com.lastchance.downed.mixin;

import com.lastchance.downed.config.DownedPlayersConfig;
import com.lastchance.downed.core.state.DownedStateManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$interceptPlayerDamage(ServerWorld world, DamageSource source, float amount,
                                                    CallbackInfoReturnable<Boolean> cir) {
        Object self = (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }

        if (DownedStateManager.isDeathBypassing(player)) {
            return;
        }

        if (DownedStateManager.isDowned(player)) {
            if (!DownedPlayersConfig.get().allow_downed_player_to_take_damage) {
                cir.setReturnValue(false);
            }

            return;
        }

        if (player.getHealth() - amount <= 0.0F && DownedStateManager.get().tryEnterDowned(player, source, amount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockJump(CallbackInfo ci) {
        Object self = (Object) this;
        if (self instanceof PlayerEntity player && DownedStateManager.isDowned(player)) {
            ci.cancel();
        }
    }
}
