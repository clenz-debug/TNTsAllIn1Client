package com.tntsallin1client.mixin;

import com.tntsallin1client.cape.ClientCapes;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.offline.OfflineProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives players the cape they set in the launcher, and the local player its own skin and cape in the launcher's offline mode. */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	/**
	 * A cape from our server wins over the one from the player's Mojang account. Offline neither can
	 * be fetched - the local player gets the copy the launcher kept (see {@link OfflineProfile}).
	 */
	@Inject(method = "getCloakTextureLocation", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$clientCape(CallbackInfoReturnable<ResourceLocation> cir) {
		if (tntsallin1client$isLocalPlayerOffline() && OfflineProfile.cape() != null) {
			cir.setReturnValue(OfflineProfile.cape());
			return;
		}
		if (!ClientConfig.get().clientCapesEnabled) {
			return;
		}
		ResourceLocation cape = ClientCapes.capeFor(((Entity) (Object) this).getUUID());
		if (cape != null) {
			cir.setReturnValue(cape);
		}
	}

	@Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$offlineSkin(CallbackInfoReturnable<ResourceLocation> cir) {
		if (tntsallin1client$isLocalPlayerOffline() && OfflineProfile.skin() != null) {
			cir.setReturnValue(OfflineProfile.skin());
		}
	}

	/** Wide or slim arms - they have to match the skin. */
	@Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$offlineModel(CallbackInfoReturnable<String> cir) {
		if (tntsallin1client$isLocalPlayerOffline() && OfflineProfile.model() != null) {
			cir.setReturnValue(OfflineProfile.model());
		}
	}

	/** Only the player playing on this client is of this kind - every other player keeps what the game shows. */
	@Unique
	private boolean tntsallin1client$isLocalPlayerOffline() {
		return (Object) this instanceof LocalPlayer && OfflineProfile.isOfflineLaunch();
	}
}
