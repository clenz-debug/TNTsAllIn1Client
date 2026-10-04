package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ThemedButton;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.menu.IntSliderButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The game's buttons drawn like {@link ThemedButton} on themed screens - see {@link ThemedUi}. A
 * button that draws itself (our own themed buttons, the logo) never gets here.
 */
@Mixin(ButtonWidget.class)
public abstract class ButtonWidgetMixin {
	@Shadow
	protected int width;
	@Shadow
	protected int height;
	@Shadow
	public int x;
	@Shadow
	public int y;
	@Shadow
	public String message;
	@Shadow
	public boolean active;
	@Shadow
	public boolean visible;
	@Shadow
	protected boolean hovered;

	@Shadow
	protected abstract void mouseDragged(MinecraftClient client, int mouseX, int mouseY);

	@Inject(method = "render(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("HEAD"), cancellable = true)
	private void tnt$drawThemed(MinecraftClient client, int mouseX, int mouseY, CallbackInfo ci) {
		if (!ThemedUi.active()) {
			return;
		}
		ci.cancel();
		if (!this.visible) {
			return;
		}
		this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
		float alpha = ThemedUi.widgetAlpha();
		// A slider's track stays plain - its knob is what lights up, drawn by the slider from mouseDragged.
		boolean slider = (Object) this instanceof IntSliderButton;
		ThemedButton.drawFrame(this.x, this.y, this.width, this.height, this.active && this.hovered && !slider, alpha);
		this.mouseDragged(client, mouseX, mouseY);
		String label = ClientFont.fit(this.message, this.width - 6);
		ClientFont.drawCentered(label, this.x + this.width / 2.0F, this.y + (this.height - ClientFont.HEIGHT) / 2.0F, ThemedButton.textColor(this.active, alpha));
	}
}
