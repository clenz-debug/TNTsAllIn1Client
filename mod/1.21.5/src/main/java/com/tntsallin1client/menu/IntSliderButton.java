package com.tntsallin1client.menu;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * Small integer-range slider on top of vanilla's own {@link AbstractSliderButton}
 * (same sprite/handle look as every vanilla options slider) - {@code value}
 * there is normalized to [0.0, 1.0] internally, this maps that to/from a
 * plain {@code [min, max]} int range.
 */
public class IntSliderButton extends AbstractSliderButton {
	private final int min;
	private final int max;
	private final IntFunction<Component> labelFactory;
	private final IntConsumer onChange;

	public IntSliderButton(int x, int y, int width, int height, int min, int max, int initial,
			IntFunction<Component> labelFactory, IntConsumer onChange) {
		super(x, y, width, height, Component.empty(), normalize(initial, min, max));
		this.min = min;
		this.max = max;
		this.labelFactory = labelFactory;
		this.onChange = onChange;
		this.updateMessage();
	}

	private static double normalize(int initial, int min, int max) {
		return max == min ? 0.0 : (double) (initial - min) / (max - min);
	}

	public int intValue() {
		return this.min + (int) Math.round(this.value * (this.max - this.min));
	}

	@Override
	protected void updateMessage() {
		this.setMessage(this.labelFactory.apply(this.intValue()));
	}

	@Override
	protected void applyValue() {
		this.onChange.accept(this.intValue());
	}
}
