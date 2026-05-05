package com.lastchance.downed.mixin;

import com.lastchance.downed.core.state.DownedStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow
    protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockTryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (DownedStateManager.isDowned(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockInteractItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand,
                                                CallbackInfoReturnable<ActionResult> cir) {
        if (DownedStateManager.isDowned(player)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$blockInteractBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand,
                                                 BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (DownedStateManager.isDowned(player)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
