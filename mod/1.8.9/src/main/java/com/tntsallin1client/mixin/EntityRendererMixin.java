package com.tntsallin1client.mixin;

import com.tntsallin1client.friends.ClientUserBadges;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nametag logo for players who use our client (see {@link ClientUserBadges}): drawn right after the
 * game has drawn a name, while the name's position and scale are still in place.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Shadow
	public abstract TextRenderer getFontRenderer();

	/**
	 * This method draws every text above an entity. For a player that is the name - and, on servers
	 * that show one, the scoreboard line below it, which must not get a logo of its own.
	 */
	@Inject(method = "renderLabelIfPresent(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;popMatrix()V"))
	private void tnt$drawClientBadge(Entity entity, String text, double x, double y, double z, int maxDistance, CallbackInfo ci) {
		if (entity instanceof PlayerEntity && text.equals(entity.getName().asFormattedString())) {
			// The game draws this one name ten units higher.
			int top = text.equals("deadmau5") ? -10 : 0;
			ClientUserBadges.drawBeside((PlayerEntity) entity, getFontRenderer().getStringWidth(text), top, false);
		}
	}
}
