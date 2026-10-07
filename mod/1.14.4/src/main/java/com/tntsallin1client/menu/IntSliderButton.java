package com.tntsallin1client.menu;

import java.util.function.IntConsumer;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;

/**
 * A slider for a whole number, looking and behaving like the game's own option sliders (which are
 * tied to the game's settings and can't be used for ours). The label's translation takes the value
 * as its argument. The config is saved when the knob is let go.
 */
public class IntSliderButton extends AbstractSliderButton {
	private final String labelKey;
	private final int min;
	private final int max;
	private final IntConsumer setter;
	private int current;

	public IntSliderButton(int x, int y, int width, int height, String labelKey, int min, int max, int value, IntConsumer setter) {
		super(x, y, width, height, (double) (Mth.clamp(value, min, max) - min) / (max - min));
		this.labelKey = labelKey;
		this.min = min;
		this.max = max;
		this.setter = setter;
		this.current = Mth.clamp(value, min, max);
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		setMessage(I18n.get(this.labelKey, this.current));
	}

	/** The game moved the knob (`value`, 0 to 1) - it snaps to the nearest whole number. */
	@Override
	protected void applyValue() {
		int newValue = this.min + (int) Math.round(this.value * (this.max - this.min));
		this.value = (double) (newValue - this.min) / (this.max - this.min);
		if (newValue != this.current) {
			this.current = newValue;
			this.setter.accept(newValue);
		}
	}

	@Override
	public void onRelease(double mouseX, double mouseY) {
		super.onRelease(mouseX, mouseY);
		ClientConfig.get().save();
	}
}
