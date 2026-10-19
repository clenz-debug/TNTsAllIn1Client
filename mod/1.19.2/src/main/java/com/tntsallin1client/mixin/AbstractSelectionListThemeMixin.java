package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lists on themed screens ({@link ThemedUi}): no dirt behind them and no dirt bars above and below,
 * thin accent lines at their edges instead, and the same flat scrollbar track and accent thumb as the
 * Client Mods card menu. This version's lists are no widgets yet and draw all of that in one
 * {@code render}: the dirt behind them and the bars are switched off by their own two flags for the
 * time of the call, and the rows are clipped to the list then (see {@code clipThemedRows}). 1.19.2
 * builds the scrollbar from raw vertices in one batch (the newer versions use three fills) - so the
 * themed track and thumb are drawn over it afterwards, on exactly the rectangles the game computes
 * (same formula as its {@code render}).
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

	@Shadow
	public abstract int getMaxScroll();

	@Shadow
	public abstract double getScrollAmount();

	@Shadow
	protected abstract int getMaxPosition();

	@Shadow
	protected abstract int getScrollbarPosition();

	@Inject(method = "render", at = @At("HEAD"))
	private void tntsallin1client$beginThemed(PoseStack poseStack, int mouseX, int mouseY, float a, CallbackInfo ci) {
		this.tntsallin1client$themed = ThemedUi.active();
		if (this.tntsallin1client$themed) {
			this.tntsallin1client$savedBackground = this.renderBackground;
			this.tntsallin1client$savedTopAndBottom = this.renderTopAndBottom;
			this.renderBackground = false;
			this.renderTopAndBottom = false;
		}
	}

	/**
	 * 1.19.2 does not clip the rows (1.19.4 scissors them to the list): a row half out of the list is
	 * drawn whole, and the dirt bars drawn over it afterwards hide the overhang. With the bars off, the
	 * rows are clipped to the list here instead, the way the newer versions do it.
	 */
	@Inject(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderList(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
	private void tntsallin1client$clipThemedRows(PoseStack poseStack, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (this.tntsallin1client$themed) {
			GuiGraphics.of(poseStack).enableScissor(this.x0, this.y0, this.x1, this.y1);
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderList(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
	private void tntsallin1client$endClipThemedRows(PoseStack poseStack, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (this.tntsallin1client$themed) {
			GuiGraphics.of(poseStack).disableScissor();
		}
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void tntsallin1client$endThemed(PoseStack poseStack, int mouseX, int mouseY, float a, CallbackInfo ci) {
		GuiGraphics graphics = GuiGraphics.of(poseStack);
		if (!this.tntsallin1client$themed) {
			return;
		}
		this.renderBackground = this.tntsallin1client$savedBackground;
		this.renderTopAndBottom = this.tntsallin1client$savedTopAndBottom;
		ClientTheme theme = ClientTheme.get();
		int maxScroll = this.getMaxScroll();
		if (maxScroll > 0) {
			int left = this.getScrollbarPosition();
			int right = left + 6;
			int thumbHeight = Mth.clamp((int) ((float) ((this.y1 - this.y0) * (this.y1 - this.y0)) / this.getMaxPosition()), 32, this.y1 - this.y0 - 8);
			int thumbTop = Math.max(this.y0, (int) this.getScrollAmount() * (this.y1 - this.y0 - thumbHeight) / maxScroll + this.y0);
			graphics.fill(left, this.y0, right, this.y1, theme.background2);
			graphics.fill(left, thumbTop, right, thumbTop + thumbHeight, theme.accent3);
		}
		graphics.fill(this.x0, this.y0 - 1, this.x1, this.y0, theme.accent2);
		graphics.fill(this.x0, this.y1, this.x1, this.y1 + 1, theme.accent2);
		this.tntsallin1client$themed = false;
	}
}
