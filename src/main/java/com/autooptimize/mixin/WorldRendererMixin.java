package com.autooptimize.mixin;

import com.autooptimize.AutoOptimizeClient;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    /** Lluvia y nieve: no se dibujan en niveles EXTREMO y ULTRA. */
    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true, require = 0)
    private void autooptimize$noWeather(CallbackInfo ci) {
        if (AutoOptimizeClient.cancelRain()) ci.cancel();
    }
}
