package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedButton;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla text buttons (plain and cycle buttons) and sliders drawn like {@link ThemedButton} on themed
 * screens - see {@link ThemedUi}. In 1.18.2 all of them are drawn by {@code AbstractWidget#renderButton}
 * itself (the newer versions have a {@code renderWidget} per kind): buttons only add their tooltip
 * after it, sliders their handle in {@code renderBg}, which a cancelled call skips as well. Icon buttons
 * ({@link ImageButton}) keep their textures; our own themed buttons draw themselves anyway.
 */
@Mixin(AbstractWidget.class)
public abstract class AbstractButtonThemeMixin {
	@Unique
	private static final int HANDLE_WIDTH = 8;

	@Shadow
	public int x;

	@Shadow
	public int y;

	@Shadow
	protected int width;

	@Shadow
	protected int height;

	@Shadow
	public boolean active;

	@Shadow
	protected float alpha;

	@Shadow
	public abstract boolean isHoveredOrFocused();

	@Shadow
	public abstract Component getMessage();

	@Inject(method = "renderButton", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$drawThemed(PoseStack poseStack, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (!ThemedUi.active()) {
			return;
		}
		Object self = this;
		GuiGraphics graphics = GuiGraphics.of(poseStack);
		boolean highlighted = this.active && this.isHoveredOrFocused();
		if ((self instanceof Button && !(self instanceof ImageButton)) || self instanceof CycleButton) {
			ThemedButton.drawFrame(graphics, this.x, this.y, this.width, this.height, highlighted, this.alpha);
		} else if (self instanceof AbstractSliderButton slider) {
			ClientTheme theme = ClientTheme.get();
			ThemedButton.drawFrame(graphics, this.x, this.y, this.width, this.height, false, this.alpha);
			int handleX = this.x + (int) (((AbstractSliderButtonAccessor) slider).tntsallin1client$getValue() * (this.width - HANDLE_WIDTH));
			graphics.fill(handleX, this.y, handleX + HANDLE_WIDTH, this.y + this.height,
					ClientTheme.withAlpha(highlighted ? theme.accent4 : theme.accent3, this.alpha));
		} else {
			return;
		}
		ThemedButton.drawCenteredLabel(graphics, this.getMessage(), this.x, this.y, this.width, this.height,
				ThemedButton.textColor(this.active, this.alpha));
		ci.cancel();
	}
}
