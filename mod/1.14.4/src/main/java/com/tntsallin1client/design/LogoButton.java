package com.tntsallin1client.design;

import net.minecraft.client.resources.language.I18n;

/**
 * The client logo as a button - the design switch on the title screen and in the pause menu, as in
 * the other versions. Grows slightly on hover so it's recognizably clickable without looking like a
 * regular button.
 */
public class LogoButton extends ThemedButton {
	private static final float HOVER_GROWTH = 0.06F;

	public LogoButton(int x, int y, int height, Runnable onPress) {
		super(x, y, Math.round(ClientLogo.widthFor(height)), height, "", onPress);
		this.tooltip = I18n.get("gui.tntsallin1client.design.switch");
	}

	@Override
	public void renderButton(int mouseX, int mouseY, float partialTick) {
		if (TitleScreenDesign.isRunning()) {
			// The switch animation draws the (moving) logo itself.
			return;
		}
		float height = this.height * (1.0F + (this.isHovered() ? HOVER_GROWTH : 0.0F));
		float left = this.x + this.width / 2.0F - ClientLogo.widthFor(height) / 2.0F;
		float top = this.y + this.height / 2.0F - height / 2.0F;
		ClientLogo.draw(left, top, height, this.fade);
	}
}
