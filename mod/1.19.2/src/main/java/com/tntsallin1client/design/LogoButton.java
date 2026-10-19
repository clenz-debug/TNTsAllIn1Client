package com.tntsallin1client.design;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import com.tntsallin1client.gui.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * The client logo as a button - the design switch on the title screen (own user request: "ein
 * button als logo getarnt"). Grows slightly on hover so it's recognizably clickable without
 * looking like a regular button.
 */
public class LogoButton extends AbstractButton {
	private static final float HOVER_GROWTH = 0.06f;

	private final Runnable onPress;

	public LogoButton(int x, int y, int height, Runnable onPress) {
		super(x, y, Math.round(ClientLogo.widthFor(height)), height, Component.translatable("gui.tntsallin1client.design.switch"));
		this.onPress = onPress;
		// The logo is only there in the client design, so its tooltip is in the client font.
		this.setTooltip(Tooltip.create(ClientFont.of(Component.translatable("gui.tntsallin1client.design.switch"))));
	}

	@Override
	public void onPress() {
		this.onPress.run();
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float a) {
		GuiGraphics graphics = GuiGraphics.of(poseStack);
		if (TitleScreenDesign.isRunning()) {
			// The switch animation draws the (moving) logo itself.
			return;
		}
		float growth = this.isHoveredOrFocused() ? HOVER_GROWTH : 0.0f;
		float height = this.getHeight() * (1.0f + growth);
		float x = this.getX() + this.getWidth() / 2.0f - ClientLogo.widthFor(height) / 2.0f;
		float y = this.getY() + this.getHeight() / 2.0f - height / 2.0f;
		ClientLogo.draw(graphics, x, y, height, this.alpha);
	}

	@Override
	public void updateNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
