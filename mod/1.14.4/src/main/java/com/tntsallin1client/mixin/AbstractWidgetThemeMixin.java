package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedButton;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The game's buttons and sliders drawn like {@link ThemedButton} on themed screens - see
 * {@link ThemedUi}. A widget that draws itself (our own themed buttons, the logo, a text field)
 * never gets here.
 */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetThemeMixin {
	/** As wide as the game's own slider knob. */
	private static final int KNOB_WIDTH = 8;

	@Shadow
	protected int width;
	@Shadow
	protected int height;
	@Shadow
	public int x;
	@Shadow
	public int y;
	@Shadow
	public boolean active;

	@Shadow
	public abstract boolean isHovered();

	@Shadow
	public abstract String getMessage();

	@Inject(method = "renderButton", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$drawThemed(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!ThemedUi.active()) {
			return;
		}
		ci.cancel();
		float alpha = ThemedUi.widgetAlpha();
		Object self = this;
		boolean slider = self instanceof AbstractSliderButton;
		boolean hovered = this.active && this.isHovered();
		// A slider's track stays plain - its knob is what lights up.
		ThemedButton.drawFrame(this.x, this.y, this.width, this.height, hovered && !slider, alpha);
		if (slider) {
			ClientTheme theme = ClientTheme.get();
			double value = ((AbstractSliderButtonAccessor) self).tntsallin1client$getValue();
			int knobX = this.x + (int) (value * (this.width - KNOB_WIDTH));
			GuiComponent.fill(knobX, this.y, knobX + KNOB_WIDTH, this.y + this.height, ClientTheme.withAlpha(hovered ? theme.accent4 : theme.accent3, alpha));
		}
		ThemedButton.drawLabel(this.getMessage(), this.x, this.y, this.width, this.height, ThemedButton.textColor(this.active, alpha));
	}
}
