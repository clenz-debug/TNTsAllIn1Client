package com.tntsallin1client.mixin;

import com.tntsallin1client.friends.ClientUserBadges;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nametag logo for players who use our client (see {@link ClientUserBadges}): this is where the
 * game draws an entity's name - sneaking or not. The text itself is drawn by a general routine
 * (`GameRendererMixin` puts the logo next to it there) that doesn't know whose name it is, so it is
 * told here. A player's scoreboard line is drawn before this and gets no logo.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Inject(method = "renderNameTags", at = @At("HEAD"))
	private void tntsallin1client$beginName(Entity entity, double x, double y, double z, String name, double distanceSquared, CallbackInfo ci) {
		ClientUserBadges.beginName(entity);
	}

	@Inject(method = "renderNameTags", at = @At("RETURN"))
	private void tntsallin1client$endName(Entity entity, double x, double y, double z, String name, double distanceSquared, CallbackInfo ci) {
		ClientUserBadges.endName();
	}
}
