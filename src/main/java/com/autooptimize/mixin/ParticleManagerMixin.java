package com.autooptimize.mixin;

import com.autooptimize.AutoOptimizeClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    private static final String TYPED =
        "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;";

    /** true solo mientras se crea una particula permitida (feedback de combate cercano). */
    private static boolean autooptimize$allow = false;

    @Inject(method = TYPED, at = @At("HEAD"), cancellable = true)
    private void autooptimize$cull(ParticleEffect effect, double x, double y, double z,
                                   double vx, double vy, double vz,
                                   CallbackInfoReturnable<Particle> cir) {
        autooptimize$allow = false;
        double maxSq = AutoOptimizeClient.particleDistSq();
        if (maxSq <= 0) { cir.setReturnValue(null); return; }

        ParticleType<?> t = effect.getType();
        boolean ok = t == ParticleTypes.CRIT || t == ParticleTypes.DAMAGE_INDICATOR
                || t == ParticleTypes.SWEEP_ATTACK || t == ParticleTypes.ENCHANTED_HIT;
        if (!ok) { cir.setReturnValue(null); return; }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
            Vec3d cam = mc.gameRenderer.getCamera().getPos();
            double dx = cam.x - x, dy = cam.y - y, dz = cam.z - z;
            if (dx * dx + dy * dy + dz * dz > maxSq) { cir.setReturnValue(null); return; }
        }
        autooptimize$allow = true;
    }

    @Inject(method = TYPED, at = @At("RETURN"))
    private void autooptimize$reset(CallbackInfoReturnable<Particle> cir) {
        autooptimize$allow = false;
    }

    /** Particulas construidas directamente (romper bloques, etc.): solo pasan las permitidas arriba. */
    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V",
            at = @At("HEAD"), cancellable = true)
    private void autooptimize$cullDirect(Particle particle, CallbackInfo ci) {
        if (!autooptimize$allow) ci.cancel();
    }
}
