package com.autooptimize.mixin;

import com.autooptimize.AutoOptimizeClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    /** Mobs lejanos: el cliente solo los tickea 1 de cada 4 veces (animaciones/logica visual). */
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private void autooptimize$throttle(Entity entity, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity) || entity instanceof PlayerEntity) return;
        if (entity.hasVehicle() || entity.hasPassengers()) return;
        Entity cam = MinecraftClient.getInstance().getCameraEntity();
        if (cam == null) return;
        if (entity.squaredDistanceTo(cam) < AutoOptimizeClient.tickDistSq()) return;
        long t = ((ClientWorld) (Object) this).getTime();
        if (((t + entity.getId()) & 3L) != 0L) ci.cancel();
    }
}
