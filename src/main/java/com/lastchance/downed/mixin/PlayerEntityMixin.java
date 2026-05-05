package com.lastchance.downed.mixin;

import com.lastchance.downed.core.state.DownedStateManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockDropItem(ItemStack stack, boolean retainOwnership,
                                            CallbackInfoReturnable<ItemEntity> cir) {
        if (DownedStateManager.isDowned((PlayerEntity) (Object) this)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "openHandledScreen", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockOpeningOwnScreens(NamedScreenHandlerFactory factory,
                                                     CallbackInfoReturnable<OptionalInt> cir) {
        if (DownedStateManager.isDowned((PlayerEntity) (Object) this)) {
            cir.setReturnValue(OptionalInt.empty());
        }
    }

}
