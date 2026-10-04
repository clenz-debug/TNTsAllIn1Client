package com.tntsallin1client.mixin;

import com.tntsallin1client.cape.ClientCapes;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import com.tntsallin1client.offline.OfflineProfile;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives players the cape they set in the launcher, and the local player its own skin and cape in the launcher's offline mode. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
	/**
	 * Despite its name in this name table, `getSkinId` is the method the cape renderer asks for the
	 * cape's texture (the one called `getCapeId` returns the skin). A cape from our server wins over
	 * the one from the player's Mojang account. Offline neither can be fetched - the local player
	 * gets the copy the launcher kept (see {@link OfflineProfile}).
	 */
	@Inject(method = "getSkinId()Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
	private void tnt$clientCape(CallbackInfoReturnable<Identifier> cir) {
		if (tnt$isLocalPlayerOffline() && OfflineProfile.cape() != null) {
			cir.setReturnValue(OfflineProfile.cape());
			return;
		}
		if (!ClientConfig.get().clientCapesEnabled) {
			return;
		}
		Identifier cape = ClientCapes.capeFor(((Entity) (Object) this).getUuid());
		if (cape != null) {
			cir.setReturnValue(cape);
		}
	}

	/** The skin's texture - see above for the name. */
	@Inject(method = "getCapeId()Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
	private void tnt$offlineSkin(CallbackInfoReturnable<Identifier> cir) {
		if (tnt$isLocalPlayerOffline() && OfflineProfile.skin() != null) {
			cir.setReturnValue(OfflineProfile.skin());
		}
	}

	/** Wide or slim arms - they have to match the skin. */
	@Inject(method = "getModel()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
	private void tnt$offlineModel(CallbackInfoReturnable<String> cir) {
		if (tnt$isLocalPlayerOffline() && OfflineProfile.model() != null) {
			cir.setReturnValue(OfflineProfile.model());
		}
	}

	/** Only the player playing on this client is of this kind - every other player keeps what the game shows. */
	@Unique
	private boolean tnt$isLocalPlayerOffline() {
		return (Object) this instanceof ClientPlayerEntity && OfflineProfile.isOfflineLaunch();
	}
}
