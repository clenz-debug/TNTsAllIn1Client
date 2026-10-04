package com.tntsallin1client.design;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;

/**
 * The client logo as a button - the design switch on the title screen, as in the Fabric versions.
 * Grows slightly on hover so it's recognizably clickable without looking like a regular button.
 */
public class LogoButton extends ThemedButton {
	private static final float HOVER_GROWTH = 0.06F;

	public LogoButton(int id, int x, int y, int height) {
		super(id, x, y, Math.round(ClientLogo.widthFor(height)), height, "");
		this.tooltip = I18n.translate("gui.tntsallin1client.design.switch");
	}

	@Override
	public void render(MinecraftClient client, int mouseX, int mouseY) {
		if (!this.visible) {
			return;
		}
		this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
		if (TitleScreenDesign.isRunning()) {
			// The switch animation draws the (moving) logo itself.
			return;
		}
		float height = this.height * (1.0F + (this.hovered ? HOVER_GROWTH : 0.0F));
		float left = this.x + this.width / 2.0F - ClientLogo.widthFor(height) / 2.0F;
		float top = this.y + this.height / 2.0F - height / 2.0F;
		ClientLogo.draw(left, top, height, this.alpha);
	}
}
