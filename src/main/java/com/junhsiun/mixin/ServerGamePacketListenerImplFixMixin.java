package com.junhsiun.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplFixMixin {
	@Inject(method = "isEntityCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true)
	private void pbf$trustClientDuringImpulseGrace(
		final LevelReader level,
		final Entity entity,
		final AABB oldAABB,
		final double newX,
		final double newY,
		final double newZ,
		final CallbackInfoReturnable<Boolean> cir
	) {
		if (entity instanceof LivingEntity living && living.isInPostImpulseGraceTime()) {
			cir.setReturnValue(false);
		}
	}
}