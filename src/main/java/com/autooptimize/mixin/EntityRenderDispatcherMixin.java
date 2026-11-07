package com.autooptimize.mixin;

import com.autooptimize.AutoOptimizeClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    private static int autooptimize$far = 0;

    /** Se llama una vez por frame: reinicia el contador de entidades lejanas. */
    @Inject(method = "configure", at = @At("HEAD"), require = 0)
    private void autooptimize$reset(CallbackInfo ci) {
        autooptimize$far = 0;
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void autooptimize$cull(Entity entity, Frustum frustum, double x, double y, double z,
                                   CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity cam = mc.getCameraEntity();
        if (cam == null || entity == cam || entity == mc.player) return;

        boolean huge = entity.getWidth() > 3.0f;
        double lim = AutoOptimizeClient.entityDistSq();
        if (entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity) {
            lim = Math.min(lim, AutoOptimizeClient.smallDistSq());
        } else if (huge) {
            lim *= 4.0;
        }
        double dx = entity.getX() - x, dy = entity.getY() - y, dz = entity.getZ() - z;
        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 > lim) { cir.setReturnValue(false); return; }

        // Tope de entidades lejanas visibles por frame (granjas de mobs, multitudes)
        if (!huge && d2 > 144.0 && frustum.isVisible(entity.getVisibilityBoundingBox())) {
            if (++autooptimize$far > AutoOptimizeClient.farCap()) cir.setReturnValue(false);
        }
    }
}
