package com.tntsallin1client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 1.19.2 stand-in for 1.19.4's {@code net.minecraft.client.gui.components.StringWidget}: one line of
 * text in a box, not clickable. Same constructors, alignment and colour as the real one.
 */
public class StringWidget extends AbstractWidget {
	private final Font font;
	private int color = 0xFFFFFF;
	private float alignX = 0.5f;

	public StringWidget(Component message, Font font) {
		this(0, 0, font.width(message.getVisualOrderText()), 9, message, font);
	}

	public StringWidget(int width, int height, Component message, Font font) {
		this(0, 0, width, height, message, font);
	}

	public StringWidget(int x, int y, int width, int height, Component message, Font font) {
		super(x, y, width, height, message);
		this.font = font;
		this.active = false;
	}

	public StringWidget setColor(int color) {
		this.color = color;
		return this;
	}

	public StringWidget alignLeft() {
		this.alignX = 0.0f;
		return this;
	}

	public StringWidget alignCenter() {
		this.alignX = 0.5f;
		return this;
	}

	public StringWidget alignRight() {
		this.alignX = 1.0f;
		return this;
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		Component message = this.getMessage();
		int x = this.x + Math.round(this.alignX * (this.getWidth() - this.font.width(message)));
		int y = this.y + (this.getHeight() - 9) / 2;
		drawString(poseStack, this.font, message, x, y, this.color);
	}

	@Override
	public void updateNarration(NarrationElementOutput output) {
	}
}
