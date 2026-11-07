package com.autooptimize.mixin;

import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pantalla roja de Mojang: sin tiempo minimo y sin fundidos. */
@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;

    private boolean autooptimize$startAdjusted = false;
    private boolean autooptimize$doneAdjusted = false;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void autooptimize$fast(CallbackInfo ci) {
        // Quita el tiempo minimo que el logo debe mostrarse (1 s) y el fundido de entrada
        if (!autooptimize$startAdjusted && reloadStartTime > -1L) {
            reloadStartTime -= 1000L;
            autooptimize$startAdjusted = true;
        }
        // Termina al instante el fundido de salida (1 s)
        if (!autooptimize$doneAdjusted && reloadCompleteTime > -1L) {
            reloadCompleteTime -= 2000L;
            autooptimize$doneAdjusted = true;
        }
    }
}
