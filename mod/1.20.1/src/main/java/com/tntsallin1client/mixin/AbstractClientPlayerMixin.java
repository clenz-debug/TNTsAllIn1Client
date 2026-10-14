package com.tntsallin1client.mixin;

import com.tntsallin1client.cape.ClientCapes;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.offline.OfflineProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives players the cape they set in the launcher: a cape from our server wins over the one from the
 * player's Mojang account. Offline neither can be fetched - the local player keeps the copy the
 * launcher handed over (see {@link OfflineProfile} and {@code PlayerInfoMixin}).
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@Inject(method = "getCloakTextureLocation", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$clientCape(CallbackInfoReturnable<ResourceLocation> cir) {
		if (!ClientConfig.get().clientCapesEnabled || ((Object) this instanceof LocalPlayer && OfflineProfile.isOfflineLaunch())) {
			return;
		}
		ResourceLocation cape = ClientCapes.capeFor(((Entity) (Object) this).getUUID());
		if (cape != null) {
			cir.setReturnValue(cape);
		}
	}
}
