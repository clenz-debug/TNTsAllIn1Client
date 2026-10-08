package com.tntsallin1client.mixin;

import com.tntsallin1client.friends.ClientUserBadges;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Nametag logo for other players who use our client (see {@link ClientUserBadges}). {@code render}
 * asks the entity for its display name and hands it to {@code renderNameTag} (checked with javap) -
 * the logo is put in front right there. Not in {@code renderNameTag} itself: the player renderer
 * sends the scoreboard line below the name through that method too.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameTagMixin {
	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;getDisplayName()Lnet/minecraft/network/chat/Component;"))
	private Component tntsallin1client$addClientBadge(Entity entity) {
		Component name = entity.getDisplayName();
		return entity instanceof Player player ? ClientUserBadges.withBadge(player.getUUID(), name) : name;
	}
}
