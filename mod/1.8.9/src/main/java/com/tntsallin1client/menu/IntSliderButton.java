package com.tntsallin1client.menu;

import net.minecraft.client.gui.DrawableHelper;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.design.ClientTheme;
import java.util.function.IntConsumer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;

/**
 * A slider for a whole number, looking and behaving like the game's own option sliders (which are
 * tied to the game's settings and can't be used for ours). The label's translation takes the value
 * as its argument. The config is saved when the knob is let go.
 */
public class IntSliderButton extends ButtonWidget {
	private static final int KNOB_WIDTH = 8;

	private final String labelKey;
	private final int min;
	private final int max;
	private final IntConsumer setter;
	private int value;
	private boolean dragging;

	public IntSliderButton(int id, int x, int y, int width, int height, String labelKey, int min, int max, int value, IntConsumer setter) {
		super(id, x, y, width, height, "");
		this.labelKey = labelKey;
		this.min = min;
		this.max = max;
		this.setter = setter;
		this.value = MathHelper.clamp(value, min, max);
		this.message = I18n.translate(labelKey, this.value);
	}

	/** Always the flat "disabled" background - the knob is what lights up. */
	@Override
	protected int getYImage(boolean hovered) {
		return 0;
	}

	/** Called by the game while it draws the button, right after the background - the place to draw the knob. */
	@Override
	protected void mouseDragged(MinecraftClient client, int mouseX, int mouseY) {
		if (!this.visible) {
			return;
		}
		if (this.dragging) {
			moveKnobTo(mouseX);
		}
		int knobX = this.x + Math.round((float) (this.value - this.min) / (this.max - this.min) * (this.width - KNOB_WIDTH));
		if (ThemedUi.active()) {
			// The client design's slider: a solid accent knob on the plain track (`ButtonWidgetMixin`).
			ClientTheme theme = ClientTheme.get();
			DrawableHelper.fill(knobX, this.y, knobX + KNOB_WIDTH, this.y + this.height, this.active && this.hovered ? theme.accent4 : theme.accent3);
			return;
		}
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.drawTexture(knobX, this.y, 0, 66, 4, 20);
		this.drawTexture(knobX + 4, this.y, 196, 66, 4, 20);
	}

	@Override
	public boolean isMouseOver(MinecraftClient client, int mouseX, int mouseY) {
		if (!super.isMouseOver(client, mouseX, mouseY)) {
			return false;
		}
		this.dragging = true;
		moveKnobTo(mouseX);
		return true;
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY) {
		if (this.dragging) {
			this.dragging = false;
			ClientConfig.get().save();
		}
	}

	private void moveKnobTo(int mouseX) {
		float ratio = MathHelper.clamp((float) (mouseX - (this.x + KNOB_WIDTH / 2)) / (this.width - KNOB_WIDTH), 0.0F, 1.0F);
		int newValue = this.min + Math.round(ratio * (this.max - this.min));
		if (newValue != this.value) {
			this.value = newValue;
			this.setter.accept(newValue);
			this.message = I18n.translate(this.labelKey, newValue);
		}
	}
}
