package com.tntsallin1client.mixin;

import java.util.List;

import com.tntsallin1client.debug.QuickInfo;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** F3 Quick Info: the debug screen puts its two columns of text together as lists of lines - ours are added to them, see {@link QuickInfo}. */
@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
	@Inject(method = "getLeftText()Ljava/util/List;", at = @At("RETURN"))
	private void tnt$addQuickInfo(CallbackInfoReturnable<List<String>> cir) {
		QuickInfo.editLeftText(cir.getReturnValue());
	}

	@Inject(method = "getRightText()Ljava/util/List;", at = @At("RETURN"))
	private void tnt$moveSystemInfoAway(CallbackInfoReturnable<List<String>> cir) {
		QuickInfo.editRightText(cir.getReturnValue());
	}
}
