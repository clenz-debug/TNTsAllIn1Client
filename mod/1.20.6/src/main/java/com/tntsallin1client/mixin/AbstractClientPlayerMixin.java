package com.tntsallin1client.mixin;

import com.tntsallin1client.cape.ClientCapes;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.offline.OfflineProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives players the cape they set in the launcher: a cape from our server wins over the one from the
 * player's Mojang account. Offline neither can be fetched - the local player keeps the copy the
 * launcher handed over (see {@link OfflineProfile}).
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void tntsallin1client$clientCape(CallbackInfoReturnable<PlayerSkin> cir) {
		if (!ClientConfig.get().clientCapesEnabled || ((Object) this instanceof LocalPlayer && OfflineProfile.isOfflineLaunch())) {
			return;
		}
		PlayerSkin skin = cir.getReturnValue();
		PlayerSkin dressed = ClientCapes.withCape(((Entity) (Object) this).getUUID(), skin);
		if (dressed != skin) {
			cir.setReturnValue(dressed);
		}
	}
}
