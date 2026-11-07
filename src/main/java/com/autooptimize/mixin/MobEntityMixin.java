package com.autooptimize.mixin;

import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    /** IA: mobs sin objetivo y a mas de 40 bloques de cualquier jugador piensan 1 de cada 4 ticks. */
    @Inject(method = "tickNewAi", at = @At("HEAD"), cancellable = true, require = 0)
    private void autooptimize$throttleAi(CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (self.getWorld().isClient) return;
        if (self.getTarget() != null || self.hasVehicle() || self.hasPassengers() || self.isLeashed()) return;
        if (((self.age + self.getId()) & 3) == 0) return;
        if (self.getWorld().getClosestPlayer(self, 40.0) != null) return;
        ci.cancel();
    }
}
