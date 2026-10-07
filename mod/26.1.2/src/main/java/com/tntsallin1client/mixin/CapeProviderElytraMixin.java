package com.tntsallin1client.mixin;

import com.tntsallin1client.compat.CapeElytra;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The bundled Cape Provider mod gives a worn elytra the cape's image whenever that image is twice
 * as wide as high - which every cape from our launcher is, painted elytra part or not. A cape whose
 * elytra part is one plain color leaves the elytra its own look instead (see {@link CapeElytra}).
 *
 * <p>Hooks Cape Provider's own class, by name: without the mod in the instance this does nothing,
 * and if one of its two methods should ever be renamed the elytra simply gets the cape's image
 * again as Cape Provider decides - nothing crashes. Both methods exist unchanged in every Cape
 * Provider version we bundle (checked with javap).
 */
@Pseudo
@Mixin(targets = "net.litetex.capes.handler.PlayerCapeHandler", remap = false)
public abstract class CapeProviderElytraMixin {
	@Unique
	private boolean tntsallin1client$plainElytra;

	/**
	 * Where Cape Provider turns a downloaded cape file into textures - the file is that method's only
	 * argument of this kind, which is passed on as it came.
	 */
	@ModifyVariable(method = "determineTexturesToRegister", at = @At("HEAD"), argsOnly = true, require = 0, remap = false)
	private byte[] tntsallin1client$noteElytraPart(byte[] imageFile) {
		this.tntsallin1client$plainElytra = CapeElytra.isPlain(imageFile);
		return imageFile;
	}

	@Inject(method = "hasElytraTexture", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
	private void tntsallin1client$ownLookForPlainElytra(CallbackInfoReturnable<Boolean> cir) {
		if (this.tntsallin1client$plainElytra) {
			cir.setReturnValue(false);
		}
	}
}
