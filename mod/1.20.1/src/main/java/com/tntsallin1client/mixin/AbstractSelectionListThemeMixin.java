package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lists on themed screens ({@link ThemedUi}): no dirt behind them and no dirt bars above and below,
 * thin accent lines at their edges instead, and the same flat scrollbar track and accent thumb as the
 * Client Mods card menu. This version's lists are no widgets yet and draw all of that in one
 * {@code render}: the dirt behind them and the bars are switched off by their own two flags for the
 * time of the call; the scrollbar is three plain fills (bytecode-verified: the black track, the gray
 * thumb, its lighter top-left part).
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListThemeMixin {
	@Shadow
	private boolean renderBackground;

	@Shadow
	private boolean renderTopAndBottom;

	@Shadow
	protected int x0;

	@Shadow
	protected int x1;

	@Shadow
	protected int y0;

	@Shadow
	protected int y1;

	@Unique
	private boolean tntsallin1client$savedBackground;

	@Unique
	private boolean tntsallin1client$savedTopAndBottom;

	@Unique
	private boolean tntsallin1client$themed;

	@Inject(method = "render", at = @At("HEAD"))
	private void tntsallin1client$beginThemed(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		this.tntsallin1client$themed = ThemedUi.active();
		if (this.tntsallin1client$themed) {
			this.tntsallin1client$savedBackground = this.renderBackground;
			this.tntsallin1client$savedTopAndBottom = this.renderTopAndBottom;
			this.renderBackground = false;
			this.renderTopAndBottom = false;
		}
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void tntsallin1client$endThemed(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (!this.tntsallin1client$themed) {
			return;
		}
		this.renderBackground = this.tntsallin1client$savedBackground;
		this.renderTopAndBottom = this.tntsallin1client$savedTopAndBottom;
		int color = ClientTheme.get().accent2;
		graphics.fill(this.x0, this.y0 - 1, this.x1, this.y0, color);
		graphics.fill(this.x0, this.y1, this.x1, this.y1 + 1, color);
		this.tntsallin1client$themed = false;
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void tntsallin1client$drawThemedTrack(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
		graphics.fill(left, top, right, bottom, this.tntsallin1client$themed ? ClientTheme.get().background2 : color);
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void tntsallin1client$drawThemedThumb(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
		graphics.fill(left, top, right, bottom, this.tntsallin1client$themed ? ClientTheme.get().accent3 : color);
	}

	/** The thumb's lighter top-left part - the themed thumb is one flat color. */
	@Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void tntsallin1client$skipThemedThumbShade(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
		if (!this.tntsallin1client$themed) {
			graphics.fill(left, top, right, bottom, color);
		}
	}
}
