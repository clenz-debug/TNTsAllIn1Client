package com.tntsallin1client.mixin;

import com.tntsallin1client.cape.ClientCapes;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives players the cape they set in the launcher. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
	/**
	 * Despite its name in this name table, `getSkinId` is the method the cape renderer asks for the
	 * cape's texture (the one called `getCapeId` returns the skin). A cape from our server wins over
	 * the one from the player's Mojang account.
	 */
	@Inject(method = "getSkinId()Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
	private void tnt$clientCape(CallbackInfoReturnable<Identifier> cir) {
		if (!ClientConfig.get().clientCapesEnabled) {
			return;
		}
		Identifier cape = ClientCapes.capeFor(((Entity) (Object) this).getUuid());
		if (cape != null) {
			cir.setReturnValue(cape);
		}
	}
}
