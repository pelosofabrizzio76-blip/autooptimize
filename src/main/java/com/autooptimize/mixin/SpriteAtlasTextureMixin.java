package com.autooptimize.mixin;

import com.autooptimize.AutoOptimizeClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteAtlasTexture.class)
public class SpriteAtlasTextureMixin {

    private static int autooptimize$counter = 0;

    /** Texturas animadas (agua, lava, fuego, portales...): se actualizan solo 1 de cada N ticks. */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void autooptimize$throttle(CallbackInfo ci) {
        if ((++autooptimize$counter % AutoOptimizeClient.animDivisor()) != 0) ci.cancel();
    }
}
