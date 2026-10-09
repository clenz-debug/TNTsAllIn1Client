package com.tntsallin1client.mixin;

import com.tntsallin1client.friends.ClientUserBadges;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nametag logo for other players who use our client (see {@link ClientUserBadges}). {@code getNameTag}
 * is the only source of {@code EntityRenderState#nameTag} - {@code EntityRenderer#extractRenderState}
 * stores its result, and neither {@code LivingEntityRenderer} nor {@code AvatarRenderer} writes the
 * field afterwards (checked with javap) - so prepending the logo here reaches every nametag drawn.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameTagMixin {
	@Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
	private void tntsallin1client$addClientBadge(Entity entity, CallbackInfoReturnable<Component> cir) {
		if (entity instanceof Player player) {
			cir.setReturnValue(ClientUserBadges.withBadge(player.getUUID(), cir.getReturnValue()));
		}
	}
}
