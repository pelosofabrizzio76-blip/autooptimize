package com.autooptimize.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Menu principal: aparece completo desde el primer frame, sin fundido del logo y botones. */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Mutable @Shadow private boolean doBackgroundFade;

    @Inject(method = "init", at = @At("HEAD"), require = 0)
    private void autooptimize$noFade(CallbackInfo ci) {
        this.doBackgroundFade = false;
    }
}
