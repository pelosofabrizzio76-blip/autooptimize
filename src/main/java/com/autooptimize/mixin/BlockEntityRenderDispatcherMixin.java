package com.autooptimize.mixin;

import com.autooptimize.AutoOptimizeClient;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At("HEAD"), cancellable = true)
    private void autooptimize$cull(BlockEntity be, float tickDelta, MatrixStack matrices,
                                   VertexConsumerProvider vcp, CallbackInfo ci) {
        Entity cam = MinecraftClient.getInstance().getCameraEntity();
        if (cam == null) return;
        BlockPos p = be.getPos();
        double dx = p.getX() + 0.5 - cam.getX();
        double dy = p.getY() + 0.5 - cam.getY();
        double dz = p.getZ() + 0.5 - cam.getZ();
        if (dx * dx + dy * dy + dz * dz > AutoOptimizeClient.blockEntDistSq()) ci.cancel();
    }
}
