package com.lastchance.downed.mixin;

import com.lastchance.downed.client.DownedPlayersClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void downedPlayers$handleHandsUpKey(int keyCode, int scanCode, int modifiers,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (DownedPlayersClient.handleHandsUpKeyPressedInScreen(keyCode, scanCode)) {
            cir.setReturnValue(true);
        }
    }
}
