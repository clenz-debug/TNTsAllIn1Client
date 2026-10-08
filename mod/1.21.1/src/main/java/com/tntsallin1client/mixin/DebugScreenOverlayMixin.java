package com.tntsallin1client.mixin;

import com.tntsallin1client.debug.QuickInfo;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** F3 Quick Info: the debug screen puts its two columns of text together as lists of lines - ours are added to them, see {@link QuickInfo}. */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
	@Inject(method = "getGameInformation", at = @At("RETURN"))
	private void tntsallin1client$addQuickInfo(CallbackInfoReturnable<List<String>> cir) {
		QuickInfo.editLeftText(cir.getReturnValue());
	}

	@Inject(method = "getSystemInformation", at = @At("RETURN"))
	private void tntsallin1client$moveSystemInfoAway(CallbackInfoReturnable<List<String>> cir) {
		QuickInfo.editRightText(cir.getReturnValue());
	}
}
