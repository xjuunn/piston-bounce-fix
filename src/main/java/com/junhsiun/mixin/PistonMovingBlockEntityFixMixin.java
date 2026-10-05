package com.junhsiun.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;

@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonMovingBlockEntityFixMixin {
	@WrapOperation(
		method = "moveCollidedEntities",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V"
		)
	)
	private static void pbf$skipServerSidePlayerBounceVelocity(
		Entity entity, double xd, double yd, double zd, Operation<Void> original
	) {
		if (entity instanceof ServerPlayer player) {
			player.applyPostImpulseGraceTime(40);
			return;
		}

		original.call(entity, xd, yd, zd);
	}
}