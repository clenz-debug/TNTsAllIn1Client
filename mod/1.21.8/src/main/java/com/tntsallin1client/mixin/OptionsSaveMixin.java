package com.tntsallin1client.mixin;

import com.tntsallin1client.fullbright.FullbrightHandler;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * See {@link FullbrightHandler}'s own doc comment for why this exists: brackets {@code save()} so
 * the out-of-range fullbright gamma bypass is never what actually gets written to options.txt.
 */
@Mixin(Options.class)
public class OptionsSaveMixin {
	@Inject(method = "save", at = @At("HEAD"))
	private void tntsallin1client$beforeSave(CallbackInfo ci) {
		if (FullbrightHandler.isActive()) {
			FullbrightHandler.restoreRealGammaForSave((Options) (Object) this);
		}
	}

	@Inject(method = "save", at = @At("TAIL"))
	private void tntsallin1client$afterSave(CallbackInfo ci) {
		if (FullbrightHandler.isActive()) {
			FullbrightHandler.reapplyBypassAfterSave((Options) (Object) this);
		}
	}
}
