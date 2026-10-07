package com.tntsallin1client.mixin;

import com.tntsallin1client.cape.ClientCapes;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A worn elytra takes its look from the player's cape, if there is one. A cape of ours whose elytra
 * part is only one plain color (what the launcher's cape converter makes) leaves the elytra its own
 * look instead - see {@link ClientCapes#elytraTexture}. The game asks for the cape twice here, once
 * to see whether there is one and once to use it; both get the same answer.
 */
@Mixin(ElytraLayer.class)
public abstract class ElytraLayerMixin {
	@Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFFFFFF)V", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/AbstractClientPlayer;getCloakTextureLocation()Lnet/minecraft/resources/ResourceLocation;"))
	private ResourceLocation tntsallin1client$capeForElytra(AbstractClientPlayer player) {
		return ClientCapes.elytraTexture(player.getCloakTextureLocation());
	}
}
